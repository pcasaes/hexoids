package me.pcasaes.hexoids.infrastructure.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.BaseUnits
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric
import java.util.function.Consumer
import java.util.function.ToDoubleFunction

@ApplicationScoped
class QueueProcessingMetrics @Inject constructor(
    private val metricRegistry: MeterRegistry,
    private val queueMetricList: List<QueueMetric>
) {
    private val tag: Tags = Tags.of("layer", "infrastructure")

    fun startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) event: StartupEvent) {
        this.queueMetricList
            .forEach { queueMetric -> this.setup(queueMetric) }


        Gauge.builder(
            "load-factor",
            this
        ) { obj -> obj.getGreaterLoadFactor() }
            .description("Percentage of time spent processing events.")
            .baseUnit(BaseUnits.PERCENT)
            .tags(tag)
            .register(metricRegistry)
    }

    private fun setup(queueMetric: QueueMetric) {
        Gauge.builder(
            queueMetric.getName() + "-load-factor",
            queueMetric
        )
        { obj -> obj.getLoadFactor() }
            .description("Percentage of time spent processing events.")
            .baseUnit(BaseUnits.PERCENT)
            .tags(tag)
            .register(metricRegistry)

        Gauge.builder(
            queueMetric.getName() + "-latency",
            queueMetric
        )
        { q -> q.getLatencyInNano() / 1000000000.0 }
            .description("Avg Latency to process events. Time since enqueued plus processing time.")
            .baseUnit("seconds")
            .tags(tag)
            .register(metricRegistry)

        Gauge.builder(
            queueMetric.getName() + "-processing-time",
            queueMetric
        )
        { q -> q.getAvgProcessingTimeInNano() / 1000000000.0 }
            .description("Avg Latency to process events. Time since enqueued plus processing time.")
            .baseUnit("seconds")
            .tags(tag)
            .register(metricRegistry)

        Gauge.builder(
            queueMetric.getName() + "-throughput",
            queueMetric
        )
        { obj -> obj.getThroughput() }
            .description("Throughput to process events.")
            .baseUnit("per_second")
            .tags(tag)
            .register(metricRegistry)
    }

    fun getGreaterLoadFactor(): Double {
        return queueMetricList
            .stream()
            .sorted { a, b -> QueueMetric.Companion.compare(a, b) }
            .map { obj -> obj.getLoadFactor() }
            .findFirst()
            .orElse(0.0)
    }
}
