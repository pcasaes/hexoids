package me.pcasaes.hexoids.entrypoints.web

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.web.Router
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers.HaveStarted
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.of
import me.pcasaes.hexoids.core.domain.service.GameTimeService
import pcasaes.hexoids.proto.ClockSync
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.RequestCommand
import java.io.IOException
import java.util.function.Supplier
import java.util.logging.Level
import java.util.logging.Logger

@ApplicationScoped
class GameRoutes @Inject constructor(
    private val clientSessions: ClientSessions,
    private val gameTime: GameTimeService,
    private val commandDelegate: CommandDelegate,
    private val consumersHaveStarted: HaveStarted
) {
    fun startup(@Observes router: Router) {
        router.route("/game/:id").handler { rc ->
            val userId = of(rc.pathParam("id"))
            val request = rc.request()
            request.toWebSocket { asyncResult ->
                if (asyncResult.succeeded()) {
                    val ctx = asyncResult.result()
                    onOpen(ctx, userId)

                    ctx.closeHandler { this.onClose(userId) }
                    ctx.exceptionHandler { this.onClose(userId) }

                    ctx.handler { buff -> onMessage(ctx, buff.bytes, userId) }

                    ctx.accept()
                } else {
                    LOGGER.warning(Supplier { "Failed to open websocket: " + asyncResult.cause() })
                }
            }
        }
    }


    fun onOpen(session: ServerWebSocket, userId: EntityId) {
        if (!this.consumersHaveStarted.asBoolean) {
            LOGGER.warning("Not ready for new connections")
            try {
                session.close()
            } catch (ex: RuntimeException) {
                LOGGER.log(Level.SEVERE, "could not reject connection", ex)
            }
            return
        }

        this.clientSessions.add(userId, session)
    }

    fun onClose(userId: EntityId) {
        if (clientSessions.remove(userId)) {
            this.commandDelegate.leave(userId)
        }
    }

    fun onMessage(ctx: ServerWebSocket, message: ByteArray, userId: EntityId) {
        try {
            getCommand(message)
                ?.let { command -> onCommand(ctx, userId, command) }
        } catch (ex: RuntimeException) {
            LOGGER.warning(ex.message)
        }
    }

    private fun onCommand(ctx: ServerWebSocket, userId: EntityId, command: RequestCommand) {
        when {
            command.hasMove() -> {
                this.commandDelegate.move(
                    userId,
                    command.getMove()
                )
            }

            command.hasFire() -> {
                this.commandDelegate.fire(userId)
            }

            command.hasSpawn() -> {
                this.commandDelegate.spawn(userId)
            }

            command.hasSetFixedIntertialDampenFactor() -> {
                this.commandDelegate.setFixedInertialDampenFactor(userId, command.getSetFixedIntertialDampenFactor())
            }

            command.hasJoin() -> {
                syncClock(ctx)
                this.commandDelegate.join(userId, command.getJoin())
            }
        }
    }

    private fun getCommand(value: ByteArray): RequestCommand? {
        return try {
            val builder = RequestCommand.newBuilder()
            builder.mergeFrom(value)
            builder.build()
        } catch (ex: IOException) {
            LOGGER.warning(ex.message)
            null
        } catch (ex: RuntimeException) {
            LOGGER.warning(ex.message)
            null
        }
    }

    private fun syncClock(ctx: ServerWebSocket) {
        val buffer = Buffer.buffer(
            Dto.newBuilder()
                .setClock(
                    ClockSync.newBuilder()
                        .setTime(this.gameTime.getTime())
                ).build()
                .toByteArray()
        )
        ctx.write(buffer)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(GameRoutes::class.java.getName())
    }
}
