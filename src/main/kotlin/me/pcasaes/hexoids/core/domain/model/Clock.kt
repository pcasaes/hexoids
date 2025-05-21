package me.pcasaes.hexoids.core.domain.model

/**
 * Used to get the current time in millis;
 */
interface Clock {
    fun getTime(): Long

    fun getNanos(): Long

    /**
     * This implementation uses [System.nanoTime] to provide a
     * monotonic clock.
     */
    class Implementation private constructor() : Clock {
        private val adjustment: Long

        init {
            val cpuTimeMillis = System.nanoTime() / 1000000L
            val systemTimeMillis = System.currentTimeMillis()
            this.adjustment = systemTimeMillis - cpuTimeMillis
        }

        override fun getTime(): Long {
            val currentCpuTimeMillis = System.nanoTime() / 1000000L
            return currentCpuTimeMillis + adjustment
        }

        override fun getNanos(): Long {
            return System.nanoTime() % 1000000L
        }

        companion object {
            var holder: Implementation = Implementation()
        }
    }

    companion object {

        fun create(): Clock {
            return Implementation.Companion.holder
        }
    }
}
