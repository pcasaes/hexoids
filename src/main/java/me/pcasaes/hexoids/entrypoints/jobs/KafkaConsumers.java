package me.pcasaes.hexoids.entrypoints.jobs;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.tuples.Tuple3;
import io.smallrye.reactive.messaging.kafka.Record;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import pcasaes.hexoids.proto.Event;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

@ApplicationScoped
public class KafkaConsumers {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumers.class.getName());

    private static final Tuple3<String, Integer, Long> NONE = Tuple3.of("", 0, 0L);

    private final ApplicationConsumers applicationConsumers;


    @Inject
    public KafkaConsumers(ApplicationConsumers applicationConsumers) {
        this.applicationConsumers = applicationConsumers;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 400) StartupEvent event) {
        LOGGER.info("Starting up consumers");
    }


    @Incoming("join-game")
    @Outgoing("join-game-metadata")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public Message<Tuple3<String, Integer, Long>> onJoinGame(Message<Record<UUID, Event>> message) {
        applicationConsumers.onJoinGame(toDomainEvent(message.getPayload()));
        return Message.of(
                message.getMetadata(IncomingKafkaRecordMetadata.class)
                        .map(md -> Tuple3.of(md.getTopic(), md.getPartition(), md.getOffset()))
                        .orElse(NONE)
        );
    }

    @Incoming("player-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onPlayerAction(Message<Record<UUID, Event>> message) {
        applicationConsumers.onPlayerAction(toDomainEvent(message.getPayload()));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("bolt-life-cycle")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onBoltLifeCycle(Message<Record<UUID, Event>> message) {
        applicationConsumers.onBoltLifeCycle(toDomainEvent(message.getPayload()));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("bolt-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onBoltAction(Message<Record<UUID, Event>> message) {
        applicationConsumers.onBoltAction(toDomainEvent(message.getPayload()));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-control")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onScoreBoardControl(Message<Record<UUID, Event>> message) {
        try {
            applicationConsumers.onScoreBoardControl(toDomainEvent(message.getPayload()));
        } finally {
            message.ack();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-update")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onScoreBoardUpdate(Message<Record<UUID, Event>> message) {
        applicationConsumers.onScoreBoardUpdate(toDomainEvent(message.getPayload()));
        return CompletableFuture.completedFuture(null);
    }

    private DomainEvent toDomainEvent(Record<UUID, Event> consumerRecord) {
        if (consumerRecord.value() == null) {
            return DomainEvent.deleted(consumerRecord.key());
        } else {
            return DomainEvent.of(consumerRecord.key(), consumerRecord.value());
        }
    }

}
