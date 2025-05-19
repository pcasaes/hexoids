package me.pcasaes.hexoids.infrastructure.clock

object HRClock {
    @JvmStatic
    fun nanoTime(): Long {
        return System.nanoTime()
    }
}
