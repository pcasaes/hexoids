package me.pcasaes.hexoids.entrypoints.jobs

import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.model.EventRecord
import org.eclipse.microprofile.reactive.messaging.Incoming
import java.util.logging.Logger

@ApplicationScoped
class EventConsumers @Inject constructor(private val applicationConsumers: ApplicationConsumers) {
    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 400) event: StartupEvent?) {
        LOGGER.info("Starting up consumers")
    }


    @Incoming("join-game")
    fun onJoinGame(eventRecord: EventRecord) {
        applicationConsumers.onJoinGame(toDomainEvent(eventRecord))
    }

    @Incoming("player-action")
    fun onPlayerAction(eventRecord: EventRecord) {
        applicationConsumers.onPlayerAction(toDomainEvent(eventRecord))
    }

    @Incoming("bolt-life-cycle")
    fun onBoltLifeCycle(eventRecord: EventRecord) {
        applicationConsumers.onBoltLifeCycle(toDomainEvent(eventRecord))
    }

    @Incoming("bolt-action")
    fun onBoltAction(eventRecord: EventRecord) {
        applicationConsumers.onBoltAction(toDomainEvent(eventRecord))
    }

    @Incoming("score-board-control")
    fun onScoreBoardControl(eventRecord: EventRecord) {
        applicationConsumers.onScoreBoardControl(toDomainEvent(eventRecord))
    }

    @Incoming("score-board-update")
    fun onScoreBoardUpdate(eventRecord: EventRecord) {
        applicationConsumers.onScoreBoardUpdate(toDomainEvent(eventRecord))
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
    }
}
