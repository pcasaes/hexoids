package me.pcasaes.hexoids.service.eventqueue;

import me.pcasaes.hexoids.model.Config;
import me.pcasaes.hexoids.model.Game;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to add events into the game loop.
 */
@ApplicationScoped
public class GameLoopService implements EventQueueConsumerService<GameLoopService.GameRunnable> {

    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private final Optional<GameRunnable> fixedUpdateRunnable = Optional.of(() ->
            this.lastTimestamp = this.fixedUpdate(this.lastTimestamp)
    );


    private long lastTimestamp;


    @PostConstruct
    public void start() {
        this.lastTimestamp = Game.get().getClock().getTime();
    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = Game.get().getClock().getTime();
        if (timestamp - lastTimestamp > Config.get().getUpdateFrequencyInMillis()) {
            Game.get()
                    .fixedUpdate(timestamp);

            return timestamp;
        }
        return lastTimestamp;
    }

    @Override
    public void accept(GameRunnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        lastTimestamp = fixedUpdate(lastTimestamp);
    }

    @Override
    public String getName() {
        return "game-loop";
    }

    public Optional<GameRunnable> getFixedUpdateRunnable() {
        long timestamp = Game.get().getClock().getTime();
        if (timestamp - lastTimestamp > Config.get().getUpdateFrequencyInMillis()) {
            return fixedUpdateRunnable;
        }
        return Optional.empty();
    }

    public interface GameRunnable extends Runnable {

    }
}
