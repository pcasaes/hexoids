package me.pcasaes.hexoids.core.application.eventhandlers;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import me.pcasaes.hexoids.core.domain.model.GameTopic;

import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationConsumers {

    private static final Logger LOGGER = Logger.getLogger(ApplicationConsumers.class.getName());

    private final GameQueue gameQueue;

    private ApplicationConsumers(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    public static ApplicationConsumers create(GameQueue gameQueue) {
        return new ApplicationConsumers(gameQueue);
    }


    private void process(DomainEvent domainEvent, GameTopic consumer) {
        try {
            gameQueue.enqueue(() -> consumer.consume(domainEvent));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void onJoinGame(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.JOIN_GAME_TOPIC);
    }

    public void onPlayerAction(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.PLAYER_ACTION_TOPIC);
    }

    public void onBoltLifeCycle(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.BOLT_LIFECYCLE_TOPIC);
    }

    public void onBoltAction(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.BOLT_ACTION_TOPIC);
    }

    public void onScoreBoardControl(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.SCORE_BOARD_CONTROL_TOPIC);
    }

    public void onScoreBoardUpdate(DomainEvent domainEvent) {
        process(domainEvent, GameTopic.SCORE_BOARD_UPDATE_TOPIC);
    }

    public interface HaveStarted extends BooleanSupplier {
    }

}
