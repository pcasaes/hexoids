package me.pcasaes.hexoids.clientinterface;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import me.pcasaes.hexoids.application.commands.CommandsService;
import me.pcasaes.hexoids.application.eventhandlers.Consumers;
import me.pcasaes.hexoids.domain.model.EntityId;
import me.pcasaes.hexoids.domain.service.GameTimeService;
import pcasaes.hexoids.proto.MoveCommandDto;
import pcasaes.hexoids.proto.RequestCommand;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.pcasaes.hexoids.domain.model.DtoUtils.CLOCK_SYNC_THREAD_SAFE_BUILDER;
import static me.pcasaes.hexoids.domain.model.DtoUtils.DTO_THREAD_SAFE_BUILDER;
import static me.pcasaes.hexoids.domain.model.DtoUtils.REQUEST_COMMAND_THREAD_SAFE_BUILDER;

@ApplicationScoped
public class GameRoutes {

    private static final Logger LOGGER = Logger.getLogger(GameRoutes.class.getName());

    private final SessionsService sessionService;

    private final GameTimeService gameTime;

    private final CommandsService commandsService;

    private final Consumers.HaveStarted consumersHaveStarted;

    @Inject
    public GameRoutes(SessionsService sessionService,
                      GameTimeService gameTime,
                      CommandsService commandsService,
                      Consumers.HaveStarted consumersHaveStarted) {
        this.sessionService = sessionService;
        this.commandsService = commandsService;
        this.gameTime = gameTime;
        this.consumersHaveStarted = consumersHaveStarted;
    }

    public void startup(@Observes Router router) {
        router.route("/game/:id").handler(rc -> {
            EntityId userId = EntityId.of(rc.pathParam("id"));
            HttpServerRequest request = rc.request();
            ServerWebSocket ctx = request.upgrade();

            onOpen(ctx, userId);

            ctx.closeHandler(n -> this.onClose(userId));
            ctx.exceptionHandler(n -> this.onClose(userId));

            ctx.handler(buff -> onMessage(ctx, buff.getBytes(), userId));

            ctx.accept();
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

        this.sessionService.add(userId, session);
    }

    public void onClose(EntityId userId) {
        if (sessionService.remove(userId)) {
            this.commandsService.getLeaveGameCommand().leave(userId);
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
            MoveCommandDto moveCommandDto = command.getMove();
            this.commandsService.getMoveCommand().move(userId, moveCommandDto);
        } else if (command.hasFire()) {
            this.commandsService.getFireCommand().fire(userId);
        } else if (command.hasSpawn()) {
            this.commandsService.getSpawn().spawn(userId);
        } else if (command.hasJoin()) {
            syncClock(ctx);
            this.commandsService.getJoinGameCommand().join(userId, command.getJoin());
        }
    }

    private Optional<RequestCommand> getCommand(byte[] value) {
        try {
            RequestCommand.Builder builder = REQUEST_COMMAND_THREAD_SAFE_BUILDER.get();
            builder.clear();
            builder.mergeFrom(value);
            return Optional.of(builder.build());
        } catch (IOException | RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
            return Optional.empty();
        }
    }

    private void syncClock(ServerWebSocket ctx) {
        Buffer buffer = Buffer.buffer(DTO_THREAD_SAFE_BUILDER
                .get()
                .setClock(CLOCK_SYNC_THREAD_SAFE_BUILDER
                        .get()
                        .setTime(this.gameTime.getTime())
                ).build()
                .toByteArray());
        ctx.write(buffer);
    }
}
