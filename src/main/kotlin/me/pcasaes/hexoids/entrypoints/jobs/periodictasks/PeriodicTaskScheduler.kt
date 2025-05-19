package me.pcasaes.hexoids.entrypoints.jobs.periodictasks;

import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.interceptor.Interceptor;
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@ApplicationScoped
public class PeriodicTaskScheduler {

    private static final Logger LOGGER = Logger.getLogger(PeriodicTaskScheduler.class.getName());

    private final Instance<GamePeriodicTask> gamePeriodicTasksFactory;
    private final List<GamePeriodicTask> gamePeriodicTasks = Collections.synchronizedList(new ArrayList<>());

    private volatile ScheduledExecutorService lowFreqScheduledExecutorService;
    private volatile ScheduledExecutorService hiFreqScheduledExecutorService;

    public PeriodicTaskScheduler(@Any Instance<GamePeriodicTask> gamePeriodicTasksFactory) {
        this.gamePeriodicTasksFactory = gamePeriodicTasksFactory;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 900) StartupEvent event) {
        // eager load
    }

    @PostConstruct
    public void start() {
        gamePeriodicTasksFactory
                .stream()
                .forEach(gamePeriodicTasks::add);

        this.lowFreqScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.hiFreqScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        gamePeriodicTasks
                .forEach(task -> {
                    StringBuilder log = new StringBuilder("Starting periodic task ")
                            .append(task.getClass().getSimpleName())
                            .append(": type=");
                    ScheduledExecutorService scheduledExecutorService;
                    if (task.getTimeUnit().toMillis(task.getPeriod()) < 1000L) {
                        scheduledExecutorService = hiFreqScheduledExecutorService;
                        log.append("high freq");
                    } else {
                        scheduledExecutorService = lowFreqScheduledExecutorService;
                        log.append("low freq");
                    }

                    log.append(", delay: ").append(task.getDelay())
                            .append(", period: ").append(task.getPeriod())
                            .append(", timeUnit: ").append(task.getTimeUnit());

                    scheduledExecutorService.scheduleAtFixedRate(
                            task,
                            task.getDelay(),
                            task.getPeriod(),
                            task.getTimeUnit());

                    LOGGER.info(log.toString());
                });

    }

    @PreDestroy
    public void stop() {

        this.hiFreqScheduledExecutorService
                .shutdown();
        this.lowFreqScheduledExecutorService
                .shutdown();

        gamePeriodicTasks
                .forEach(gamePeriodicTasksFactory::destroy);
    }
}
