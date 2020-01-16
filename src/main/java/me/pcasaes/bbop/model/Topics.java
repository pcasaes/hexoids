package me.pcasaes.bbop.model;

import java.util.function.Consumer;

public enum Topics {
    JOIN_GAME_TOPIC(d -> getGame().getPlayers().consumeFromJoinTopic(d)),
    PLAYER_ACTION_TOPIC(d -> getGame().getPlayers().consumeFromPlayerActionTopic(d)),
    BOLT_LIFECYCLE_TOPIC(d -> getGame().getPlayers().consumeFromBoltFiredTopic(d)),
    BOLT_ACTION_TOPIC(((Consumer<DomainEvent>) d -> getGame().getBolts().consumeFromBoltActionTopic(d)) //NOSONAR: false positive
            .andThen(d -> getGame().getPlayers().consumeFromBoltActionTopic(d))),
    SCORE_BOARD_CONTROL_TOPIC(d -> getGame().getScoreBoard().consumeFromScoreBoardControlTopic(d)),
    SCORE_BOARD_UPDATE_TOPIC(d -> getGame().getScoreBoard().consumeFromScoreBoardUpdateTopic(d)),
    ;

    private static Game game;

    static void setGame(Game game) {
        Topics.game = game;
    }

    static Game getGame() {
        return game;
    }

    private final Consumer<DomainEvent> consumer;


    Topics(Consumer<DomainEvent> consumer) {
        this.consumer = consumer;
    }

    public void consume(DomainEvent domainEvent) {
        consumer.accept(domainEvent);
    }

}
