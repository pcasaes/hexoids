package me.pcasaes.hexoids.entrypoints.web

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.of
import me.pcasaes.hexoids.core.domain.service.GameTimeService
import org.eclipse.microprofile.config.inject.ConfigProperty
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Events

/**
 * Used to broadcast events to the game clients
 */
@ApplicationScoped
class ClientBroadcaster @Inject constructor(
    private val clientSessions: ClientSessions,
    private val gameTime: GameTimeService,
    @param:ConfigProperty(
        name = "hexoids.config.service.client-broadcast.enabled",
        defaultValue = "true"
    ) private val enabled: Boolean,
    @param:ConfigProperty(
        name = "hexoids.config.service.client-broadcast.batch.size",
        defaultValue = "64"
    ) private val batchSize: Int,
    @param:ConfigProperty(
        name = "hexoids.config.service.client-broadcast.batch.timeout",
        defaultValue = "20"
    ) private val batchTimeout: Int
) {
    private val dtoBuilder: Dto.Builder = Dto.newBuilder()
    private val eventsBuilder: Events.Builder = Events.newBuilder()

    private var flushTimestamp: Long = 0

    fun accept(dto: Dto?) {
        if (dto != null) {
            when {
                dto.hasEvent() -> {
                    eventsBuilder
                        .addEvents(dto.getEvent())
                }

                dto.hasDirectedCommand() -> {
                    val command = dto.getDirectedCommand()
                    this.clientSessions.direct(of(command.playerId), dto.toByteArray())
                }

                dto.hasFlush() && canFlush() -> {
                    flushEvents(this.gameTime.getTime())
                    return
                }
            }
        }
        val now = this.gameTime.getTime()
        if (eventsBuilder.eventsCount > this.batchSize ||
            now > this.flushTimestamp
        ) {
            flushEvents(now)
        }
    }

    private fun flushEvents(now: Long) {
        if (eventsBuilder.eventsCount > 0) {
            this.clientSessions.broadcast(
                dtoBuilder
                    .setEvents(
                        eventsBuilder
                    )
                    .build()
                    .toByteArray()
            )

            dtoBuilder.clearEvents()
            eventsBuilder.clear()
        }
        this.flushTimestamp = now + this.batchTimeout
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    private fun canFlush(): Boolean {
        return this.gameTime.getTime() > this.flushTimestamp
    }

    fun getBatchTimeout(): Int {
        return batchTimeout
    }
}
