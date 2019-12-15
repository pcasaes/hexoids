package me.paulo.casaes.bbop.service;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.Session;
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
                .ifPresent(s -> asyncSend(s, message));
    }

    public void broadcast(String message) {
        sessions.values().forEach(s -> asyncSend(s, message));
    }

    private void asyncSend(Session session, String message) {
        session.getAsyncRemote().sendObject(message, result -> {
            if (result.getException() != null) {
                LOGGER.warning("Unable to send message: " + result.getException());
            }
        });
    }


}
