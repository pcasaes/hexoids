package me.paulo.casaes.bbop.service;

import me.paulo.casaes.bbop.dto.Dto;
import me.paulo.casaes.bbop.model.Clock;
import me.paulo.casaes.bbop.model.Game;
import me.paulo.casaes.bbop.util.concurrent.eventqueue.EventQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class GameLoopService {

    private static final long UPDATE_DELTA = 50L;

    private static final Logger LOGGER = Logger.getLogger(GameLoopService.class.getName());

    private EventQueue<Supplier<List<? extends Dto>>> eventQueue;

    private final ClientBroadcastService clientBroadcastService;

    private final ConfigurationService configurationService;

    GameLoopService() {
        this.clientBroadcastService = null;
        this.configurationService = null;
    }

    @Inject
    public GameLoopService(ClientBroadcastService clientBroadcastService, ConfigurationService configurationService) {
        this.clientBroadcastService = clientBroadcastService;
        this.configurationService = configurationService;
    }

    private Thread thread;
    private boolean running = true;

    public void enqueue(Supplier<List<? extends Dto>> runnable) {
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
                    .fixedUpdate(timestamp)
                    .forEach(clientBroadcastService::broadcast);

            return timestamp;
        }
        return lastTimestamp;
    }

    private void run() {
        long lastTimestamp = Clock.get().getTime();
        while (running) {
            lastTimestamp = fixedUpdate(lastTimestamp);

            Supplier<List<? extends Dto>> runnable;
            while ((runnable = eventQueue.consume()) != null) {
                try {
                    runnable
                            .get()
                            .forEach(clientBroadcastService::broadcast);
                } catch (RuntimeException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

}
