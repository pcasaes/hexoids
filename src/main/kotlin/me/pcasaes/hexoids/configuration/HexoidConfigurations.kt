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
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.beans.IntrospectionException
import java.beans.Introspector
import java.util.logging.Level
import java.util.logging.Logger


@ApplicationScoped
class HexoidConfigurations {
    private var inertiaDampenCoefficient = 0f

    private var updateFrequencyInMillis: Long = 0

    private var minMove = 0f

    private var playerMaxMove = 0f

    private var playerMaxAngleDivisor = 0f

    private var expungeSinceLastSpawnTimeout: Long = 0

    private var playerResetPosition: String = ""

    private var maxBolts = 0

    private var playerNameLength = 0

    private var boltMaxDuration = 0

    private var boltSpeed = 0f

    private var boltCollisionRadius = 0f

    private var boltInertiaEnabled: Boolean = false

    private var boltInertiaRejectionScale = 0f

    private var boltInertiaProjectionScale = 0f

    private var boltInertiaNegativeProjectionScale = 0f

    private var playerDestroyedShockwaveDistance = 0f

    private var playerDestroyedShockwaveDuration: Long = 0

    private var playerDestroyedShockwaveImpulse = 0f

    private var blackholeEventHorizonRadius = 0f

    private var blackholeGravityRadius = 0f

    private var blackholeGravityImpulse = 0f

    private var blackholeDampenFactor = 0f

    private var blackholeGenesisProbabilityFactor = 0

    private var blackholeTeleportProbability = 0f


    /**
     * This singleton will eager load before all others
     *
     * @param event
     */
    fun startup(@Observes @Priority(Interceptor.Priority.APPLICATION) event: StartupEvent?) {
        LOGGER.info("Eager load Configuration")
    }

    @PostConstruct
    fun start() {
        if (LOGGER.isLoggable(Level.INFO)) {
            try {
                for (pd in Introspector.getBeanInfo(HexoidConfigurations::class.java).getPropertyDescriptors()) {
                    if (pd.getReadMethod() != null && pd.getWriteMethod() != null && ("class" != pd.getName())) {
                        val annotations = pd.getWriteMethod().parameterAnnotations
                        if (annotations.size == 1 && annotations[0].size == 1 &&
                            annotations[0][0] is ConfigProperty
                        ) {
                            val configProperty =
                                annotations[0][0] as ConfigProperty
                            LOGGER.info(configProperty.name + "=" + pd.getReadMethod().invoke(this))
                        }
                    }
                }
            } catch (ex: ReflectiveOperationException) {
                LOGGER.warning { "Could not introspect config. " + ex }
            } catch (ex: IntrospectionException) {
                LOGGER.warning { "Could not introspect config. " + ex }
            } catch (ex: RuntimeException) {
                LOGGER.warning { "Could not introspect config. " + ex }
            }
        }

        Config.setUpdateFrequencyInMillis(getUpdateFrequencyInMillis())
        Config.setInertiaDampenCoefficient(getInertiaDampenCoefficient())
        Config.setMaxBolts(getMaxBolts())
        Config.setMinMove(getMinMove())
        Config.setExpungeSinceLastSpawnTimeout(getExpungeSinceLastSpawnTimeout())
        Config.setPlayerNameLength(getPlayerNameLength())
        Config.setPlayerMaxMove(getPlayerMaxMove())
        Config.setPlayerMaxAngleDivisor(getPlayerMaxAngleDivisor())
        Config.setPlayerResetPosition(getPlayerResetPosition())
        Config.setBoltMaxDuration(getBoltMaxDuration())
        Config.setBoltSpeed(getBoltSpeed())
        Config.setBoltCollisionRadius(getBoltCollisionRadius())
        Config.setBoltInertiaEnabled(isBoltInertiaEnabled())
        Config.setBoltInertiaRejectionScale(getBoltInertiaRejectionScale())
        Config.setBoltInertiaProjectionScale(getBoltInertiaProjectionScale())
        Config.setBoltInertiaNegativeProjectionScale(getBoltInertiaNegativeProjectionScale())
        Config.getPlayerDestroyedShockwave().setDistance(getPlayerDestroyedShockwaveDistance())
        Config.getPlayerDestroyedShockwave().setDuration(getPlayerDestroyedShockwaveDuration())
        Config.getPlayerDestroyedShockwave().setImpulse(getPlayerDestroyedShockwaveImpulse())
        Config.getBlackhole().setDampenFactor(getBlackholeDampenFactor())
        Config.getBlackhole().setEventHorizonRadius(getBlackholeEventHorizonRadius())
        Config.getBlackhole().setGenesisProbabilityFactor(getBlackholeGenesisProbabilityFactor())
        Config.getBlackhole().setGravityImpulse(getBlackholeGravityImpulse())
        Config.getBlackhole().setGravityRadius(getBlackholeGravityRadius())
        Config.getBlackhole().setTeleportProbability(getBlackholeTeleportProbability())
    }


    fun getInertiaDampenCoefficient(): Float {
        return inertiaDampenCoefficient
    }

    @Inject
    fun setPlayerSpatialIndex(playerSpatialIndex: PlayerSpatialIndex) {
        PlayerSpatialIndexFactory.factory().setPlayerSpatialIndex(playerSpatialIndex)
    }

    @Inject
    fun setBarrierSpatialIndex(barrierSpatialIndex: BarrierSpatialIndex) {
        BarrierSpatialIndexFactory.factory().setBarrierSpatialIndex(barrierSpatialIndex)
    }

    @Inject
    fun setInertiaDampenCoefficient(
        @ConfigProperty(
            name = "hexoids.config.inertia.dampen-coefficient",
            defaultValue = "-0.001"
        ) inertiaDampenCoefficient: Float
    ) {
        this.inertiaDampenCoefficient = inertiaDampenCoefficient
    }

    fun getUpdateFrequencyInMillis(): Long {
        return updateFrequencyInMillis
    }

    @Inject
    fun setUpdateFrequencyInMillis(
        @ConfigProperty(
            name = "hexoids.config.update-frequency-in-millis",
            defaultValue = "50"
        ) updateFrequencyInMillis: Long
    ) {
        this.updateFrequencyInMillis = updateFrequencyInMillis
    }

    fun getMinMove(): Float {
        return minMove
    }

    @Inject
    fun setMinMove(
        @ConfigProperty(name = "hexoids.config.min.move", defaultValue = "0.0001") minMove: Float
    ) {
        this.minMove = minMove
    }

    fun getPlayerMaxMove(): Float {
        return playerMaxMove
    }

    @Inject
    fun setPlayerMaxMove(
        @ConfigProperty(name = "hexoids.config.player.max.move", defaultValue = "10") playerMaxMove: Float
    ) {
        this.playerMaxMove = playerMaxMove
    }

    fun getPlayerMaxAngleDivisor(): Float {
        return playerMaxAngleDivisor
    }

    @Inject
    fun setPlayerMaxAngleDivisor(
        @ConfigProperty(
            name = "hexoids.config.player.max.angle.divisor",
            defaultValue = "4"
        ) playerMaxAngleDivisor: Float
    ) {
        this.playerMaxAngleDivisor = playerMaxAngleDivisor
    }

    fun getExpungeSinceLastSpawnTimeout(): Long {
        return expungeSinceLastSpawnTimeout
    }

    @Inject
    fun setExpungeSinceLastSpawnTimeout(
        @ConfigProperty(
            name = "hexoids.config.player.expunge-since-last-spawn-timeout",
            defaultValue = "60000"
        ) expungeSinceLastSpawnTimeout: Long
    ) {
        this.expungeSinceLastSpawnTimeout = expungeSinceLastSpawnTimeout
    }

    fun getPlayerResetPosition(): String {
        return playerResetPosition
    }

    @Inject
    fun setPlayerResetPosition(
        @ConfigProperty(
            name = "hexoids.config.player.reset.position",
            defaultValue = "rng"
        ) playerResetPosition: String
    ) {
        this.playerResetPosition = playerResetPosition
    }

    fun getMaxBolts(): Int {
        return maxBolts
    }

    @Inject
    fun setMaxBolts(
        @ConfigProperty(name = "hexoids.config.player.max.bolts", defaultValue = "10") maxBolts: Int
    ) {
        this.maxBolts = maxBolts
    }

    fun getPlayerNameLength(): Int {
        return playerNameLength
    }

    @Inject
    fun setPlayerNameLength(
        @ConfigProperty(name = "hexoids.config.player.name-length", defaultValue = "7") playerNameLength: Int
    ) {
        this.playerNameLength = playerNameLength
    }

    fun getBoltMaxDuration(): Int {
        return boltMaxDuration
    }

    @Inject
    fun setBoltMaxDuration(
        @ConfigProperty(name = "hexoids.config.bolt.max.duration", defaultValue = "10000") boltMaxDuration: Int
    ) {
        this.boltMaxDuration = boltMaxDuration
    }

    fun getBoltSpeed(): Float {
        return boltSpeed
    }

    @Inject
    fun setBoltSpeed(
        @ConfigProperty(name = "hexoids.config.bolt.speed", defaultValue = "0.07") boltSpeed: Float
    ) {
        this.boltSpeed = boltSpeed
    }

    fun getBoltCollisionRadius(): Float {
        return boltCollisionRadius
    }

    @Inject
    fun setBoltCollisionRadius(
        @ConfigProperty(
            name = "hexoids.config.bolt.collision.radius",
            defaultValue = "0.001"
        ) boltCollisionRadius: Float
    ) {
        this.boltCollisionRadius = boltCollisionRadius
    }

    fun isBoltInertiaEnabled(): Boolean {
        return boltInertiaEnabled
    }

    @Inject
    fun setBoltInertiaEnabled(
        @ConfigProperty(name = "hexoids.config.bolt.inertia.enabled", defaultValue = "true") boltInertiaEnabled: Boolean
    ) {
        this.boltInertiaEnabled = boltInertiaEnabled
    }

    fun getBoltInertiaRejectionScale(): Float {
        return boltInertiaRejectionScale
    }

    @Inject
    fun setBoltInertiaRejectionScale(
        @ConfigProperty(
            name = "hexoids.config.bolt.inertia.rejection-scale",
            defaultValue = "0.8"
        ) boltInertiaRejectionScale: Float
    ) {
        this.boltInertiaRejectionScale = boltInertiaRejectionScale
    }

    fun getBoltInertiaProjectionScale(): Float {
        return boltInertiaProjectionScale
    }

    @Inject
    fun setBoltInertiaProjectionScale(
        @ConfigProperty(
            name = "hexoids.config.bolt.inertia.projection-scale",
            defaultValue = "0.8"
        ) boltInertiaProjectionScale: Float
    ) {
        this.boltInertiaProjectionScale = boltInertiaProjectionScale
    }

    fun getBoltInertiaNegativeProjectionScale(): Float {
        return boltInertiaNegativeProjectionScale
    }

    @Inject
    fun setBoltInertiaNegativeProjectionScale(
        @ConfigProperty(
            name = "hexoids.config.bolt.inertia.negative-projection-scale",
            defaultValue = "0.1"
        ) boltInertiaNegativeProjectionScale: Float
    ) {
        this.boltInertiaNegativeProjectionScale = boltInertiaNegativeProjectionScale
    }

    fun getPlayerDestroyedShockwaveDistance(): Float {
        return playerDestroyedShockwaveDistance
    }

    @Inject
    fun setPlayerDestroyedShockwaveDistance(
        @ConfigProperty(
            name = "hexoids.config.player.destroyed.shockwave.distance",
            defaultValue = "0.0408"
        ) playerDestroyedShockwaveDistance: Float
    ) {
        this.playerDestroyedShockwaveDistance = playerDestroyedShockwaveDistance
    }

    fun getPlayerDestroyedShockwaveDuration(): Long {
        return playerDestroyedShockwaveDuration
    }

    @Inject
    fun setPlayerDestroyedShockwaveDuration(
        @ConfigProperty(
            name = "hexoids.config.player.destroyed.shockwave.duration.ms",
            defaultValue = "400"
        ) playerDestroyedShockwaveDuration: Long
    ) {
        this.playerDestroyedShockwaveDuration = playerDestroyedShockwaveDuration
    }

    fun getPlayerDestroyedShockwaveImpulse(): Float {
        return playerDestroyedShockwaveImpulse
    }

    @Inject
    fun setPlayerDestroyedShockwaveImpulse(
        @ConfigProperty(
            name = "hexoids.config.player.destroyed.shockwave.impulse",
            defaultValue = "0.007"
        ) playerDestroyedShockwaveImpulse: Float
    ) {
        this.playerDestroyedShockwaveImpulse = playerDestroyedShockwaveImpulse
    }

    fun getBlackholeEventHorizonRadius(): Float {
        return blackholeEventHorizonRadius
    }

    @Inject
    fun setBlackholeEventHorizonRadius(
        @ConfigProperty(
            name = "hexoids.config.blackhole.eventhorizon.radius",
            defaultValue = "0.005"
        ) blackholeEventHorizonRadius: Float
    ) {
        this.blackholeEventHorizonRadius = blackholeEventHorizonRadius
    }

    fun getBlackholeGravityRadius(): Float {
        return blackholeGravityRadius
    }

    @Inject
    fun setBlackholeGravityRadius(
        @ConfigProperty(
            name = "hexoids.config.blackhole.gravity.radius",
            defaultValue = "0.07"
        ) blackholeGravityRadius: Float
    ) {
        this.blackholeGravityRadius = blackholeGravityRadius
    }

    fun getBlackholeGravityImpulse(): Float {
        return blackholeGravityImpulse
    }

    @Inject
    fun setBlackholeGravityImpulse(
        @ConfigProperty(
            name = "hexoids.config.blackhole.gravity.impulse",
            defaultValue = "0.07"
        ) blackholeGravityImpulse: Float
    ) {
        this.blackholeGravityImpulse = blackholeGravityImpulse
    }

    fun getBlackholeDampenFactor(): Float {
        return blackholeDampenFactor
    }

    @Inject
    fun setBlackholeDampenFactor(
        @ConfigProperty(
            name = "hexoids.config.blackhole.dampen.factor",
            defaultValue = "5"
        ) blackholeDampenFactor: Float
    ) {
        this.blackholeDampenFactor = blackholeDampenFactor
    }

    fun getBlackholeGenesisProbabilityFactor(): Int {
        return blackholeGenesisProbabilityFactor
    }

    @Inject
    fun setBlackholeGenesisProbabilityFactor(
        @ConfigProperty(
            name = "hexoids.config.blackhole.genesis.probability.factor",
            defaultValue = "3"
        ) blackholeGenesisProbabilityFactor: Int
    ) {
        this.blackholeGenesisProbabilityFactor = blackholeGenesisProbabilityFactor
    }

    fun getBlackholeTeleportProbability(): Float {
        return blackholeTeleportProbability
    }

    @Inject
    fun setBlackholeTeleportProbability(
        @ConfigProperty(
            name = "hexoids.config.blackhole.teleport.probability",
            defaultValue = "0.05"
        ) blackholeTeleportProbability: Float
    ) {
        this.blackholeTeleportProbability = blackholeTeleportProbability
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(HexoidConfigurations::class.java.getName())
    }
}
