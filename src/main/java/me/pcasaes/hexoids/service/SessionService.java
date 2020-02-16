package me.pcasaes.hexoids.service;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import me.pcasaes.hexoids.model.EntityId;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.model.Player;
import me.pcasaes.hexoids.service.eventqueue.GameQueueService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class SessionService {

    private static final Logger LOGGER = Logger.getLogger(SessionService.class.getName());

    private final Map<EntityId, ServerWebSocket> sessions;

    private final GameQueueService gameLoopService;

    SessionService() {
        this.sessions = null;
        this.gameLoopService = null;
    }

    @Inject
    public SessionService(GameQueueService gameLoopService) {
        this.gameLoopService = gameLoopService;
        this.sessions = new ConcurrentHashMap<>();
    }

    public void add(EntityId id, ServerWebSocket session) {
        this.sessions.put(id, session);
    }

    public boolean remove(EntityId id) {
        return this.sessions.remove(id) != null;
    }

    private Optional<ServerWebSocket> get(EntityId id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public void direct(EntityId id, byte[] message) {
        Buffer buffer = Buffer.buffer(message);
        get(id)
                .ifPresent(s -> asyncSend(id, s, buffer));
    }

    public void broadcast(byte[] message) {
        Buffer buffer = Buffer.buffer(message);
        sessions.forEach((key, value) -> asyncSend(key, value, buffer));
    }

    private void asyncSend(EntityId userId, ServerWebSocket session, Buffer message) {
        session.write(message, result -> {
            if (result.failed()) {
                boolean removed = this.remove(userId);
                if (removed && LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("Session closed: " + userId + ", " + result.cause());
                }
                close(removed, session);
                if (removed) {
                    this.gameLoopService.enqueue(() -> Game.get().getPlayers().get(userId).ifPresent(Player::leave));
                }
            }
        });
    }

    private void close(boolean removed, ServerWebSocket session) {
        try {
            session.close();
        } catch (RuntimeException ex) {
            if (removed && LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning("Already closed " + ex.getMessage());
            }
        }
    }

    void stop(@Observes ShutdownEvent event) {
        sessions.clear();
    }
}
