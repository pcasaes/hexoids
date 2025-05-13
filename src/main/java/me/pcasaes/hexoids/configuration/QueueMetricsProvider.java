package me.pcasaes.hexoids.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;

import java.util.List;

@ApplicationScoped
public class QueueMetricsProvider {

    @Produces
    @Singleton
    public List<QueueMetric> getMetrics() {
        return QueueMetric.getAllMetrics();
    }

}
