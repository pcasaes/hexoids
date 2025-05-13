package me.pcasaes.hexoids.entrypoints.web;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import me.pcasaes.hexoids.core.domain.model.EntityId;
import me.pcasaes.hexoids.core.domain.service.GameTimeService;
import pcasaes.hexoids.proto.ClockSync;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.RequestCommand;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class GameRoutes {

    private static final Logger LOGGER = Logger.getLogger(GameRoutes.class.getName());

    private final ClientSessions clientSessions;

    private final GameTimeService gameTime;

    private final CommandDelegate commandDelegate;

    private final ApplicationConsumers.HaveStarted consumersHaveStarted;

    @Inject
    public GameRoutes(ClientSessions clientSessions,
                      GameTimeService gameTime,
                      CommandDelegate commandDelegate,
                      ApplicationConsumers.HaveStarted consumersHaveStarted) {
        this.clientSessions = clientSessions;
        this.commandDelegate = commandDelegate;
        this.gameTime = gameTime;
        this.consumersHaveStarted = consumersHaveStarted;
    }

    public void startup(@Observes Router router) {
        router.route("/game/:id").handler(rc -> {
            var userId = EntityId.of(rc.pathParam("id"));
            HttpServerRequest request = rc.request();

            request.toWebSocket(as -> {
                if (as.succeeded()) {
                    ServerWebSocket ctx = as.result();
                    onOpen(ctx, userId);

                    ctx.closeHandler(n -> this.onClose(userId));
                    ctx.exceptionHandler(n -> this.onClose(userId));

                    ctx.handler(buff -> onMessage(ctx, buff.getBytes(), userId));

                    ctx.accept();
                } else {
                    LOGGER.warning(() -> "Failed to open websocket: " + as.cause());
                }
            });
        });
    }


    public void onOpen(ServerWebSocket session, EntityId userId) {
        if (!this.consumersHaveStarted.getAsBoolean()) {
            LOGGER.warning("Not ready for new connections");
            try {
                session.close();
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "could not reject connection", ex);
            }
            return;
        }

        this.clientSessions.add(userId, session);
    }

    public void onClose(EntityId userId) {
        if (clientSessions.remove(userId)) {
            this.commandDelegate.leave(userId);
        }
    }

    public void onMessage(ServerWebSocket ctx, byte[] message, EntityId userId) {
        try {
            getCommand(message)
                    .ifPresent(command -> onCommand(ctx, userId, command));

        } catch (RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
        }
    }

    private void onCommand(ServerWebSocket ctx, EntityId userId, RequestCommand command) {
        if (command.hasMove()) {
            this.commandDelegate.move(
                    userId,
                    command.getMove()
            );
        } else if (command.hasFire()) {
            this.commandDelegate.fire(userId);
        } else if (command.hasSpawn()) {
            this.commandDelegate.spawn(userId);
        } else if (command.hasSetFixedIntertialDampenFactor()) {
            this.commandDelegate.setFixedInertialDampenFactor(userId, command.getSetFixedIntertialDampenFactor());
        } else if (command.hasJoin()) {
            syncClock(ctx);
            this.commandDelegate.join(userId, command.getJoin());
        }
    }

    private Optional<RequestCommand> getCommand(byte[] value) {
        try {
            var builder = RequestCommand.newBuilder();
            builder.mergeFrom(value);
            return Optional.of(builder.build());
        } catch (IOException | RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
            return Optional.empty();
        }
    }

    private void syncClock(ServerWebSocket ctx) {
        var buffer = Buffer.buffer(Dto.newBuilder()
                .setClock(ClockSync.newBuilder()
                        .setTime(this.gameTime.getTime())
                ).build()
                .toByteArray());
        ctx.write(buffer);
    }
}
