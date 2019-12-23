package me.paulo.casaes.bbop.service.kafka;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.model.DomainEvent;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

@Dependent
public class KafkaConsumerService {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerService.class.getName());

    private final KafkaConfiguration configuration;

    private final DtoProcessorService dtoProcessorService;

    private List<KafkaThreadedConsumer> threads = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public KafkaConsumerService(KafkaConfiguration configuration, DtoProcessorService dtoProcessorService) {
        this.configuration = configuration;
        this.dtoProcessorService = dtoProcessorService;
    }


    public void start(String topic, java.util.function.Consumer<DomainEvent> processor) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            kafkaConsumer.partitionsFor(topic)
                    .stream()
                    .map(pInfo -> new TopicPartition(pInfo.topic(), pInfo.partition()))
                    .map(topicPartition -> KafkaThreadedConsumer
                            .startWithoutSubscription(
                                    properties,
                                    topicPartition,
                                    dtoProcessorService,
                                    processor))
                    .forEach(threads::add);
        }
    }

    @PreDestroy
    void stop() {
        threads
                .forEach(KafkaThreadedConsumer::stop);
    }


    private static class KafkaThreadedConsumer {

        private final DtoProcessorService dtoProcessorService;

        private final java.util.function.Consumer<DomainEvent> processor;

        private final Consumer<String, String> kafkaConsumer;

        private Thread thread;

        private boolean running;

        private KafkaThreadedConsumer(DtoProcessorService dtoProcessorService,
                                      java.util.function.Consumer<DomainEvent> processor,
                                      Consumer<String, String> kafkaConsumer) {
            this.dtoProcessorService = dtoProcessorService;
            this.processor = processor;
            this.kafkaConsumer = kafkaConsumer;
        }

        static KafkaThreadedConsumer startWithoutSubscription(Properties properties,
                                                              TopicPartition topicPartition,
                                                              DtoProcessorService dtoProcessorService,
                                                              java.util.function.Consumer<DomainEvent> processor) {

            Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);


            Collection<TopicPartition> partitions = Collections.singletonList(topicPartition);
            kafkaConsumer.assign(partitions);
            kafkaConsumer.seekToBeginning(partitions);

            KafkaThreadedConsumer kafkaThreadedConsumer = new KafkaThreadedConsumer(
                    dtoProcessorService,
                    processor,
                    kafkaConsumer);

            kafkaThreadedConsumer.thread = new Thread(kafkaThreadedConsumer::run, "kafka-consumer-" + topicPartition.topic());
            kafkaThreadedConsumer.thread.start();

            return kafkaThreadedConsumer;
        }

        void stop() {
            this.running = false;
            kafkaConsumer.wakeup();
        }

        private void run() {
            this.running = true;
            Duration pollDuration = Duration.ofMillis(1000);

            try {
                this.pollWhileRunning(pollDuration);
            } catch (WakeupException ex) {
                LOGGER.info("Received shutdown signal");
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            } finally {
                closeConsumer();
            }
            LOGGER.info("Consumer stopped");
        }

        private void pollWhileRunning(Duration pollDuration) {
            while (this.running) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(pollDuration);
                if (!records.isEmpty()) {
                    StreamSupport
                            .stream(
                                    records.spliterator(),
                                    false)
                            .forEach(this::process);
                }
            }
        }

        private void closeConsumer() {
            try {
                kafkaConsumer.close(Duration.ofSeconds(5));
            } catch (KafkaException ex) {
                LOGGER.log(Level.WARNING, "Could not close connection", ex);
            }
        }

        private void process(ConsumerRecord<String, String> record) {
            try {
                if (record.value() == null) {
                    this.processor.accept(DomainEvent.deleted(record.key()));
                } else {
                    dtoProcessorService.getEventType(record.value())
                            .ifPresent(eventType -> {
                                EventDto event = dtoProcessorService.deserialize(record.value(), eventType.getClassType());
                                this.processor.accept(DomainEvent.of(record.key(), event));
                            });
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
