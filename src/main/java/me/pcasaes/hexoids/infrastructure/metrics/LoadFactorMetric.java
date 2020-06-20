package me.pcasaes.hexoids.infrastructure.metrics;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorIn;
import me.pcasaes.hexoids.infrastructure.disruptor.DisruptorOut;
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.List;

@ApplicationScoped
public class LoadFactorMetric {


    private final List<QueueMetric> queueMetricList;

    @Inject
    public LoadFactorMetric(List<QueueMetric> queueMetricList) {
        this.queueMetricList = queueMetricList;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.APPLICATION + 700) StartupEvent event) {
        // do nothing
    }

    @Gauge(name = DisruptorOut.METRIC_DOMAIN_EVENT_OUT + "-load-factor", unit = MetricUnits.PERCENT, description = "Percentage of a fixed time processing events.")
    public double getDomainEventOutLF() {
        return queueMetricList
                .stream()
                .filter(m -> DisruptorOut.METRIC_DOMAIN_EVENT_OUT.equalsIgnoreCase(m.getName()))
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }

    @Gauge(name = DisruptorOut.METRIC_CLIENT_EVENT_OUT + "-load-factor", unit = MetricUnits.PERCENT, description = "Percentage of a fixed time processing events.")
    public double getClientEventOutLF() {
        return queueMetricList
                .stream()
                .filter(m -> DisruptorOut.METRIC_CLIENT_EVENT_OUT.equalsIgnoreCase(m.getName()))
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }

    @Gauge(name = DisruptorIn.METRIC_GAME_LOOP_IN + "-load-factor", unit = MetricUnits.PERCENT, description = "Percentage of a fixed time processing events.")
    public double getGameLoopLF() {
        return queueMetricList
                .stream()
                .filter(m -> DisruptorIn.METRIC_GAME_LOOP_IN.equalsIgnoreCase(m.getName()))
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }

    @Gauge(name = "load-factor", unit = MetricUnits.PERCENT, description = "Percentage of a fixed time processing events.")
    public double getLF() {
        return queueMetricList
                .stream()
                .sorted(QueueMetric::compare)
                .map(QueueMetric::getLoadFactor)
                .findFirst()
                .orElse(0.);
    }
}
