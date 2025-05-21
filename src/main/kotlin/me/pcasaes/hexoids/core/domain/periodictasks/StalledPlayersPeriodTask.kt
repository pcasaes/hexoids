package me.pcasaes.hexoids.core.domain.periodictasks

import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.model.Game.Companion.get
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class StalledPlayersPeriodTask private constructor(private val gameQueue: GameQueue) : GamePeriodicTask {
    override fun getPeriod(): Long {
        return 60
    }

    override fun run() {
        gameQueue.enqueue(Runnable {
            get().getPlayers()
                .forEach(Consumer { obj -> obj.expungeIfStalled() })
        })
    }

    override fun getDelay(): Long {
        return 0
    }

    override fun getTimeUnit(): TimeUnit {
        return TimeUnit.SECONDS
    }

    companion object {
        fun create(gameQueue: GameQueue): StalledPlayersPeriodTask {
            return StalledPlayersPeriodTask(gameQueue)
        }
    }
}
