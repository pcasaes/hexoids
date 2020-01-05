package me.paulo.casaes.bbop.service.kafka;

import me.paulo.casaes.bbop.dto.EventDto;
import me.paulo.casaes.bbop.service.eventqueue.EventQueueService;
import me.paulo.casaes.bbop.service.eventqueue.GameLoopService;
import me.paulo.casaes.bbop.service.kafka.converter.EventDtoDeserializer;
import me.paulo.casaes.bbop.service.kafka.converter.UUIDBytesDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

@Dependent
public class KafkaConsumerService {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerService.class.getName());

    private final KafkaConfiguration configuration;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    private List<KafkaThreadedConsumer> threads = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public KafkaConsumerService(KafkaConfiguration configuration,
                                EventQueueService<GameLoopService.GameRunnable> gameLoopService) {
        this.configuration = configuration;
        this.gameLoopService = gameLoopService;
    }


    public void start(TopicInfo topicInfo) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDBytesDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDtoDeserializer.class.getName());

        topicInfo
                .consumerInfos()
                .forEach(consumerInfo -> {
                    consumerInfo.consumerConfig()
                            .ifPresent(properties::putAll);

                    if (consumerInfo.useSubscription()) {
                        threads.add(KafkaThreadedConsumer.startWithSubscription(
                                properties,
                                topicInfo.topic().name(),
                                runInGameLoop(consumerInfo)
                        ));
                    } else {
                        try (Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
                            kafkaConsumer.partitionsFor(topicInfo.topic().name())
                                    .stream()
                                    .map(pInfo -> new TopicPartition(pInfo.topic(), pInfo.partition()))
                                    .map(topicPartition -> KafkaThreadedConsumer
                                            .startWithoutSubscription(
                                                    properties,
                                                    topicPartition,
                                                    runInGameLoop(consumerInfo)))
                                    .forEach(threads::add);
                        }
                    }
                });

    }

    @PreDestroy
    void stop() {
        threads
                .forEach(KafkaThreadedConsumer::stop);
    }

    private Predicate<ConsumerRecord<UUID, EventDto>> runInGameLoop(TopicInfo.ConsumerInfo info) {
        return record -> {
            gameLoopService.enqueue(() -> info.consumeRecord(record));
            return false;
        };
    }

    private static class KafkaThreadedConsumer {

        private final Predicate<ConsumerRecord<UUID, EventDto>> processor;

        private final Consumer<UUID, EventDto> kafkaConsumer;

        private Thread thread;

        private boolean running;

        private KafkaThreadedConsumer(Predicate<ConsumerRecord<UUID, EventDto>> processor,
                                      Consumer<UUID, EventDto> kafkaConsumer) {
            this.processor = processor;
            this.kafkaConsumer = kafkaConsumer;
        }

        static KafkaThreadedConsumer startWithoutSubscription(Properties properties,
                                                              TopicPartition topicPartition,
                                                              Predicate<ConsumerRecord<UUID, EventDto>> processor) {

            Consumer<UUID, EventDto> kafkaConsumer = new KafkaConsumer<>(properties);


            Collection<TopicPartition> partitions = Collections.singletonList(topicPartition);
            kafkaConsumer.assign(partitions);
            kafkaConsumer.seekToBeginning(partitions);

            KafkaThreadedConsumer kafkaThreadedConsumer = new KafkaThreadedConsumer(
                    processor,
                    kafkaConsumer);

            kafkaThreadedConsumer.thread = new Thread(kafkaThreadedConsumer::run, "kafka-consumer-" + topicPartition.topic());
            kafkaThreadedConsumer.thread.start();

            return kafkaThreadedConsumer;
        }

        static KafkaThreadedConsumer startWithSubscription(Properties properties,
                                                           String topic,
                                                           Predicate<ConsumerRecord<UUID, EventDto>> processor) {

            Consumer<UUID, EventDto> kafkaConsumer = new KafkaConsumer<>(properties);

            kafkaConsumer.subscribe(Collections.singletonList(topic));


            KafkaThreadedConsumer kafkaThreadedConsumer = new KafkaThreadedConsumer(
                    processor,
                    kafkaConsumer);

            kafkaThreadedConsumer.thread = new Thread(kafkaThreadedConsumer::run, "kafka-consumer-" + topic);
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
                ConsumerRecords<UUID, EventDto> records = kafkaConsumer.poll(pollDuration);
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

        private void process(ConsumerRecord<UUID, EventDto> record) {
            try {
                if (this.processor.test(record)) {
                    this.kafkaConsumer.commitAsync();
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
