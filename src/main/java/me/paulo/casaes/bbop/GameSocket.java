package me.paulo.casaes.bbop;

import me.paulo.casaes.bbop.dto.CommandType;
import me.paulo.casaes.bbop.dto.MoveCommandDto;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.model.Player;
import me.paulo.casaes.bbop.service.DtoProcessorService;
import me.paulo.casaes.bbop.service.SessionService;
import me.paulo.casaes.bbop.service.eventqueue.EventQueueService;
import me.paulo.casaes.bbop.service.eventqueue.GameLoopService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Optional;
import java.util.logging.Logger;

@ServerEndpoint("/game/{userId}")
@ApplicationScoped
public class GameSocket {

    private static final Logger LOGGER = Logger.getLogger(GameSocket.class.getName());

    private final SessionService sessionService;

    private final DtoProcessorService dtoProcessorService;

    private final EventQueueService<GameLoopService.GameRunnable> gameLoopService;

    public GameSocket() {
        this.sessionService = null;
        this.dtoProcessorService = null;
        this.gameLoopService = null;
    }

    @Inject
    public GameSocket(SessionService sessionService,
                      DtoProcessorService dtoProcessorService,
                      EventQueueService<GameLoopService.GameRunnable> gameLoopService) {
        this.sessionService = sessionService;
        this.dtoProcessorService = dtoProcessorService;
        this.gameLoopService = gameLoopService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.sessionService.add(userId, session);
        this.gameLoopService.enqueue(() -> Game.get().getPlayers().createOrGet(userId).join());

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
                        }
                    });

        } catch (RuntimeException ex) {
            LOGGER.warning(ex.getMessage());
        }
    }
}
