package me.pcasaes.hexoids.core.domain.periodictasks

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.service.GameLoopService

class GameLoopPeriodicTask private constructor(
    private val gameQueue: GameQueue,
    private val gameLoopService: GameLoopService,
    private val period: Long
) : GamePeriodicTask {

    private fun gameLoopPeriodicTask() {
        gameLoopService
            .getFixedUpdateRunnable()
            .ifPresent { event -> this.publish(event) }
    }

    private fun publish(event: Runnable) {
        this.gameQueue.enqueue(event)
    }

    override fun getPeriod(): Long {
        return period
    }

    override fun run() {
        this.gameLoopPeriodicTask()
    }

    companion object {
        fun create(
            gameQueue: GameQueue,
            gameLoopService: GameLoopService,
            period: Long
        ): GameLoopPeriodicTask {
            return GameLoopPeriodicTask(gameQueue, gameLoopService, period)
        }
    }
}
