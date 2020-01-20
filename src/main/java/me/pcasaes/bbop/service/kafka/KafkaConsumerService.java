package me.pcasaes.bbop.service.kafka;

import me.pcasaes.bbop.dto.EventDto;
import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.service.eventqueue.EventQueueService;
import me.pcasaes.bbop.service.eventqueue.GameLoopService;
import me.pcasaes.bbop.service.kafka.converter.EventDtoDeserializer;
import me.pcasaes.bbop.service.kafka.converter.UUIDBytesDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
import java.util.logging.Level;
import java.util.logging.Logger;

@Dependent
public class KafkaConsumerService {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerService.class.getName());

    private final KafkaConfiguration configuration;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    private List<KafkaThreadedConsumer> threads = Collections.synchronizedList(new ArrayList<>());

    private long startedAt;

    private boolean started = false;

    private boolean subcriber;

    @Inject
    public KafkaConsumerService(KafkaConfiguration configuration,
                                EventQueueService<GameLoopService.GameRunnable> gameLoopService,
                                @ConfigProperty(
                                        name = "bbop.config.service.kafka.subscriber",
                                        defaultValue = "true"
                                ) boolean subscriber) {
        this.configuration = configuration;
        this.gameLoopService = gameLoopService;
        this.subcriber = subscriber;
    }


    public void start(TopicInfo topicInfo) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.configuration.getConnectionUrl());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDBytesDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDtoDeserializer.class.getName());

        this.startedAt = System.currentTimeMillis();
        topicInfo
                .consumerInfos()
                .forEach(consumerInfo -> {
                    consumerInfo.consumerConfig()
                            .ifPresent(properties::putAll);

                    if (consumerInfo.useSubscription()) {
                        if (this.subcriber) {
                            threads.add(startWithSubscription(
                                    properties,
                                    topicInfo.topic().name(),
                                    consumerInfo
                            ));
                        } else {
                            this.started = true;
                        }
                    } else {
                        try (Consumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
                            kafkaConsumer.partitionsFor(topicInfo.topic().name())
                                    .stream()
                                    .map(pInfo -> new TopicPartition(pInfo.topic(), pInfo.partition()))
                                    .map(topicPartition -> startWithoutSubscription(
                                            properties,
                                            topicPartition,
                                            consumerInfo))
                                    .forEach(threads::add);
                        }
                    }
                });
    }

    @PreDestroy
    void stop() {
        this.started = true;
        threads
                .forEach(KafkaThreadedConsumer::stop);
    }

    boolean isStarted() {
        return started;
    }

    private KafkaThreadedConsumer startWithoutSubscription(
            Properties properties,
            TopicPartition topicPartition,
            TopicInfo.ConsumerInfo consumerInfo) {

        Consumer<UUID, EventDto> kafkaConsumer = new KafkaConsumer<>(properties);


        Collection<TopicPartition> partitions = Collections.singletonList(topicPartition);
        kafkaConsumer.assign(partitions);
        kafkaConsumer.seekToBeginning(partitions);

        KafkaThreadedConsumer kafkaThreadedConsumer = new KafkaThreadedConsumer(
                consumerInfo,
                kafkaConsumer,
                gameLoopService);

        kafkaThreadedConsumer.thread = new Thread(kafkaThreadedConsumer::run, "kafka-consumer-" + topicPartition.topic());
        kafkaThreadedConsumer.thread.start();

        return kafkaThreadedConsumer;
    }

    private KafkaThreadedConsumer startWithSubscription(
            Properties properties,
            String topic,
            TopicInfo.ConsumerInfo consumerInfo) {

        Consumer<UUID, EventDto> kafkaConsumer = new KafkaConsumer<>(properties);

        kafkaConsumer.subscribe(Collections.singletonList(topic));


        KafkaThreadedConsumer kafkaThreadedConsumer = new KafkaThreadedConsumer(
                consumerInfo,
                kafkaConsumer,
                gameLoopService);

        kafkaThreadedConsumer.thread = new Thread(kafkaThreadedConsumer::run, "kafka-consumer-" + topic);
        kafkaThreadedConsumer.thread.start();

        return kafkaThreadedConsumer;
    }

    private class KafkaThreadedConsumer {

        private final TopicInfo.ConsumerInfo consumerInfo;

        private final Consumer<UUID, EventDto> kafkaConsumer;

        private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

        private Thread thread;

        private boolean running;

        private long latestRecordTimestamp;

        private KafkaThreadedConsumer(TopicInfo.ConsumerInfo consumerInfo,
                                      Consumer<UUID, EventDto> kafkaConsumer,
                                      EventQueueService<GameLoopService.GameRunnable> gameLoopService) {
            this.consumerInfo = consumerInfo;
            this.kafkaConsumer = kafkaConsumer;
            this.gameLoopService = gameLoopService;
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
                    records.forEach(this::process);
                    if (!started && this.latestRecordTimestamp >= startedAt) {
                        started = true;
                    }
                } else if (!started) {
                    started = true;
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
                this.latestRecordTimestamp = record.timestamp();
                DomainEvent domainEvent;
                if (record.value() == null) {
                    domainEvent = DomainEvent.deleted(record.key());
                } else {
                    domainEvent = DomainEvent.of(record.key(), record.value());
                }
                this.gameLoopService.enqueue(() -> consumerInfo.consume(domainEvent));
                this.consumerInfo.postConsume(kafkaConsumer, record);
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

}
