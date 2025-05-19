package me.pcasaes.hexoids.core.domain.metrics

import me.pcasaes.hexoids.core.domain.metrics.GameMetric.Companion.of

class GameMetrics private constructor() {

    companion object {
        private val INSTANCE = GameMetrics()

        @JvmStatic
        fun get(): GameMetrics {
            return INSTANCE
        }
    }

    private val playerDestroyed: GameMetric
    private val playerSpawned: GameMetric
    private val playerJoined: GameMetric
    private val playerLeft: GameMetric
    private val playerStalled: GameMetric
    private val boltFired: GameMetric
    private val boltExhausted: GameMetric
    private val movedByShockwave: GameMetric
    private val massCollapsedIntoBlackhole: GameMetric
    private val blackholeEvaporated: GameMetric
    private val movedByBlackhole: GameMetric
    private val destroyedByBlackhole: GameMetric

    private val metrics: List<GameMetric>

    init {
        this.playerDestroyed = of("player-destroyed-total")
        this.playerSpawned = of("player-spawned-total")
        this.playerJoined = of("player-joined-total")
        this.playerLeft = of("player-left-total")
        this.playerStalled = of("player-stalled-total")
        this.boltFired = of("bolt-fired-total")
        this.boltExhausted = of("bolt-exhausted-total")
        this.movedByShockwave = of("moved-by-shockwave-total")
        this.massCollapsedIntoBlackhole = of("mass-collapsed-into-blackhole-total")
        this.blackholeEvaporated = of("blackhole-evaporated-total")
        this.movedByBlackhole = of("moved-by-blackhole-total")
        this.destroyedByBlackhole = of("destroyed-by-blackhole-total")

        val list: MutableList<GameMetric> = ArrayList<GameMetric>(12)
        list.add(playerDestroyed)
        list.add(playerSpawned)
        list.add(playerJoined)
        list.add(playerLeft)
        list.add(playerStalled)
        list.add(boltFired)
        list.add(boltExhausted)
        list.add(movedByShockwave)
        list.add(massCollapsedIntoBlackhole)
        list.add(blackholeEvaporated)
        list.add(movedByBlackhole)
        list.add(destroyedByBlackhole)

        this.metrics = list
    }

    fun getMetrics(): List<GameMetric> {
        return metrics
    }

    fun getPlayerDestroyed(): GameMetric {
        return playerDestroyed
    }

    fun getBoltFired(): GameMetric {
        return boltFired
    }

    fun getBoltExhausted(): GameMetric {
        return boltExhausted
    }

    fun getPlayerSpawned(): GameMetric {
        return playerSpawned
    }

    fun getPlayerJoined(): GameMetric {
        return playerJoined
    }

    fun getPlayerLeft(): GameMetric {
        return playerLeft
    }

    fun getPlayerStalled(): GameMetric {
        return playerStalled
    }

    fun getMovedByShockwave(): GameMetric {
        return movedByShockwave
    }

    fun getMassCollapsedIntoBlackhole(): GameMetric {
        return massCollapsedIntoBlackhole
    }

    fun getBlackholeEvaporated(): GameMetric {
        return blackholeEvaporated
    }

    fun getMovedByBlackhole(): GameMetric {
        return movedByBlackhole
    }

    fun getDestroyedByBlackhole(): GameMetric {
        return destroyedByBlackhole
    }
}
