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

    private final Map<EntityId, WebSocketSession> sessions;

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
        this.sessions.put(id, new WebSocketSession(session));
    }

    public boolean remove(EntityId id) {
        return this.sessions.remove(id) != null;
    }

    private Optional<WebSocketSession> get(EntityId id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public void direct(EntityId id, byte[] message) {
        LazzyBuffer buffer = LazzyBuffer.buffer(message);
        get(id)
                .ifPresent(s -> send(id, s, buffer));
    }

    public void broadcast(byte[] message) {
        LazzyBuffer buffer = LazzyBuffer.buffer(message);
        sessions.forEach((key, value) -> send(key, value, buffer));
    }

    private void send(EntityId key, WebSocketSession session, LazzyBuffer buffer) {
        if (session.socket.writeQueueFull()) {
            session.backlog(buffer.bytes);
        } else {
            asyncSend(key, session.socket, buffer.getBuffer());
        }
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

    private static class WebSocketSession {
        private final ServerWebSocket socket;

        public WebSocketSession(ServerWebSocket socket) {
            this.socket = socket;
        }

        /**
         * For now this logs lost messages. We should look into backing them up for retry.
         * @param bytes
         */
        public void backlog(byte[] bytes) {
            LOGGER.severe("Out of space in ServerWebSocket");
        }
    }

    private static class LazzyBuffer {
        private final byte[] bytes;
        private Buffer buffer;

        public LazzyBuffer(byte[] bytes) {
            this.bytes = bytes;
        }

        public static LazzyBuffer buffer(byte[] bytes) {
            return new LazzyBuffer(bytes);
        }

        public Buffer getBuffer() {
            if (buffer == null) {
                buffer = Buffer.buffer(bytes);
            }
            return buffer;
        }
    }
}
