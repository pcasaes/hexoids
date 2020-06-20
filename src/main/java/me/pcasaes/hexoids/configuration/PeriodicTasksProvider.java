package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.periodictasks.GameLoopPeriodicTask;
import me.pcasaes.hexoids.core.domain.periodictasks.StalledPlayersPeriodTask;
import me.pcasaes.hexoids.core.domain.service.GameLoopService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@ApplicationScoped
public class PeriodicTasksProvider {

    private final GameQueue gameQueue;

    private final GameLoopService gameLoopService;

    private final HexoidConfigurations configurations;

    @Inject
    public PeriodicTasksProvider(GameQueue gameQueue,
                                 GameLoopService gameLoopService,
                                 HexoidConfigurations configurations) {
        this.gameQueue = gameQueue;
        this.gameLoopService = gameLoopService;
        this.configurations = configurations;
    }

    @Produces
    @Singleton
    public StalledPlayersPeriodTask getStalledPlayersPeriodTask() {
        return StalledPlayersPeriodTask.create(this.gameQueue);
    }

    @Produces
    @Singleton
    public GameLoopPeriodicTask getGameLoopPeriodicTask() {
        return GameLoopPeriodicTask.create(this.gameQueue, this.gameLoopService, this.configurations.getUpdateFrequencyInMillis());
    }
}
