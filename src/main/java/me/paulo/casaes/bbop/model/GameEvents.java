package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.Dto;

import java.util.function.Consumer;

public class GameEvents {

    private static final GameEvents INSTANCE = new GameEvents();

    private Consumer<Dto> consumer;

    private GameEvents() {
    }

    public static GameEvents get() {
        return INSTANCE;
    }

    public void setConsumer(Consumer<Dto> consumer) {
        this.consumer = consumer;
    }


    void register(Dto event) {
        if (consumer != null) {
            consumer.accept(event);
        }
    }
}
