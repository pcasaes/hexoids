package me.pcasaes.hexoids.core.domain.service

import me.pcasaes.hexoids.core.domain.config.Config
import me.pcasaes.hexoids.core.domain.model.Game
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Used to add events into the game loop.
 */
object GameLoopService {

    private const val name = "game-loop"
    private val log: Logger = Logger.getLogger(GameLoopService::class.java.getName())


    private val fixedUpdateRunnable: Runnable = Runnable { this.fixedUpdate() }

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

            this.nextFixedUpdateTime = timestamp + Config.getUpdateFrequencyInMillis()
        }
    }

    fun accept(runnable: Runnable) {
        try {
            runnable.run()
        } catch (ex: RuntimeException) {
            log.log(Level.SEVERE, ex.message, ex)
        }
        fixedUpdate()
    }

    fun getName(): String {
        return name
    }

    fun getFixedUpdateRunnable(): Runnable? {
        val timestamp = this.gethGameTime()
        return if (timestamp > nextFixedUpdateTime) {
            fixedUpdateRunnable
        } else {
            null
        }
    }


}
