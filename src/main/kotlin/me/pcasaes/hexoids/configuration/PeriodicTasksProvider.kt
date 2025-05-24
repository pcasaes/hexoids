package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.periodictasks.GameLoopPeriodicTask
import me.pcasaes.hexoids.core.domain.periodictasks.GamePeriodicTask
import me.pcasaes.hexoids.core.domain.periodictasks.StalledPlayersPeriodTask
import me.pcasaes.hexoids.core.domain.service.GameLoopService

@ApplicationScoped
class PeriodicTasksProvider @Inject constructor(
    private val gameQueue: GameQueue,
    private val gameLoopService: GameLoopService,
    private val configurations: Configuration
) {
    @Produces
    @Singleton
    @Named("StalledPlayersPeriodTask")
    fun getStalledPlayersPeriodTask(): GamePeriodicTask {
        return StalledPlayersPeriodTask.create(this.gameQueue)
    }

    @Produces
    @Singleton
    @Named("GameLoopPeriodicTask")
    fun getGameLoopPeriodicTask(): GamePeriodicTask {
        return GameLoopPeriodicTask.create(
            this.gameQueue,
            this.gameLoopService,
            this.configurations.updateFrequencyInMillis()
        )
    }
}
