package me.pcasaes.hexoids.infrastructure.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness

@Liveness
@ApplicationScoped
class StalledHealthCheck @Inject constructor(private val queueMetricList: List<QueueMetric>) : HealthCheck {
    override fun call(): HealthCheckResponse {
        return queueMetricList
            .stream()
            .sorted { a, b -> QueueMetric.Companion.compare(a, b) }
            .filter { obj -> obj.isStalled() }
            .map { m -> "Stalled. Queue '${m.getName()}' was checked  ${m.getLastCheckTimeAgoNano()} nanosecond ago." }
            .map { name -> HealthCheckResponse.down(name) }
            .findFirst()
            .orElse(HealthCheckResponse.up("Queue is up to date"))
    }
}
