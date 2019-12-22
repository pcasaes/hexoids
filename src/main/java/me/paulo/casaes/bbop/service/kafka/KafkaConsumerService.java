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
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Dependent
public class KafkaConsumerService {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerService.class.getName());

    private final KafkaConfiguration configuration;

    private final DtoProcessorService dtoProcessorService;

    private Consumer<String, String> kafkaConsumer;

    private java.util.function.Consumer<DomainEvent> processor;

    private boolean running;

    @Inject
    public KafkaConsumerService(KafkaConfiguration configuration, DtoProcessorService dtoProcessorService) {
        this.configuration = configuration;
        this.dtoProcessorService = dtoProcessorService;
    }


    public void start(String topic, java.util.function.Consumer<DomainEvent> processor) {
        this.processor = processor;

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        this.kafkaConsumer = new KafkaConsumer<>(properties);
        Collection<TopicPartition> partitions = this.kafkaConsumer.partitionsFor(topic)
                .stream()
                .map(pInfo -> new TopicPartition(pInfo.topic(), pInfo.partition()))
                .collect(Collectors.toList());

        this.kafkaConsumer.assign(partitions);
        this.kafkaConsumer.seekToBeginning(partitions);
        new Thread(this::run, "kafka-consumer-" + topic).start();
    }

    @PreDestroy
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
