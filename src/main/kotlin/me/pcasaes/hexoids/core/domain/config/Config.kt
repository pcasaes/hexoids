package me.pcasaes.hexoids.core.domain.config

class Config {

    companion object {

        fun get(): Config {
            return ConfigHolder.INSTANCE
        }
    }

    private var updateFrequencyInMillis: Long = 0
    private var updateFrequencyInMillisWithAdded20Percent = 0F
    private var updateFrequencyInMillisWithSubstract10Percent = 0F
    private var inertiaDampenCoefficient = 0F

    /**
     * The smallest value for a valid magnitude. Should be 1/boundSizeInPixels
     */
    private var minMove = 0F
    private var playerNameLength = 0
    private var playerMaxMove = 0F
    private var playerMaxAngle = 0F
    private var playerResetPosition: String = ""
    private var maxBolts = 0
    private var boltMaxDuration = 0
    private var boltSpeed = 0F
    private var boltCollisionRadius = 0F
    var isBoltInertiaEnabled: Boolean = false
        private set
    private var boltInertiaRejectionScale = 0F
    private var boltInertiaProjectionScale = 0F
    private var boltInertiaNegativeProjectionScale = 0F
    private var expungeSinceLastSpawnTimeout: Long = 0

    private var boltCollisionIndexSearchDistance = 0F

    private val playerDestroyedShockwave = PlayerDestroyedShockwave()

    private val blackhole = Blackhole()

    private object ConfigHolder {
        val INSTANCE: Config = Config()
    }

    fun getUpdateFrequencyInMillis(): Long {
        return updateFrequencyInMillis
    }

    fun setUpdateFrequencyInMillis(updateFrequencyInMillis: Long) {
        this.updateFrequencyInMillis = updateFrequencyInMillis
        this.updateFrequencyInMillisWithAdded20Percent = updateFrequencyInMillis * 1.2f
        this.updateFrequencyInMillisWithSubstract10Percent = updateFrequencyInMillis * 0.9f
    }

    fun getUpdateFrequencyInMillisWithAdded20Percent(): Float {
        return updateFrequencyInMillisWithAdded20Percent
    }

    fun getUpdateFrequencyInMillisWithSubstract10Percent(): Float {
        return updateFrequencyInMillisWithSubstract10Percent
    }

    fun getInertiaDampenCoefficient(): Float {
        return inertiaDampenCoefficient
    }

    fun setInertiaDampenCoefficient(inertiaDampenCoefficient: Float) {
        this.inertiaDampenCoefficient = inertiaDampenCoefficient
    }

    fun getMaxBolts(): Int {
        return maxBolts
    }

    fun getMinMove(): Float {
        return minMove
    }

    fun setMinMove(minMove: Float) {
        this.minMove = minMove
    }

    fun getPlayerMaxMove(): Float {
        return playerMaxMove
    }

    fun getPlayerMaxAngle(): Float {
        return playerMaxAngle
    }

    fun getPlayerNameLength(): Int {
        return playerNameLength
    }

    fun setPlayerNameLength(playerNameLength: Int) {
        this.playerNameLength = playerNameLength
    }

    fun setPlayerMaxAngleDivisor(playerMaxAngleDivisor: Float) {
        this.playerMaxAngle = Math.PI.toFloat() / playerMaxAngleDivisor
    }

    fun setPlayerMaxMove(playerMaxMove: Float) {
        this.playerMaxMove = playerMaxMove
    }

    fun getPlayerResetPosition(): String {
        return playerResetPosition
    }

    fun setPlayerResetPosition(playerResetPosition: String) {
        this.playerResetPosition = playerResetPosition
    }

    fun setMaxBolts(maxBolts: Int) {
        this.maxBolts = maxBolts
    }

    fun getBoltMaxDuration(): Int {
        return boltMaxDuration
    }

    fun setBoltMaxDuration(boltMaxDuration: Int) {
        this.boltMaxDuration = boltMaxDuration
    }

    fun getBoltSpeed(): Float {
        return boltSpeed
    }

    fun setBoltSpeed(boltSpeed: Float) {
        this.boltSpeed = boltSpeed
    }

    fun getBoltCollisionRadius(): Float {
        return boltCollisionRadius
    }

    fun setBoltCollisionRadius(boltCollisionRadius: Float) {
        this.boltCollisionIndexSearchDistance = 100F * boltCollisionRadius
        this.boltCollisionRadius = boltCollisionRadius
    }

    fun setBoltInertiaEnabled(boltInertiaEnabled: Boolean) {
        this.isBoltInertiaEnabled = boltInertiaEnabled
    }

    fun getBoltInertiaRejectionScale(): Float {
        return boltInertiaRejectionScale
    }

    fun setBoltInertiaRejectionScale(boltInertiaRejectionScale: Float) {
        this.boltInertiaRejectionScale = boltInertiaRejectionScale
    }

    fun getBoltInertiaProjectionScale(): Float {
        return boltInertiaProjectionScale
    }

    fun getBoltInertiaNegativeProjectionScale(): Float {
        return boltInertiaNegativeProjectionScale
    }

    fun setBoltInertiaNegativeProjectionScale(boltInertiaNegativeProjectionScale: Float) {
        this.boltInertiaNegativeProjectionScale = boltInertiaNegativeProjectionScale
    }

    fun setBoltInertiaProjectionScale(boltInertiaProjectionScale: Float) {
        this.boltInertiaProjectionScale = boltInertiaProjectionScale
    }

    fun getExpungeSinceLastSpawnTimeout(): Long {
        return expungeSinceLastSpawnTimeout
    }

    fun setExpungeSinceLastSpawnTimeout(expungeSinceLastSpawnTimeout: Long) {
        this.expungeSinceLastSpawnTimeout = expungeSinceLastSpawnTimeout
    }

    fun getBoltCollisionIndexSearchDistance(): Float {
        return boltCollisionIndexSearchDistance
    }

    fun getPlayerDestroyedShockwave(): PlayerDestroyedShockwave {
        return playerDestroyedShockwave
    }

    fun getBlackhole(): Blackhole {
        return blackhole
    }

    class PlayerDestroyedShockwave internal constructor() {
        private var distance = 0F
        private var duration: Long = 0
        private var impulse = 0F

        fun getDistance(): Float {
            return distance
        }

        fun setDistance(distance: Float) {
            this.distance = distance
        }

        fun getDuration(): Long {
            return duration
        }

        fun setDuration(duration: Long) {
            this.duration = duration
        }

        fun getImpulse(): Float {
            return impulse
        }

        fun setImpulse(impulse: Float) {
            this.impulse = impulse
        }
    }

    class Blackhole {
        private var eventHorizonRadius = 0F
        private var gravityRadius = 0F
        private var gravityImpulse = 0F
        private var dampenFactor = 0F
        private var genesisProbabilityFactor = 0
        private var teleportProbability = 0F

        fun getEventHorizonRadius(): Float {
            return eventHorizonRadius
        }

        fun setEventHorizonRadius(eventHorizonRadius: Float) {
            this.eventHorizonRadius = eventHorizonRadius
        }

        fun getGravityRadius(): Float {
            return gravityRadius
        }

        fun setGravityRadius(gravityRadius: Float) {
            this.gravityRadius = gravityRadius
        }

        fun getGravityImpulse(): Float {
            return gravityImpulse
        }

        fun setGravityImpulse(gravityImpulse: Float) {
            this.gravityImpulse = gravityImpulse
        }

        fun getDampenFactor(): Float {
            return dampenFactor
        }

        fun setDampenFactor(dampenFactor: Float) {
            this.dampenFactor = dampenFactor
        }

        fun getGenesisProbabilityFactor(): Int {
            return genesisProbabilityFactor
        }

        fun setGenesisProbabilityFactor(genesisProbabilityFactor: Int) {
            this.genesisProbabilityFactor = genesisProbabilityFactor
        }

        fun getTeleportProbability(): Float {
            return teleportProbability
        }

        fun setTeleportProbability(teleportProbability: Float) {
            this.teleportProbability = teleportProbability
        }
    }

}
