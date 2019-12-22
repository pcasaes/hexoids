package me.paulo.casaes.bbop.service.eventqueue;

import me.paulo.casaes.bbop.model.Clock;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.service.ConfigurationService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to add events into the game loop.
 */
@ApplicationScoped
public class GameLoopService implements EventQueueConsumerService<GameLoopService.GameRunnable> {

    private static final long UPDATE_DELTA = 50L;

    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());


    private final ConfigurationService configurationService;

    private long lastTimestamp;

    GameLoopService() {
        this.configurationService = null;
    }

    @Inject
    public GameLoopService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void start() {
        this.lastTimestamp = Clock.Factory.get().getTime();

    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = Clock.Factory.get().getTime();
        if (timestamp - lastTimestamp > UPDATE_DELTA) {
            Game.get()
                    .fixedUpdate(timestamp);

            return timestamp;
        }
        return lastTimestamp;
    }

    @Override
    public long getWaitTime() {
        long timeSinceFixedUpdate = Clock.Factory.get().getTime();
        return (timeSinceFixedUpdate - lastTimestamp) % UPDATE_DELTA;
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
    public void empty() {
        lastTimestamp = fixedUpdate(lastTimestamp);
    }

    @Override
    public boolean useLinkedList() {
        return configurationService.isGameLoopUseLinkedList();
    }

    @Override
    public boolean useSingleProducer() {
        return false;
    }

    @Override
    public int getMaxSizeExponent() {
        return configurationService.getGameLoopMaxSizeExponent();
    }

    @Override
    public String getName() {
        return GameLoopService.class.getSimpleName();
    }

    @Override
    public Class<?> getEventType() {
        return GameRunnable.class;
    }

    public interface GameRunnable extends Runnable {

    }
}
