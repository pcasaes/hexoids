package me.pcasaes.hexoids.infrastructure.vertx.cluster;

import com.google.protobuf.InvalidProtocolBufferException;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import me.pcasaes.hexoids.core.domain.model.GameTopic;
import me.pcasaes.hexoids.infrastructure.kafka.DelayedStartConsumerHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.record.proto.EventRecord;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@ApplicationScoped
@Startup
public class ClusteredReceiver {

    private static final Logger LOGGER = Logger.getLogger(ClusteredReceiver.class.getName());

    private final Vertx vertx;

    private final Emitter<Record<UUID, Event>>[] emitters;

    private final List<MessageConsumer<byte[]>> consumers = new CopyOnWriteArrayList<>();

    private final DelayedStartConsumerHandler delayedStartConsumerHandler;

    private volatile Cancellable startupSubscription;

    public ClusteredReceiver(
            Vertx vertx,
            DelayedStartConsumerHandler delayedStartConsumerHandler,
            @ConfigProperty(name = "hexoids.config.service.kafka.subscriber", defaultValue = "true")
                    boolean isSubscriber,
            @Channel("player-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
                    Emitter<Record<UUID, Event>> playerActionEmitter,
            @Channel("bolt-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
                    Emitter<Record<UUID, Event>> boltActionEmitter,
            @Channel("bolt-life-cycle") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
                    Emitter<Record<UUID, Event>> boltLifeCycleEmitter) {
        this.vertx = vertx;
        this.delayedStartConsumerHandler = delayedStartConsumerHandler;

        Emitter<Record<UUID, Event>>[] ems = new Emitter[GameTopic.values().length];

        ems[GameTopic.PLAYER_ACTION_TOPIC.ordinal()] = playerActionEmitter;
        ems[GameTopic.BOLT_ACTION_TOPIC.ordinal()] = boltActionEmitter;

        if (isSubscriber) {
            ems[GameTopic.BOLT_LIFECYCLE_TOPIC.ordinal()] = boltLifeCycleEmitter;
        } else {
            LOGGER.warning("bolt lifecycle topic consumer disabled");
        }

        this.emitters = ems;
    }

    @PostConstruct
    public void startup() {
        this.startupSubscription = delayedStartConsumerHandler
                .onStarted()
                .invoke(() ->
                        Arrays.stream(GameTopic.values())
                                .filter(topic -> emitters[topic.ordinal()] != null)
                                .forEach(this::configureConsumer)
                ).subscribe()
                .with(a -> {
                });
    }

    @PreDestroy
    public void shutdown() {
        this.startupSubscription.cancel();
        consumers
                .forEach(MessageConsumer::unregister);
    }

    private void configureConsumer(GameTopic topic) {
        final EventBus eventBus = vertx.eventBus();
        MessageConsumer<byte[]> consumer = eventBus.consumer(topic.name(), receivedMessage -> {
                    try {
                        EventRecord eventRecord = EventRecord.newBuilder().mergeFrom(receivedMessage.body()).build();
                        UUID key = new UUID(eventRecord.getKey().getMostSignificantDigits(), eventRecord.getKey().getLeastSignificantDigits());
                        emitters[topic.ordinal()].send(Record.of(key, eventRecord.getEvent()));
                    } catch (InvalidProtocolBufferException ex) {
                        LOGGER.severe("Could not process record: " + ex.toString());
                    }
                }
        );
        consumer.completionHandler(res -> {
            if (res.failed()) {
                LOGGER.severe(() -> "Failed to subscribe to " + topic + ": " + res.cause());
            } else {
                LOGGER.info(() -> "Subscribed to " + topic);
            }
        });

        this.consumers.add(consumer);
    }
}
