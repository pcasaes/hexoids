package me.pcasaes.hexoids.infrastructure.metrics;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.List;

@ApplicationScoped
public class QueueProcessingMetrics {


    private final MetricRegistry metricRegistry;
    private final List<QueueMetric> queueMetricList;

    @Inject
    public QueueProcessingMetrics(MetricRegistry metricRegistry, List<QueueMetric> queueMetricList) {
        this.metricRegistry = metricRegistry;
        this.queueMetricList = queueMetricList;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        this.queueMetricList
                .forEach(this::setup);
    }

    private void setup(QueueMetric queueMetric) {
        Tag tag = new Tag("layer", "infrastructure");

        org.eclipse.microprofile.metrics.Gauge<Double> lf = queueMetric::getLoadFactor;
        metricRegistry.register(new MetadataBuilder()
                        .withName(queueMetric.getName() + "-load-factor")
                        .withDescription("Percentage of time spent processing events.")
                        .withUnit(MetricUnits.PERCENT)
                        .withType(MetricType.GAUGE)
                        .build(),
                lf,
                tag
        );

        org.eclipse.microprofile.metrics.Gauge<Double> latency = queueMetric::getLatencyInNano;
        metricRegistry.register(new MetadataBuilder()
                        .withName(queueMetric.getName() + "-latency")
                        .withDescription("Avg Latency to process events. Time since enqueued plus processing time.")
                        .withUnit(MetricUnits.NANOSECONDS)
                        .withType(MetricType.GAUGE)
                        .build(),
                latency,
                tag
        );

        org.eclipse.microprofile.metrics.Gauge<Double> processingTime = queueMetric::getAvgProcessingTimeInNano;
        metricRegistry.register(new MetadataBuilder()
                        .withName(queueMetric.getName() + "-processing-time")
                        .withDescription("Avg Latency to process events. Time since enqueued plus processing time.")
                        .withUnit(MetricUnits.NANOSECONDS)
                        .withType(MetricType.GAUGE)
                        .build(),
                processingTime,
                tag
        );

        org.eclipse.microprofile.metrics.Gauge<Double> throughput = queueMetric::getThroughput;
        metricRegistry.register(new MetadataBuilder()
                        .withName(queueMetric.getName() + "-throughput")
                        .withDescription("Throughput to process events.")
                        .withUnit(MetricUnits.PER_SECOND)
                        .withType(MetricType.GAUGE)
                        .build(),
                throughput,
                tag
        );
    }

    @Gauge(
            name = "load-factor",
            unit = MetricUnits.PERCENT,
            description = "Percentage of time spent processing events.",
            absolute = true,
            tags = "layer=infrastructure"
    )
    public double getGreaterLoadFactor() {
        return queueMetricList
                .stream()
                .sorted(QueueMetric::compare)
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }
}
