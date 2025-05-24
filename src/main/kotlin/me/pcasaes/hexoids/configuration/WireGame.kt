package me.pcasaes.hexoids.configuration

import io.quarkus.runtime.StartupEvent
import jakarta.annotation.PostConstruct
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndex
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndexFactory
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndexFactory
import java.util.logging.Logger


@ApplicationScoped
class WireGame @Inject constructor(
    private val configuration: Configuration,
    private val playerSpatialIndex: PlayerSpatialIndex,
    private val barrierSpatialIndex: BarrierSpatialIndex,
) {
    /**
     * This singleton will eager load before all others
     *
     * @param event
     */
    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION) event: StartupEvent?) {
        LOGGER.info("Wiring Game")
    }

    @PostConstruct
    fun start() {

        Config.setUpdateFrequencyInMillis(configuration.updateFrequencyInMillis())
        Config.setInertiaDampenCoefficient(configuration.inertia().dampenCoefficient())
        Config.setMaxBolts(configuration.player().maxBolts())
        Config.setMinMove(configuration.minMove())
        Config.setExpungeSinceLastSpawnTimeout(configuration.player().expungeSinceLastSpawnTimeout())
        Config.setPlayerNameLength(configuration.player().nameLength())
        Config.setPlayerMaxMove(configuration.player().maxMove())
        Config.setPlayerMaxAngleDivisor(configuration.player().maxAngleDivisor())
        Config.setPlayerResetPosition(configuration.player().resetPosition())
        Config.setBoltMaxDuration(configuration.bolt().maxDuration())
        Config.setBoltSpeed(configuration.bolt().speed())
        Config.setBoltCollisionRadius(configuration.bolt().collisionRadius())
        Config.setBoltInertiaEnabled(configuration.bolt().inertiaEnabled())
        Config.setBoltInertiaRejectionScale(configuration.bolt().inertiaRejectionScale())
        Config.setBoltInertiaProjectionScale(configuration.bolt().inertiaProjectionScale())
        Config.setBoltInertiaNegativeProjectionScale(configuration.bolt().inertiaNegativeProjectionScale())
        Config.getPlayerDestroyedShockwave().setDistance(configuration.player().destroyed().shockwaveDistance())
        Config.getPlayerDestroyedShockwave().setDuration(configuration.player().destroyed().shockwaveDurationMs())
        Config.getPlayerDestroyedShockwave().setImpulse(configuration.player().destroyed().shockwaveImpulse())
        Config.getBlackhole().setDampenFactor(configuration.blackhole().dampenFactor())
        Config.getBlackhole().setEventHorizonRadius(configuration.blackhole().eventHorizonRadius())
        Config.getBlackhole().setGenesisProbabilityFactor(configuration.blackhole().genesisProbabilityFactor())
        Config.getBlackhole().setGravityImpulse(configuration.blackhole().gravityImpulse())
        Config.getBlackhole().setGravityRadius(configuration.blackhole().gravityRadius())
        Config.getBlackhole().setTeleportProbability(configuration.blackhole().teleportProbability())


        PlayerSpatialIndexFactory.setPlayerSpatialIndex(playerSpatialIndex)
        BarrierSpatialIndexFactory.setBarrierSpatialIndex(barrierSpatialIndex)

    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(WireGame::class.java.getName())
    }
}
