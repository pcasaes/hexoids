package me.pcasaes.hexoids.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;

import java.util.List;

@ApplicationScoped
public class QueueProcessingMetrics {


    private final MeterRegistry metricRegistry;
    private final List<QueueMetric> queueMetricList;

    private final Tags tag = Tags.of("layer", "infrastructure");

    @Inject
    public QueueProcessingMetrics(MeterRegistry metricRegistry, List<QueueMetric> queueMetricList) {
        this.metricRegistry = metricRegistry;
        this.queueMetricList = queueMetricList;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        this.queueMetricList
                .forEach(this::setup);


        Gauge.builder("load-factor", this, QueueProcessingMetrics::getGreaterLoadFactor)
                .description("Percentage of time spent processing events.")
                .baseUnit(BaseUnits.PERCENT)
                .tags(tag)
                .register(metricRegistry);
    }

    private void setup(QueueMetric queueMetric) {
        Gauge.builder(queueMetric.getName() + "-load-factor", queueMetric, QueueMetric::getLoadFactor)
                .description("Percentage of time spent processing events.")
                .baseUnit(BaseUnits.PERCENT)
                .tags(tag)
                .register(metricRegistry);

        Gauge.builder(queueMetric.getName() + "-latency", queueMetric, q -> q.getLatencyInNano() / 1_000_000_000.0)
                .description("Avg Latency to process events. Time since enqueued plus processing time.")
                .baseUnit("seconds")
                .tags(tag)
                .register(metricRegistry);

        Gauge.builder(queueMetric.getName() + "-processing-time", queueMetric, q -> q.getAvgProcessingTimeInNano() / 1_000_000_000.0)
                .description("Avg Latency to process events. Time since enqueued plus processing time.")
                .baseUnit("seconds")
                .tags(tag)
                .register(metricRegistry);

        Gauge.builder(queueMetric.getName() + "-throughput", queueMetric, QueueMetric::getThroughput)
                .description("Throughput to process events.")
                .baseUnit("per_second")
                .tags(tag)
                .register(metricRegistry);
    }

    public double getGreaterLoadFactor() {
        return queueMetricList
                .stream()
                .sorted(QueueMetric::compare)
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }
}
