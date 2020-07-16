package me.pcasaes.hexoids.core.domain.model;

import pcasaes.hexoids.proto.Dto;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class GameEvents<T> {

    private static final Logger LOGGER = Logger.getLogger(GameEvents.class.getName());

    private static final GameEvents<Dto> CLIENT_INSTANCE = new GameEvents<>("client");

    private static final GameEvents<DomainEvent> DOMAIN_EVENT_INSTANCE = new GameEvents<>("domain-event");

    private final AtomicReference<Consumer<T>> dispatcher = new AtomicReference<>();

    private final String name;

    private GameEvents(String name) {
        this.name = name;
    }

    public static GameEvents<Dto> getClientEvents() {
        return CLIENT_INSTANCE;
    }

    public static GameEvents<DomainEvent> getDomainEvents() {
        return DOMAIN_EVENT_INSTANCE;
    }

    /**
     * The game model generates events that must be delivered to other instances
     * of the game model (horizontal scaling) or broadcast to clients.
     * <p>
     * The domain model does not concern itself with how this is done, only that it is done.
     * This method is used to register infrastructure code to dispatch event.
     *
     * @param dispatcher
     */
    public void registerEventDispatcher(Consumer<T> dispatcher) {
        this.dispatcher.set(dispatcher);
    }

    private Consumer<T> getDispatcher() {
        Consumer<T> currentDispatcher = this.dispatcher.getPlain();
        if (currentDispatcher != null) {
            return currentDispatcher;
        }
        currentDispatcher = this.dispatcher.get();
        if (currentDispatcher == null) {
            // this is a noop dispatcher and should never actually be used
            return event -> LOGGER.severe("NO DISPATCHER REGISTERED FOR ".concat(this.name));
        }
        return currentDispatcher;
    }

    public void dispatch(T event) {
        getDispatcher().accept(event);
    }
}
