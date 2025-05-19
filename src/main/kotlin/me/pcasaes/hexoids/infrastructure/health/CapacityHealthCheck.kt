package me.pcasaes.hexoids.infrastructure.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@Liveness
@ApplicationScoped
class CapacityHealthCheck @Inject constructor(private val queueMetricList: List<QueueMetric>) : HealthCheck {
    override fun call(): HealthCheckResponse {
        return queueMetricList
            .stream()
            .sorted { a, b -> QueueMetric.Companion.compare(a, b) }
            .filter { obj -> obj.isOverCapacity() }
            .map { m -> "Service is over capacity. Queue '${m.getName()}' has load factory ${m.getLoadFactor()}" }
            .map { name -> HealthCheckResponse.down(name) }
            .findFirst()
            .orElse(HealthCheckResponse.up("Plenty of capacity"))
    }
}
