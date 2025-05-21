package me.pcasaes.hexoids.infrastructure.disruptor

import me.pcasaes.hexoids.infrastructure.clock.HRClock.nanoTime
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.Volatile

class QueueMetric private constructor(private val name: String) {
    private var runningTime: Long = 0
    private var lastReportTime: Long = 0
    private var lastStartClock: Long = 0

    private var nextAccumulate: Long = 0

    private var latencyTotal: Long = 0
    private var eventCount: Long = 0

    @Volatile
    private var latency = 0.0

    @Volatile
    private var avgProcessingTime = 0.0

    @Volatile
    private var loadFactor = 0.0

    @Volatile
    private var throughput = 0.0

    @Volatile
    private var lastAccumulationTime: Long = 0

    fun startClock() {
        this.lastStartClock = nanoTime()
    }

    fun stopClock(eventCreatedTime: Long) {
        val now = nanoTime()
        this.runningTime += now - this.lastStartClock
        this.latencyTotal += now - eventCreatedTime
        this.eventCount++

        if (now >= this.nextAccumulate) {
            val count = this.eventCount.toDouble()
            val elapsedTime = (now - this.lastReportTime).toDouble()
            this.throughput = count / (elapsedTime / NANO_PER_SECONDS)
            this.avgProcessingTime = runningTime / count
            this.loadFactor = runningTime / elapsedTime
            this.lastReportTime = now
            this.runningTime = 0L

            this.latency = this.latencyTotal / count
            this.latencyTotal = 0L
            this.eventCount = 0

            this.nextAccumulate = now + LOAD_FACTOR_CALC_WINDOW_NANO
            this.lastAccumulationTime = now
        }
    }

    fun isOverCapacity(): Boolean {
        val t = this.lastAccumulationTime
        if (t == 0L) {
            return false
        }

        val l = this.loadFactor
        return l >= 0.8
    }

    fun isStalled(): Boolean {
        val t = this.lastAccumulationTime
        if (t == 0L) {
            return false
        }
        return nanoTime() - t > STALLED_TIME_NANO
    }

    fun getLoadFactor(): Double {
        return loadFactor
    }

    fun getLatencyInNano(): Double {
        return latency
    }

    fun getAvgProcessingTimeInNano(): Double {
        return avgProcessingTime
    }

    fun getThroughput(): Double {
        return throughput
    }

    fun getLastCheckTimeAgoNano(): Long {
        return nanoTime() - this.lastAccumulationTime
    }

    fun getName(): String? {
        return name
    }

    companion object {
        const val NANO_PER_SECONDS: Long = 1000000000L
        const val LOAD_FACTOR_CALC_WINDOW_MILLIS: Long = 1000L
        const val LOAD_FACTOR_CALC_WINDOW_NANO: Long = LOAD_FACTOR_CALC_WINDOW_MILLIS * 1000000L
        const val STALLED_TIME_NANO: Long = LOAD_FACTOR_CALC_WINDOW_NANO * 5L

        private val LIST = CopyOnWriteArrayList<QueueMetric>()

        fun of(name: String): QueueMetric {
            val metric = QueueMetric(name)
            LIST.add(metric)
            return metric
        }

        fun getAllMetrics(): List<QueueMetric> {
            return LIST
        }

        fun compare(a: QueueMetric, b: QueueMetric): Int {
            return b.loadFactor.compareTo(a.loadFactor)
        }
    }
}
