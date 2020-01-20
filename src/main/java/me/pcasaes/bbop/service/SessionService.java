package me.pcasaes.bbop.service;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.http.ServerWebSocket;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.Player;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class SessionService {

    private static final Logger LOGGER = Logger.getLogger(SessionService.class.getName());

    private final Map<String, ServerWebSocket> sessions = new ConcurrentHashMap<>();

    public void add(String id, ServerWebSocket session) {
        this.sessions.put(id, session);
    }

    public boolean remove(String id) {
        return this.sessions.remove(id) != null;
    }

    private Optional<ServerWebSocket> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public void direct(String id, String message) {
        get(id)
                .ifPresent(s -> asyncSend(id, s, message));
    }

    public void broadcast(String message) {
        sessions.entrySet().forEach(s -> asyncSend(s.getKey(), s.getValue(), message));
    }

    private void asyncSend(String userId, ServerWebSocket session, String message) {
        session.writeTextMessage(message, result -> {
            if (result.failed()) {
                LOGGER.warning("Session closed: " + userId);
                try {
                    session.close();
                } catch (RuntimeException ex) {
                    LOGGER.warning("Already closed " + ex.getMessage());
                }
                if (this.remove(userId)) {
                    Game.get().getPlayers().get(userId).ifPresent(Player::leave);
                }
            }
        });
    }

    void stop(@Observes ShutdownEvent event) {
        sessions.clear();
    }
}
