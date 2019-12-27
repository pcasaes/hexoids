package me.paulo.casaes.bbop.model;

import java.util.function.Consumer;

public enum Topics {
    JoinGameTopic(Players.get()::consumeFromJoinTopic),
    PlayerActionTopic(Players.get()::consumeFromPlayerActionTopic);

    private final Consumer<DomainEvent> consumer;


    Topics(Consumer<DomainEvent> consumer) {
        this.consumer = consumer;
    }

    public void consume(DomainEvent domainEvent) {
        this.consumer.accept(domainEvent);
    }

}
