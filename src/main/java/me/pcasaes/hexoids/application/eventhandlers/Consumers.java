package me.pcasaes.hexoids.application.eventhandlers;

import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.domain.model.DomainEvent;
import me.pcasaes.hexoids.domain.model.GameTopic;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class Consumers {

    private static final Logger LOGGER = Logger.getLogger(Consumers.class.getName());

    private final GameQueue gameQueue;

    @Inject
    public Consumers(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
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
