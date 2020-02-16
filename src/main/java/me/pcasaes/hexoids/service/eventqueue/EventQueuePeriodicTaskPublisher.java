package me.pcasaes.hexoids.service.eventqueue;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.model.Config;
import me.pcasaes.hexoids.model.Game;
import me.pcasaes.hexoids.service.ConfigurationService;
import pcasaes.hexoids.proto.Dto;
import pcasaes.hexoids.proto.Flush;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class EventQueuePeriodicTaskPublisher {

    private static final Dto FLUSH = Dto
            .newBuilder()
            .setFlush(Flush.newBuilder())
            .build();

    private final DisruptorService disruptorService;

    private final GameLoopService gameLoopService;
    private final ClientBroadcastService clientBroadcastService;

    private final ConfigurationService configurationService;

    private ScheduledExecutorService scheduledExecutorService;

    private long lastGamePublish;
    private long lastClientFlush;


    public EventQueuePeriodicTaskPublisher() {
        this.disruptorService = null;
        this.gameLoopService = null;
        this.clientBroadcastService = null;
        this.configurationService = null;
    }

    @Inject
    public EventQueuePeriodicTaskPublisher(DisruptorService disruptorService,
                                           GameLoopService gameLoopService,
                                           ClientBroadcastService clientBroadcastService,
                                           ConfigurationService configurationService) {
        this.disruptorService = disruptorService;
        this.gameLoopService = gameLoopService;
        this.clientBroadcastService = clientBroadcastService;
        this.configurationService = configurationService;
    }

    public void startup(@Observes StartupEvent event) {
        //eager load
    }


    @PostConstruct
    public void start() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        scheduledExecutorService.scheduleAtFixedRate(
                this::gameLoopPeriodicTask,
                1000,
                this.configurationService.getUpdateFrequencyInMillis(),
                TimeUnit.MILLISECONDS);

        scheduledExecutorService.scheduleAtFixedRate(
                this::clientFlushPeriodicTask,
                1000,
                this.clientBroadcastService.getBatchTimeout(),
                TimeUnit.MILLISECONDS);
    }

    private void gameLoopPeriodicTask() {
        if (Game.get().getClock().getTime() - lastGamePublish > Config.get().getUpdateFrequencyInMillis()) {
            gameLoopService.getFixedUpdateRunnable().ifPresent(this::publish);
        }
    }

    private void publish(GameLoopService.GameRunnable event) {
        this.lastGamePublish = Game.get().getClock().getTime();
        disruptorService.enqueueGame(event);
    }

    private void clientFlushPeriodicTask() {
        if (Game.get().getClock().getTime() - lastClientFlush > clientBroadcastService.getBatchTimeout() &&
                clientBroadcastService.canFlush()) {
            flush();
        }
    }


    private void flush() {
        this.lastClientFlush = Game.get().getClock().getTime();
        disruptorService.enqueueClient(FLUSH);
    }

    @PreDestroy
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
