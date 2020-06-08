package me.pcasaes.hexoids.domain.service;

import me.pcasaes.hexoids.domain.config.Config;
import me.pcasaes.hexoids.domain.model.Game;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to add events into the game loop.
 */
public class GameLoopService {

    private static final GameLoopService INSTANCE = new GameLoopService();

    public static GameLoopService getInstance() {
        return INSTANCE;
    }

    private static final String NAME = "game-loop";
    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private final Optional<Runnable> fixedUpdateRunnable = Optional.of(this::fixedUpdate);


    private long nextFixedUpdateTime;

    private long getGameTime() {
        return Game.get().getClock().getTime();
    }

    private GameLoopService() {
        this.nextFixedUpdateTime = this.getGameTime();
    }

    private void fixedUpdate() {
        long timestamp = this.getGameTime();
        if (timestamp > this.nextFixedUpdateTime) {
            Game.get()
                    .fixedUpdate(timestamp);

            this.nextFixedUpdateTime = timestamp + Config.get().getUpdateFrequencyInMillis();
        }
    }

    public void accept(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        fixedUpdate();
    }

    public String getName() {
        return NAME;
    }

    public Optional<Runnable> getFixedUpdateRunnable() {
        long timestamp = this.getGameTime();
        if (timestamp > nextFixedUpdateTime) {
            return fixedUpdateRunnable;
        }
        return Optional.empty();
    }

}
