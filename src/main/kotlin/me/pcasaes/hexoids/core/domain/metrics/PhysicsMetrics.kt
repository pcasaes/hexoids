package me.pcasaes.hexoids.core.domain.metrics

import java.util.function.BiConsumer
import java.util.function.LongConsumer

class PhysicsMetrics private constructor() {
    private val measurements = HashMap<String, MutableList<Long>>()

    fun intercept(execution: LongConsumer, name: String): LongConsumer {
        val measures = ArrayList<Long>()
        measurements.put(name, measures)
        return LongConsumer { t: Long ->
            val start = System.nanoTime()
            try {
                execution.accept(t)
            } finally {
                measures.add(System.nanoTime() - start)
            }
        }
    }

    fun flush(consumeMeasurements: BiConsumer<String, List<Long>>) {
        measurements
            .forEach { (n: String, list: MutableList<Long>) ->
                consumeMeasurements.accept(n, ArrayList<Long>(list))
                list.clear()
            }
    }

    companion object {
        private val instance = PhysicsMetrics()

        @JvmStatic
        fun get(): PhysicsMetrics {
            return instance
        }
    }
}
