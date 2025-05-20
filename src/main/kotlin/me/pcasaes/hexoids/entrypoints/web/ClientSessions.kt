package me.pcasaes.hexoids.entrypoints.web

import io.quarkus.runtime.ShutdownEvent
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.application.commands.ApplicationCommands
import me.pcasaes.hexoids.core.domain.model.EntityId
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
class ClientSessions @Inject constructor(private val commandsService: ApplicationCommands) {
    private val sessions = ConcurrentHashMap<EntityId, ServerWebSocket>()

    fun add(id: EntityId, session: ServerWebSocket) {
        this.sessions.put(id, session)
    }

    fun remove(id: EntityId): Boolean {
        return this.sessions.remove(id) != null
    }

    private fun get(id: EntityId): ServerWebSocket? {
        return sessions[id]
    }

    fun direct(id: EntityId, message: ByteArray) {
        val buffer = Buffer.buffer(message)
        get(id)
            ?.let { s -> asyncSend(id, s, buffer) }
    }

    fun broadcast(message: ByteArray) {
        val buffer = Buffer.buffer(message)
        sessions.forEach { (key, value) -> asyncSend(key, value, buffer) }
    }

    private fun asyncSend(userId: EntityId, session: ServerWebSocket, message: Buffer) {
        try {
            session.write(message, Handler { result ->
                if (result.failed()) {
                    removeAndClose(userId, session, result.cause())
                }
            })
        } catch (ex: RuntimeException) {
            removeAndClose(userId, session, ex)
        }
    }

    private fun removeAndClose(userId: EntityId, session: ServerWebSocket, ex: Throwable) {
        val removed = this.remove(userId)
        if (removed && LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("Session closed: $userId, $ex")
        }
        close(removed, session)
        if (removed) {
            this.commandsService
                .getLeaveGameCommand()
                .leave(userId)
        }
    }

    private fun close(removed: Boolean, session: ServerWebSocket) {
        try {
            session.close()
        } catch (ex: RuntimeException) {
            if (removed && LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Already closed " + ex.message)
            }
        }
    }

    fun stop(@Observes event: ShutdownEvent) {
        sessions.clear()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ClientSessions::class.java.getName())
    }
}
