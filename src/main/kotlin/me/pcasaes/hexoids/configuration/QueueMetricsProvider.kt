package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric

@ApplicationScoped
class QueueMetricsProvider {
    @Produces
    @Singleton
    fun getMetrics(): List<QueueMetric> {
        return QueueMetric.getAllMetrics()
    }
}
