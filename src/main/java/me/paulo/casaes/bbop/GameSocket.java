package me.paulo.casaes.bbop;

import me.paulo.casaes.bbop.dto.CommandType;
import me.paulo.casaes.bbop.dto.MoveCommandDto;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.model.Player;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import me.paulo.casaes.bbop.service.SessionService;
import me.paulo.casaes.bbop.service.eventqueue.EventQueueService;
import me.paulo.casaes.bbop.service.eventqueue.GameLoopService;
import me.paulo.casaes.bbop.service.kafka.KafkaService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint("/game/{userId}")
@ApplicationScoped
public class GameSocket {

    private static final Logger LOGGER = Logger.getLogger(GameSocket.class.getName());

    private final SessionService sessionService;

    private final DtoProcessorService dtoProcessorService;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    private final KafkaService kafkaService;

    public GameSocket() {
        this.sessionService = null;
        this.dtoProcessorService = null;
        this.gameLoopService = null;
        this.kafkaService = null;
    }

    @Inject
    public GameSocket(SessionService sessionService,
                      DtoProcessorService dtoProcessorService,
                      EventQueueService<GameLoopService.GameRunnable> gameLoopService,
                      KafkaService kafkaService) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
        this.gameLoopService = gameLoopService;
        this.kafkaService = kafkaService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        if (!this.kafkaService.hasStarted()) {
            LOGGER.warning("Not ready for new connections");
            try {
                session.close();
            } catch (IOException ex) {
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

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        if (sessionService.remove(userId)) {
            Optional<Player> player = Game.get().getPlayers().get(userId);
            if (player.isPresent()) {
                player.get().leave();
            } else {
                gameLoopService.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
            }
        }
    }

    @OnError
    public void onError(Session session, @PathParam("userId") String userId, Throwable throwable) {
        onClose(session, userId);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
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
