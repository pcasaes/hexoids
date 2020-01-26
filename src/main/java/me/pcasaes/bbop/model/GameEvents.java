package me.pcasaes.bbop.model;

import pcasaes.bbop.proto.Dto;

import java.util.function.Consumer;

public class GameEvents<T> {

    private static final GameEvents<Dto> CLIENT_INSTANCE = new GameEvents<>();

    private static final GameEvents<DomainEvent> DOMAIN_EVENT_INSTANCE = new GameEvents<>();

    private Consumer<T> consumer;

    private GameEvents() {
    }

    public static GameEvents<Dto> getClientEvents() {
        return CLIENT_INSTANCE;
    }

    public static GameEvents<DomainEvent> getDomainEvents() {
        return DOMAIN_EVENT_INSTANCE;
    }

    public void setConsumer(Consumer<T> consumer) {
        this.consumer = consumer;
    }


    public void register(T event) {
        if (consumer != null) {
            consumer.accept(event);
        }
    }
}
