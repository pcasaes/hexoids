package me.pcasaes.hexoids.entrypoints.jobs

import io.quarkus.runtime.StartupEvent
import io.smallrye.mutiny.tuples.Tuple3
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.EventRecord
import org.eclipse.microprofile.reactive.messaging.Acknowledgment
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.logging.Logger

@ApplicationScoped
class EventConsumers @Inject constructor(private val applicationConsumers: ApplicationConsumers) {
    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 400) event: StartupEvent?) {
        LOGGER.info("Starting up consumers")
    }


    @Incoming("join-game")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onJoinGame(message: Message<EventRecord>): CompletionStage<Unit> {
        applicationConsumers.onJoinGame(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("player-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onPlayerAction(message: Message<EventRecord>): CompletionStage<Unit> {
        applicationConsumers.onPlayerAction(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("bolt-life-cycle")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onBoltLifeCycle(message: Message<EventRecord>): CompletionStage<Unit> {
        applicationConsumers.onBoltLifeCycle(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("bolt-action")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onBoltAction(message: Message<EventRecord>): CompletionStage<Unit> {
        applicationConsumers.onBoltAction(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("score-board-control")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    fun onScoreBoardControl(message: Message<EventRecord>): CompletionStage<Unit> {
        try {
            applicationConsumers.onScoreBoardControl(toDomainEvent(message.getPayload()))
        } finally {
            message.ack()
        }
        return CompletableFuture.completedFuture(Unit)
    }

    @Incoming("score-board-update")
    @Acknowledgment(Acknowledgment.Strategy.NONE)
    fun onScoreBoardUpdate(message: Message<EventRecord>): CompletionStage<Unit> {
        applicationConsumers.onScoreBoardUpdate(toDomainEvent(message.getPayload()))
        return CompletableFuture.completedFuture(Unit)
    }

    private fun toDomainEvent(consumerRecord: EventRecord): DomainEvent {
        return if (consumerRecord.event == null) {
            DomainEvent.deleted(consumerRecord.key)
        } else {
            DomainEvent.of(consumerRecord.key, consumerRecord.event)
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(EventConsumers::class.java.getName())

        private val NONE: Tuple3<String?, Int?, Long?> = Tuple3.of<String?, Int?, Long?>("", 0, 0L)
    }
}
