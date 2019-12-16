package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.model.Clock;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class GameLoopService {

    private static final long UPDATE_DELTA = 50L;

    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private EventQueue<Runnable> eventQueue;

    private final ConfigurationService configurationService;

    GameLoopService() {
        this.configurationService = null;
    }

    @Inject
    public GameLoopService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private Thread thread;
    private boolean running = true;

    public void enqueue(Runnable runnable) {
        eventQueue.produce(runnable);
    }

    @PostConstruct
    public void start() {
        this.eventQueue = EventQueue.Factory.createMultipleProducerSingleConsumerEventQueue(
                configurationService.isGameLoopUseLinkedList(),
                configurationService.getGameLoopMaxSizeExponent());

        thread = new Thread(this::run);
        thread.setName(GameLoopService.class.getSimpleName());
        thread.setDaemon(true);
        thread.start();
    }

    @PreDestroy
    public void stop() {
        this.running = false;
        try {
            thread.join(5_000L);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        }
    }

    private long fixedUpdate(long lastTimestamp) {
        long timestamp = Clock.get().getTime();
        if (timestamp - lastTimestamp > UPDATE_DELTA) {
            Game.get()
                    .fixedUpdate(timestamp);

            return timestamp;
        }
        return lastTimestamp;
    }

    private void run() {
        long lastTimestamp = Clock.get().getTime();
        while (running) {
            lastTimestamp = fixedUpdate(lastTimestamp);

            Runnable runnable;
            while ((runnable = eventQueue.consume()) != null) {
                try {
                    runnable.run();
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
                lastTimestamp = fixedUpdate(lastTimestamp);
            }

            sleep(lastTimestamp);
        }
    }

    private void sleep(long lastTimestamp) {
        long timeSinceFixedUpdate = Clock.get().getTime();
        long sleepTime = (timeSinceFixedUpdate - lastTimestamp) % UPDATE_DELTA;
        if (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

}
