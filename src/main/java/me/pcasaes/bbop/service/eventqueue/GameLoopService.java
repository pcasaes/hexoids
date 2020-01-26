package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.GameEvents;
import me.pcasaes.bbop.service.ConfigurationService;
import pcasaes.bbop.proto.Dto;
import pcasaes.bbop.proto.Event;
import pcasaes.bbop.proto.Sleep;

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


    private final ThreadService threadService;
    private final ConfigurationService configurationService;
    private final Sleep.Builder sleepBuilder;
    private final Event.Builder eventBuilder;
    private final Dto.Builder dtoBuilder;

    private long lastTimestamp;

    GameLoopService() {
        this.threadService = null;
        this.configurationService = null;
        this.sleepBuilder = null;
        this.eventBuilder = null;
        this.dtoBuilder = null;
    }

    @Inject
    public GameLoopService(ThreadService threadService, ConfigurationService configurationService) {
        this.threadService = threadService;
        this.configurationService = configurationService;
        this.sleepBuilder = Sleep.newBuilder();
        this.eventBuilder = Event.newBuilder();
        this.dtoBuilder = Dto.newBuilder();
    }

    @PostConstruct
    public void start() {
        this.lastTimestamp = Game.get().getClock().getTime();
        threadService.setGameLoopThread();
    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = Game.get().getClock().getTime();
        if (timestamp - lastTimestamp > UPDATE_DELTA) {
            Game.get()
                    .fixedUpdate(timestamp);

            return timestamp;
        }
        return lastTimestamp;
    }

    @Override
    public long getWaitTime() {
        long timeSinceFixedUpdate = Game.get().getClock().getTime() - lastTimestamp;
        return timeSinceFixedUpdate % UPDATE_DELTA;
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

        Sleep sleepDto = sleepBuilder
                .clear()
                .setSleepUntil(Game.get().getClock().getTime() + getWaitTime())
                .build();
        GameEvents.getDomainEvents().register(DomainEvent.withoutKey(eventBuilder.clear().setSleep(sleepDto).build()));
        GameEvents.getClientEvents().register(dtoBuilder.clear().setSleep(sleepDto).build());
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
