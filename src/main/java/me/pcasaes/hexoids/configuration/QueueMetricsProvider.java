package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.List;

@ApplicationScoped
public class QueueMetricsProvider {

    @Produces
    @Singleton
    public List<QueueMetric> getMetrics() {
        return QueueMetric.getAllMetrics();
    }

}
