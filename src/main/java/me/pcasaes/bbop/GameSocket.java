package me.pcasaes.bbop;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import me.pcasaes.bbop.dto.CommandType;
import me.pcasaes.bbop.dto.MoveCommandDto;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.Player;
import me.pcasaes.bbop.service.DtoProcessorService;
import me.pcasaes.bbop.service.SessionService;
import me.pcasaes.bbop.service.eventqueue.EventQueueService;
import me.pcasaes.bbop.service.eventqueue.GameLoopService;
import me.pcasaes.bbop.service.kafka.KafkaService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class GameSocket {

    private static final Logger LOGGER = Logger.getLogger(GameSocket.class.getName());

    private final SessionService sessionService;

    private final DtoProcessorService dtoProcessorService;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    private final KafkaService kafkaService;

    private final Vertx vertx;

    public GameSocket() {
        this.sessionService = null;
        this.dtoProcessorService = null;
        this.gameLoopService = null;
        this.kafkaService = null;
        this.vertx = null;
    }

    @Inject
    public GameSocket(SessionService sessionService,
                      DtoProcessorService dtoProcessorService,
                      EventQueueService<GameLoopService.GameRunnable> gameLoopService,
                      KafkaService kafkaService,
                      Vertx vertx) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
        this.gameLoopService = gameLoopService;
        this.kafkaService = kafkaService;
        this.vertx = vertx;
    }

    public void startup(@Observes Router router) {
        router.route("/game/:id").handler(rc -> {
            String userId = rc.pathParam("id");
            HttpServerRequest request = rc.request();
            ServerWebSocket ctx = request.upgrade();

            onOpen(ctx, userId);

            ctx.closeHandler(n -> this.onClose(userId));
            ctx.exceptionHandler(n -> this.onClose(userId));

            ctx.handler(buff -> onMessage(buff.toString(), userId));

            ctx.accept();
        });
    }


    public void onOpen(ServerWebSocket session, String userId) {
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
        this.gameLoopService.enqueue(() -> {
            Optional<Player> player = Game.get()
                    .getPlayers()
                    .createPlayer(userId);

            if (player.isPresent()) {
                player.get().join();
            } else {
                Game.get().getPlayers().requestListOfPlayers(userId);
            }
        });

    }

    public void onClose(String userId) {
        if (sessionService.remove(userId)) {
            Optional<Player> player = Game.get().getPlayers().get(userId);
            if (player.isPresent()) {
                player.get().leave();
            } else {
                gameLoopService.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
            }
        }
    }

    public void onMessage(String message, String userId) {
        try {
            dtoProcessorService.getCommand(message)
                    .ifPresent(command -> {
                        if (command == CommandType.MOVE_PLAYER) {
                            final MoveCommandDto moveCommandDto = dtoProcessorService.deserialize(message, MoveCommandDto.class);

                            this.gameLoopService.enqueue(() -> Game.get().getPlayers()
                                    .createOrGet(userId)
                                    .move(moveCommandDto.getMoveX(),
                                            moveCommandDto.getMoveY(),
                                            moveCommandDto.getAngle(),
                                            moveCommandDto.getThrustAngle()
                                    )
                            );
                        } else if (command == CommandType.FIRE_BOLT) {
                            Optional<Player> player = Game.get().getPlayers().get(userId);
                            if (player.isPresent()) {
                                player.get().fire();
                            } else {
                                gameLoopService.enqueue(() -> Game.get().getPlayers().createOrGet(userId).fire());
                            }
                        } else if (command == CommandType.SPAWN_PLAYER) {
                            gameLoopService.enqueue(() -> Game.get().getPlayers().createOrGet(userId).spawn());
                        }
                    });

        } catch (RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
        }
    }
}
