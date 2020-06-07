package me.pcasaes.hexoids.domain.service;

import me.pcasaes.hexoids.domain.model.Config;
import me.pcasaes.hexoids.domain.model.Game;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to add events into the game loop.
 */
@ApplicationScoped
public class GameLoopService {

    private static final String NAME = "game-loop";
    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private final Optional<Runnable> fixedUpdateRunnable = Optional.of(() ->
            this.lastTimestamp = this.fixedUpdate(this.lastTimestamp)
    );


    private final LongSupplier gameTime;

    private long lastTimestamp;

    @Inject
    public GameLoopService(@GameTime LongSupplier gameTime) {
        this.gameTime = gameTime;
    }

    @PostConstruct
    public void start() {
        this.lastTimestamp = this.gameTime.getAsLong();
    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = this.gameTime.getAsLong();
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
        long timestamp = this.gameTime.getAsLong();
        if (timestamp - lastTimestamp > Config.get().getUpdateFrequencyInMillis()) {
            return fixedUpdateRunnable;
        }
        return Optional.empty();
    }

}
