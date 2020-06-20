package me.pcasaes.hexoids.entrypoints.web;

import io.quarkus.runtime.ShutdownEvent;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import me.pcasaes.hexoids.core.application.commands.ApplicationCommands;
import me.pcasaes.hexoids.core.domain.model.EntityId;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ClientSessions {

    private static final Logger LOGGER = Logger.getLogger(ClientSessions.class.getName());

    private final Map<EntityId, ServerWebSocket> sessions;

    private final ApplicationCommands commandsService;

    @Inject
    public ClientSessions(ApplicationCommands commandsService) {
        this.commandsService = commandsService;
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
        try {
            session.write(message, result -> {
                if (result.failed()) {
                    removeAndClose(userId, session, result.cause());
                }
            });
        } catch (RuntimeException ex) {
            removeAndClose(userId, session, ex);
        }
    }

    private void removeAndClose(EntityId userId, ServerWebSocket session, Throwable ex) {
        boolean removed = this.remove(userId);
        if (removed && LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning("Session closed: " + userId + ", " + ex);
        }
        close(removed, session);
        if (removed) {
            this.commandsService
                    .getLeaveGameCommand()
                    .leave(userId);
        }
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
