package me.pcasaes.hexoids.infrastructure.vertx.cluster;

import io.quarkus.runtime.Startup;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import me.pcasaes.hexoids.core.domain.model.GameTopic;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import pcasaes.hexoids.proto.Event;
import pcasaes.hexoids.record.proto.EventRecord;
import pcasaes.hexoids.record.proto.UUIDKey;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
@Startup
public class ClusteredSender {

    private final Vertx vertx;

    @Inject
    public ClusteredSender(Vertx vertx) {
        this.vertx = vertx;
    }

    @Incoming("player-action-out")
    public void sendPlayerAction(Record<UUID, Event> consumerRecord) {
        send(consumerRecord, GameTopic.PLAYER_ACTION_TOPIC, true);
    }

    @Incoming("bolt-action-out")
    public void sendBoltAction(Record<UUID, Event> consumerRecord) {
        send(consumerRecord, GameTopic.BOLT_ACTION_TOPIC, true);
    }

    @Incoming("bolt-life-cycle-out")
    public void sendBoltLifeCycle(Record<UUID, Event> consumerRecord) {
        send(consumerRecord, GameTopic.BOLT_LIFECYCLE_TOPIC, false);
    }

    public void send(Record<UUID, Event> consumerRecord, GameTopic topic, boolean broadcast) {
        final EventBus eventBus = vertx.eventBus();

        EventRecord.Builder eventRecord = EventRecord
                .newBuilder()
                .setKey(UUIDKey.newBuilder()
                        .setMostSignificantDigits(consumerRecord.key().getMostSignificantBits())
                        .setLeastSignificantDigits(consumerRecord.key().getLeastSignificantBits()));

        if (consumerRecord.value() != null) {
            eventRecord.setEvent(consumerRecord.value());
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
