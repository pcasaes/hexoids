package me.pcasaes.bbop.service.eventqueue;

import me.pcasaes.bbop.dto.Dto;
import me.pcasaes.bbop.dto.EventDto;
import me.pcasaes.bbop.dto.EventType;
import me.pcasaes.bbop.model.DomainEvent;
import me.pcasaes.bbop.model.Game;
import me.pcasaes.bbop.model.GameEvents;
import me.pcasaes.bbop.service.ConfigurationService;

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

    private long lastTimestamp;

    GameLoopService() {
        this.threadService = null;
        this.configurationService = null;
    }

    @Inject
    public GameLoopService(ThreadService threadService, ConfigurationService configurationService) {
        this.threadService = threadService;
        this.configurationService = configurationService;
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

        SleepDto sleepDto = SleepDto.sleepUntil(Game.get().getClock().getTime() + getWaitTime());
        GameEvents.getDomainEvents().register(DomainEvent.withoutKey(sleepDto));
        GameEvents.getClientEvents().register(sleepDto);
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

    /**
     * Special Dto that is not transmitted and instead is used to sleep dependent queues
     */
    static class SleepDto implements EventDto {

        private final long sleepUntil;

        public SleepDto(long sleepUntil) {
            this.sleepUntil = sleepUntil;
        }

        public static SleepDto sleepUntil(long sleepUntil) {
            return new SleepDto(sleepUntil);
        }

        public long getSleepUntil() {
            return sleepUntil;
        }

        @Override
        public EventType getEvent() {
            return null;
        }

        @Override
        public Type getDtoType() {
            return DtoType.SLEEP_DTO;
        }

        enum DtoType implements Dto.Type {
            SLEEP_DTO;
        }

    }
}