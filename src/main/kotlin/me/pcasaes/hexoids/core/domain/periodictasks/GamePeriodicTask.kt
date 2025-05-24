package me.pcasaes.hexoids.core.domain.periodictasks

import java.util.concurrent.TimeUnit

interface GamePeriodicTask : Runnable {

    fun enabled(): Boolean {
        return true
    }

    fun getPeriod(): Long

    fun getDelay(): Long {
        return 1000
    }

    fun getTimeUnit(): TimeUnit {
        return TimeUnit.MILLISECONDS
    }
}
