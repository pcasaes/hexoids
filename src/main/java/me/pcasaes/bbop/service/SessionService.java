package me.pcasaes.bbop.service;

import io.quarkus.runtime.ShutdownEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.Player;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.websocket.Session;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class SessionService {

    private static final Logger LOGGER = Logger.getLogger(SessionService.class.getName());

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void add(String id, Session session) {
        this.sessions.put(id, session);
    }

    public boolean remove(String id) {
        return this.sessions.remove(id) != null;
    }

    private Optional<Session> get(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    public void direct(String id, String message) {
        get(id)
                .ifPresent(s -> asyncSend(id, s, message));
    }

    public void broadcast(String message) {
        sessions.entrySet().forEach(s -> asyncSend(s.getKey(), s.getValue(), message));
    }

    private void asyncSend(String userId, Session session, String message) {
        session.getAsyncRemote().sendText(message, result -> {
            if (result.getException() instanceof ClosedChannelException) {
                LOGGER.warning("Session closed: " + result.getException());
                if (this.remove(userId)) {
                    Game.get().getPlayers().get(userId).ifPresent(Player::leave);
                }
            } else if (result.getException() != null) {
                LOGGER.warning("Unable to send message: " + result.getException());
            }
        });
    }

    void stop(@Observes ShutdownEvent event) {
        sessions.clear();
    }
}
