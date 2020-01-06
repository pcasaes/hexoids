package me.paulo.casaes.bbop.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public enum Topics {
    JoinGameTopic(Players.get()::consumeFromJoinTopic),
    PlayerActionTopic(Players.get()::consumeFromPlayerActionTopic),
    BoltLifecycleTopic(Players.get()::consumeFromBoltFiredTopic),
    BoltActionTopic(Bolts.get()::consumeFromBoltActionTopic, Players.get()::consumeFromBoltActionTopic),
    ScoreBoardControlTopic(ScoreBoard.get()::consumeFromScoreBoardControlTopic),
    ScoreBoardUpdateTopic(ScoreBoard.get()::consumeFromScoreBoardUpdateTopic),
    ;

    private final List<Consumer<DomainEvent>> consumers;


    Topics(Consumer<DomainEvent>... consumers) {
        this.consumers = Arrays.asList(consumers);
    }

    public void consume(DomainEvent domainEvent) {
        for (Consumer<DomainEvent> consumer : consumers) {
            consumer.accept(domainEvent);
        }
    }

}
