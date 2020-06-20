package me.pcasaes.hexoids.entrypoints.jobs;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import pcasaes.hexoids.proto.Event;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

@ApplicationScoped
public class KafkaConsumers {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumers.class.getName());

    private final ApplicationConsumers applicationConsumers;


    @Inject
    public KafkaConsumers(ApplicationConsumers applicationConsumers) {
        this.applicationConsumers = applicationConsumers;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 400) StartupEvent event) {
        LOGGER.info("Starting up consumers");
    }


    @Incoming("join-game")
    @Outgoing("join-game-time")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public Message<Long> onJoinGame(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onJoinGame(toDomainEvent(record));
        return Message.of(record.getTimestamp().toEpochMilli());
    }

    @Incoming("player-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onPlayerAction(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onPlayerAction(toDomainEvent(record));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("bolt-life-cycle")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onBoltLifeCycle(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onBoltLifeCycle(toDomainEvent(record));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("bolt-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onBoltAction(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onBoltAction(toDomainEvent(record));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-control")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onScoreBoardControl(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onScoreBoardControl(toDomainEvent(record));
        return CompletableFuture.completedFuture(null);
    }

    @Incoming("score-board-update")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    public CompletionStage<Void> onScoreBoardUpdate(IncomingKafkaRecord<UUID, Event> record) {
        applicationConsumers.onScoreBoardUpdate(toDomainEvent(record));
        return CompletableFuture.completedFuture(null);
    }

    private DomainEvent toDomainEvent(IncomingKafkaRecord<UUID, Event> record) {
        if (record.getPayload() == null) {
            return DomainEvent.deleted(record.getKey());
        } else {
            return DomainEvent.of(record.getKey(), record.getPayload());
        }
    }

}
