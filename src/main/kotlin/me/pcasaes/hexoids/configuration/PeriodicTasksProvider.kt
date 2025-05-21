package me.pcasaes.hexoids.configuration

import io.quarkus.arc.properties.IfBuildProperty
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
    private val configurations: HexoidConfigurations
) {
    @Produces
    @Singleton
    @Named("StalledPlayersPeriodTask")
    @IfBuildProperty(
        name = "hexoids.config.core.domain.stalled-players.periodic-task.enabled",
        stringValue = "true",
        enableIfMissing = true
    )
    fun getStalledPlayersPeriodTask(): GamePeriodicTask {
        return StalledPlayersPeriodTask.create(this.gameQueue)
    }

    @Produces
    @Singleton
    @Named("GameLoopPeriodicTask")
    @IfBuildProperty(
        name = "hexoids.config.core.domain.game-loop.periodic-task.enabled",
        stringValue = "true",
        enableIfMissing = true
    )
    fun getGameLoopPeriodicTask(): GamePeriodicTask {
        return GameLoopPeriodicTask.create(
            this.gameQueue,
            this.gameLoopService,
            this.configurations.getUpdateFrequencyInMillis()
        )
    }
}
