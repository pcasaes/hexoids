package me.pcasaes.hexoids.domain.service;

import me.pcasaes.hexoids.domain.model.Config;
import me.pcasaes.hexoids.domain.model.Game;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to add events into the game loop.
 */
public class GameLoopService {

    private static final GameLoopService INSTANCE = new GameLoopService(GameTimeService.getInstance());

    public static GameLoopService getInstance() {
        return INSTANCE;
    }

    private static final String NAME = "game-loop";
    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private final Optional<Runnable> fixedUpdateRunnable = Optional.of(() ->
            this.lastTimestamp = this.fixedUpdate(this.lastTimestamp)
    );


    private final GameTimeService gameTime;

    private long lastTimestamp;

    private GameLoopService(GameTimeService gameTime) {
        this.gameTime = gameTime;
        this.lastTimestamp = this.gameTime.getTime();
    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = this.gameTime.getTime();
        if (timestamp - lastTimestamp > Config.get().getUpdateFrequencyInMillis()) {
            Game.get()
                    .fixedUpdate(timestamp);

            return timestamp;
        }
        return lastTimestamp;
    }

    public void accept(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        lastTimestamp = fixedUpdate(lastTimestamp);
    }

    public String getName() {
        return NAME;
    }

    public Optional<Runnable> getFixedUpdateRunnable() {
        long timestamp = this.gameTime.getTime();
        if (timestamp - lastTimestamp > Config.get().getUpdateFrequencyInMillis()) {
            return fixedUpdateRunnable;
        }
        return Optional.empty();
    }

}
