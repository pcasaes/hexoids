package me.pcasaes.hexoids;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import me.pcasaes.hexoids.model.EntityId;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.model.Player;
import me.pcasaes.hexoids.service.SessionService;
import me.pcasaes.hexoids.service.eventqueue.EventQueueService;
import me.pcasaes.hexoids.service.eventqueue.GameLoopService;
import me.pcasaes.hexoids.service.kafka.KafkaService;
import pcasaes.hexoids.proto.MoveCommandDto;
import pcasaes.hexoids.proto.RequestCommand;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static me.pcasaes.hexoids.model.DtoUtils.REQUEST_COMMAND_THREAD_SAFE_BUILDER;

@ApplicationScoped
public class GameSocket {

    private static final Logger LOGGER = Logger.getLogger(GameSocket.class.getName());

    private final SessionService sessionService;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    private final KafkaService kafkaService;

    public GameSocket() {
        this.sessionService = null;
        this.gameLoopService = null;
        this.kafkaService = null;
    }

    @Inject
    public GameSocket(SessionService sessionService,
                      EventQueueService<GameLoopService.GameRunnable> gameLoopService,
                      KafkaService kafkaService) {
        this.sessionService = sessionService;
        this.gameLoopService = gameLoopService;
        this.kafkaService = kafkaService;
    }

    public void startup(@Observes Router router) {
        router.route("/game/:id").handler(rc -> {
            EntityId userId = EntityId.of(rc.pathParam("id"));
            HttpServerRequest request = rc.request();
            ServerWebSocket ctx = request.upgrade();

            onOpen(ctx, userId);

            ctx.closeHandler(n -> this.onClose(userId));
            ctx.exceptionHandler(n -> this.onClose(userId));

            ctx.handler(buff -> onMessage(buff.getBytes(), userId));

            ctx.accept();
        });
    }


    public void onOpen(ServerWebSocket session, EntityId userId) {
        if (!this.kafkaService.hasStarted()) {
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
            Optional<Player> player = Game.get().getPlayers().get(userId);
            if (player.isPresent()) {
                player.get().leave();
            } else {
                gameLoopService.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
            }
        }
    }

    public void onMessage(byte[] message, EntityId userId) {
        try {
            getCommand(message)
                    .ifPresent(command -> onCommand(userId, command));

        } catch (RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
        }
    }

    private void onCommand(EntityId userId, RequestCommand command) {
        if (command.hasMove()) {
            MoveCommandDto moveCommandDto = command.getMove();

            this.gameLoopService.enqueue(() -> Game.get().getPlayers()
                    .createOrGet(userId)
                    .move(moveCommandDto.getMoveX(),
                            moveCommandDto.getMoveY(),
                            moveCommandDto.hasAngle() ? moveCommandDto.getAngle().getValue() : null
                    )
            );
        } else if (command.hasFire()) {
            this.gameLoopService.enqueue(() -> Game.get().getPlayers().createOrGet(userId).fire());
        } else if (command.hasSpawn()) {
            this.gameLoopService.enqueue(() -> Game.get().getPlayers().createOrGet(userId).spawn());
        } else if (command.hasJoin()) {
            this.gameLoopService.enqueue(() -> {
                Optional<Player> player = Game.get()
                        .getPlayers()
                        .createPlayer(userId);

                if (player.isPresent()) {
                    player.get().join(command.getJoin());
                } else {
                    Game.get().getPlayers().requestListOfPlayers(userId);
                }
                Game.get().getPlayers().connected(userId);
            });
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
}