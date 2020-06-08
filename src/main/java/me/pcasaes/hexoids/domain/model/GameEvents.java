package me.pcasaes.hexoids.domain.model;

import pcasaes.hexoids.proto.Dto;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class GameEvents<T> {

    private static final Logger LOGGER = Logger.getLogger(GameEvents.class.getName());

    private static final GameEvents<Dto> CLIENT_INSTANCE = new GameEvents<>("client");

    private static final GameEvents<DomainEvent> DOMAIN_EVENT_INSTANCE = new GameEvents<>("domain-event");

    private Consumer<T> dispatcher;

    private final String name;

    private GameEvents(String name) {
        this.name = name;
        // this is a noop dispatcher and should never actually be used
        this.dispatcher = event -> LOGGER.severe("NO DISPATCHER REGISTERED FOR " + this.name);
    }

    public static GameEvents<Dto> getClientEvents() {
        return CLIENT_INSTANCE;
    }

    public static GameEvents<DomainEvent> getDomainEvents() {
        return DOMAIN_EVENT_INSTANCE;
    }

    /**
     * The game model generates events that must be delivered to other instances
     * of the game model (horizontal scaling) or broadcasted to clients.
     * <p>
     * The domain model does not concern itself with how this is done, only that it is done.
     * This method is used to register infrastructure code to dispatch event.
     *
     * @param dispatcher
     */
    public void registerEventDispatcher(Consumer<T> dispatcher) {
        this.dispatcher = dispatcher;
    }


    public void dispatch(T event) {
        dispatcher.accept(event);
    }
}
