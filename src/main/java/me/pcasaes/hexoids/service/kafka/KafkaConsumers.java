package me.pcasaes.hexoids.service.kafka;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.vertx.AsyncResultUni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import me.pcasaes.hexoids.model.DomainEvent;
import me.pcasaes.hexoids.model.GameTopic;
import me.pcasaes.hexoids.service.ConfigurationService;
import me.pcasaes.hexoids.service.eventqueue.GameQueueService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import pcasaes.hexoids.proto.Event;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class KafkaConsumers implements KafkaService {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumers.class.getName());

    private final GameQueueService gameLoopService;
    private final ConfigurationService configurationService;

    private volatile boolean caughtUp = false;

    private boolean caughtUpLocal = false;

    private volatile long startedAt;
    private volatile long lastCheck;

    private final Field consumerField;

    private volatile long allowSeekOnBoltLifeCycle = 0L;

    @Inject
    public KafkaConsumers(GameQueueService gameLoopService,
                          ConfigurationService configurationService) {
        this.gameLoopService = gameLoopService;
        this.configurationService = configurationService;

        try {
            consumerField = IncomingKafkaRecord.class.getDeclaredField("consumer");
            consumerField.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public void startup(@Observes StartupEvent event) {
        LOGGER.info("Starting up consumers");
    }

    @PostConstruct
    public void start() {

        long now = System.currentTimeMillis();
        this.startedAt = now;
        this.lastCheck = now + 5_000L;
    }

    @PreDestroy
    public void stop() {
        this.caughtUpLocal = this.caughtUp = true;
    }

    @Override
    public boolean hasStarted() {
        if (caughtUpLocal) {
            return true;
        }
        if (this.caughtUp || this.lastCheck <= System.currentTimeMillis()) {
            caughtUpLocal = caughtUp = true;
            return true;
        }
        return false;
    }

    private void processOnStarted(IncomingKafkaRecord<UUID, Event> record, GameTopic consumer) {
        while (!hasStarted()) {
            // this will likely block the io thread during start up. That's what we want.
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        process(record, consumer);
    }

    private void process(IncomingKafkaRecord<UUID, Event> record, GameTopic consumer) {
        try {
            DomainEvent d = toDomainEvent(record);
            gameLoopService.enqueue(() -> consumer.consume(d));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Incoming("join-game")
    public CompletionStage<Void> onJoinGame(IncomingKafkaRecord<UUID, Event> record) {
        process(record, GameTopic.JOIN_GAME_TOPIC);
        boolean c = caughtUpLocal;
        if (!c) {
            if (record.getTimestamp().toEpochMilli() >= this.startedAt) {
                caughtUpLocal = caughtUp = true;
            } else {
                lastCheck = System.currentTimeMillis() + 1000L;
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("player-action")
    public CompletionStage<Void> onPlayerAction(IncomingKafkaRecord<UUID, Event> record) {
        processOnStarted(record, GameTopic.PLAYER_ACTION_TOPIC);
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("bolt-life-cycle")
    public CompletionStage<Void> onBoltLifeCycle(IncomingKafkaRecord<UUID, Event> record) {
        long now = System.currentTimeMillis();
        long shouldStartAt = now - (configurationService.getBoltMaxDuration() + 10_000L);

        /*
        This is a very ugly hack. We'd like to start off from events that we're created since bolt_max_duration ago.
        Smallrye does not allow us to provide a org.apache.kafka.clients.consumer.ConsumerRebalanceListener
        Ideally we would use that to setup the correct offsets during startup or rebalance. Since we can't
        we'll break IncomingKafkaRecord's encapsulation to get the underlying kafka consumer and seek to the
        appropriate offset.
         */
        if (record.getTimestamp().toEpochMilli() < shouldStartAt) {
            //while seeking the main poll loop will keep going. So let's not always reseek.
            if (allowSeekOnBoltLifeCycle > now) {
                return CompletableFuture.completedFuture(null);
            }
            allowSeekOnBoltLifeCycle = now + (configurationService.getBoltMaxDuration() + 10_000L);

            final KafkaConsumer<UUID, Event> consumer = getConsumer(record);
            final TopicPartition topicPartition = new TopicPartition(GameTopic.BOLT_LIFECYCLE_TOPIC.name(), record.getPartition());

            Consumer<Handler<AsyncResult<Void>>> search = completionHandler ->
                    consumer.offsetsForTimes(topicPartition,
                            shouldStartAt,
                            done -> {
                                if (done.succeeded()) {
                                    if (done.result() == null) {
                                        consumer.endOffsets(topicPartition, d2 -> {
                                            if (done.succeeded()) {
                                                consumer.seek(topicPartition, d2.result(), completionHandler);
                                            } else {
                                                completionHandler.handle(Future.failedFuture(done.cause()));
                                            }
                                        });
                                    } else {
                                        consumer.seek(topicPartition, done.result().getOffset(), completionHandler);
                                    }
                                } else {
                                    completionHandler.handle(Future.failedFuture(done.cause()));
                                }
                            });


            return AsyncResultUni
                    .toUni(search)
                    .subscribeAsCompletionStage();
        } else {
            processOnStarted(record, GameTopic.BOLT_LIFECYCLE_TOPIC);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Incoming("bolt-action")
    public CompletionStage<Void> onBoltAction(IncomingKafkaRecord<UUID, Event> record) {
        processOnStarted(record, GameTopic.BOLT_ACTION_TOPIC);
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-control")
    public CompletionStage<Void> onSoreBoardControl(IncomingKafkaRecord<UUID, Event> record) {
        processOnStarted(record, GameTopic.SCORE_BOARD_CONTROL_TOPIC);
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-update")
    public CompletionStage<Void> onSoreBoardUpdate(IncomingKafkaRecord<UUID, Event> record) {
        processOnStarted(record, GameTopic.SCORE_BOARD_UPDATE_TOPIC);
        return CompletableFuture.completedFuture(null);
    }

    private DomainEvent toDomainEvent(IncomingKafkaRecord<UUID, Event> record) {
        if (record.getPayload() == null) {
            return DomainEvent.deleted(record.getKey());
        } else {
            return DomainEvent.of(record.getKey(), record.getPayload());
        }
    }


    private KafkaConsumer<UUID, Event> getConsumer(IncomingKafkaRecord<UUID, Event> record) {
        try {
            io.vertx.mutiny.kafka.client.consumer.KafkaConsumer<UUID, Event> c = (io.vertx.mutiny.kafka.client.consumer.KafkaConsumer<UUID, Event>) consumerField.get(record);
            return c.getDelegate();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
