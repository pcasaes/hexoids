package me.pcasaes.hexoids.entrypoints.jobs

import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.tuples.Tuple3
import io.smallrye.reactive.messaging.kafka.Record
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Outgoing
import pcasaes.hexoids.proto.Event
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Function
import java.util.logging.Logger

@ApplicationScoped
class KafkaConsumers @Inject constructor(private val applicationConsumers: ApplicationConsumers) {
    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 400) event: StartupEvent?) {
        LOGGER.info("Starting up consumers")
    }


    @Incoming("join-game")
    @Outgoing("join-game-metadata")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onJoinGame(message: Message<Record<UUID, Event?>>): Message<Tuple3<String, Int, Long>> {
        applicationConsumers.onJoinGame(toDomainEvent(message.getPayload()))
        return Message.of(
            message.getMetadata(IncomingKafkaRecordMetadata::class.java)
                .map(Function { md ->
                    Tuple3.of(
                        md.topic, md.partition, md.offset
                    )
                })
                .orElse(NONE)
        )
    }

    @Incoming("player-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onPlayerAction(message: Message<Record<UUID, Event?>>): CompletionStage<Unit> {
        applicationConsumers.onPlayerAction(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("bolt-life-cycle")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onBoltLifeCycle(message: Message<Record<UUID, Event?>>): CompletionStage<Unit> {
        applicationConsumers.onBoltLifeCycle(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("bolt-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onBoltAction(message: Message<Record<UUID, Event?>>): CompletionStage<Unit> {
        applicationConsumers.onBoltAction(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("score-board-control")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun onScoreBoardControl(message: Message<Record<UUID, Event?>>): CompletionStage<Unit> {
        try {
            applicationConsumers.onScoreBoardControl(toDomainEvent(message.getPayload()))
        } finally {
            message.ack()
        }
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("score-board-update")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onScoreBoardUpdate(message: Message<Record<UUID, Event?>>): CompletionStage<Unit> {
        applicationConsumers.onScoreBoardUpdate(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    private fun toDomainEvent(consumerRecord: Record<UUID, Event?>): DomainEvent {
        return if (consumerRecord.value() == null) {
            DomainEvent.deleted(consumerRecord.key())
        } else {
            DomainEvent.of(consumerRecord.key(), consumerRecord.value())
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(KafkaConsumers::class.java.getName())

        private val NONE: Tuple3<String?, Int?, Long?> = Tuple3.of<String?, Int?, Long?>("", 0, 0L)
    }
}
