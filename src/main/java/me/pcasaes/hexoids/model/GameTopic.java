package me.pcasaes.hexoids.model;

import java.util.function.Consumer;

/**
 * Enum of game topics use to publish and consume domain events.
 */
public enum GameTopic {

    /**
     * Topic used to track joined and left game
     */
    JOIN_GAME_TOPIC(d -> getGame().getPlayers().consumeFromJoinTopic(d)),

    /**
     * Topic used to track player actions: moved, spawned, destroyed
     */
    PLAYER_ACTION_TOPIC(d -> getGame().getPlayers().consumeFromPlayerActionTopic(d)),

    /**
     * Topic used to track a bolts being fired (created)
     */
    BOLT_LIFECYCLE_TOPIC(d -> getGame().getPlayers().consumeFromBoltFiredTopic(d)),

    /**
     * Topic used to track bolt action: moved, exhausted
     */
    BOLT_ACTION_TOPIC(((Consumer<DomainEvent>) d -> getGame().getBolts().consumeFromBoltActionTopic(d)) //NOSONAR: false positive
            .andThen(d -> getGame().getPlayers().consumeFromBoltActionTopic(d))),

    /**
     * Topic used to track points being gained by a player
     */
    SCORE_BOARD_CONTROL_TOPIC(d -> getGame().getScoreBoard().consumeFromScoreBoardControlTopic(d)),

    /**
     * Topic used to track points score board being updated
     */
    SCORE_BOARD_UPDATE_TOPIC(d -> getGame().getScoreBoard().consumeFromScoreBoardUpdateTopic(d)),
    ;

    private static Game game;

    static void setGame(Game game) {
        GameTopic.game = game;
    }

    static Game getGame() {
        return game;
    }

    private final Consumer<DomainEvent> consumer;


    GameTopic(Consumer<DomainEvent> consumer) {
        this.consumer = consumer;
    }

    public void consume(DomainEvent domainEvent) {
        consumer.accept(domainEvent);
    }

}
