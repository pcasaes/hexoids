package me.pcasaes.hexoids.core.domain.service

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.model.Game
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Used to add events into the game loop.
 */
class GameLoopService private constructor() {
    private val fixedUpdateRunnable: Optional<Runnable> = Optional.of(Runnable { this.fixedUpdate() })


    private var nextFixedUpdateTime: Long

    private fun gethGameTime(): Long {
        return Game.get().getClock().getTime()
    }

    init {
        this.nextFixedUpdateTime = this.gethGameTime()
    }

    private fun fixedUpdate() {
        val timestamp = this.gethGameTime()
        if (timestamp > this.nextFixedUpdateTime) {
            Game.get()
                .fixedUpdate(timestamp)

            this.nextFixedUpdateTime = timestamp + Config.get().getUpdateFrequencyInMillis()
        }
    }

    fun accept(runnable: Runnable) {
        try {
            runnable.run()
        } catch (ex: RuntimeException) {
            LOGGER.log(Level.SEVERE, ex.message, ex)
        }
        fixedUpdate()
    }

    fun getName(): String {
        return NAME
    }

    fun getFixedUpdateRunnable(): Optional<Runnable> {
        val timestamp = this.gethGameTime()
        if (timestamp > nextFixedUpdateTime) {
            return fixedUpdateRunnable
        }
        return Optional.empty()
    }

    companion object {
        private val INSTANCE = GameLoopService()

        fun getInstance(): GameLoopService {
            return INSTANCE
        }

        private const val NAME = "game-loop"
        private val LOGGER: Logger = Logger.getLogger(GameLoopService::class.java.getName())
    }
}
