package me.pcasaes.hexoids.infrastructure.vertx.cluster;

import io.quarkus.runtime.Startup;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.pcasaes.hexoids.core.domain.model.GameTopic;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.record.proto.EventRecord;
import pcasaes.hexoids.record.proto.UUIDKey;

import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
@Startup
public class ClusteredSender extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(ClusteredSender.class.getName());

    private final Vertx bootVertx;

    private boolean started = false;

    private final UUIDKey.Builder key = UUIDKey.newBuilder();
    private final EventRecord.Builder eventRecord = EventRecord
            .newBuilder()
            .setKey(key);


    @Inject
    public ClusteredSender(Vertx vertx) {
        this.bootVertx = vertx;
    }

    @PostConstruct
    void startup() {
        bootVertx.deployVerticle(this);
    }

    @PreDestroy
    void shutdown() {
        if (this.started) {
            bootVertx.undeploy(this.deploymentID());
        }
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        this.started = true;
        super.start(startPromise);
        LOGGER.info("Started");
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        this.started = false;
        super.stop(stopPromise);
        LOGGER.info("Stopped");
    }

    @Incoming("player-action-out")
    public void sendPlayerAction(Record<UUID, Event> consumerRecord) {
        trySendInContext(consumerRecord, GameTopic.PLAYER_ACTION_TOPIC, true);
    }

    @Incoming("bolt-action-out")
    public void sendBoltAction(Record<UUID, Event> consumerRecord) {
        trySendInContext(consumerRecord, GameTopic.BOLT_ACTION_TOPIC, true);
    }

    @Incoming("bolt-life-cycle-out")
    public void sendBoltLifeCycle(Record<UUID, Event> consumerRecord) {
        trySendInContext(consumerRecord, GameTopic.BOLT_LIFECYCLE_TOPIC, false);
    }

    private void trySendInContext(Record<UUID, Event> consumerRecord, GameTopic topic, boolean broadcast) {
        if (this.started) {
            context.runOnContext(h -> send(consumerRecord, topic, broadcast));
        } else {
            LOGGER.warning("Not started yet. dropping message");
        }
    }

    private void send(Record<UUID, Event> consumerRecord, GameTopic topic, boolean broadcast) {
        final EventBus eventBus = getVertx().eventBus();

        this.key.setMostSignificantDigits(consumerRecord.key().getMostSignificantBits())
                .setLeastSignificantDigits(consumerRecord.key().getLeastSignificantBits());

        this.eventRecord.setKey(this.key);

        if (consumerRecord.value() != null) {
            this.eventRecord.setEvent(consumerRecord.value());
        } else {
            this.eventRecord.clearEvent();
        }

        if (broadcast) {
            eventBus.publish(topic.name(), eventRecord
                    .build().toByteArray());
        } else {
            eventBus.send(topic.name(), eventRecord
                    .build().toByteArray());

        }
    }
}
