package me.pcasaes.hexoids.infrastructure.clock

object HRClock {
    fun nanoTime(): Long {
        return System.nanoTime()
    }
}
