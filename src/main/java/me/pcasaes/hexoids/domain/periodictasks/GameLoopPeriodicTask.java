package me.pcasaes.hexoids.domain.periodictasks;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.domain.service.GameLoopService;
import me.pcasaes.hexoids.domain.eventqueue.GameQueue;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GameLoopPeriodicTask {

    private final GameQueue gameQueue;

    private final GameLoopService gameLoopService;

    private final long period;

    private volatile ScheduledExecutorService scheduledExecutorService;


    @Inject
    public GameLoopPeriodicTask(GameQueue gameQueue,
                                GameLoopService gameLoopService,
                                @ConfigProperty(
                                        name = "hexoids.config.update-frequency-in-millis",
                                        defaultValue = "50"
                                ) long period) {
        this.gameQueue = gameQueue;
        this.gameLoopService = gameLoopService;
        this.period = period;
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
                this.period,
                TimeUnit.MILLISECONDS);
    }

    private void gameLoopPeriodicTask() {
        gameLoopService
                .getFixedUpdateRunnable()
                .ifPresent(this::publish);
    }

    private void publish(Runnable event) {
        this.gameQueue.enqueue(event);
    }

    @PreDestroy
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
