package me.pcasaes.hexoids.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import java.util.List;

@Liveness
@ApplicationScoped
public class CapacityHealthCheck implements HealthCheck {

    private final List<QueueMetric> queueMetricList;

    @Inject
    public CapacityHealthCheck(List<QueueMetric> queueMetricList) {
        this.queueMetricList = queueMetricList;
    }

    @Override
    public HealthCheckResponse call() {
        return queueMetricList
                .stream()
                .sorted(QueueMetric::compare)
                .filter(QueueMetric::isOverCapacity)
                .map(m -> "Service is over capacity. Queue '" + m.getName() + "' has load factory " + m.getLoadFactor())
                .map(HealthCheckResponse::down)
                .findFirst()
                .orElse(HealthCheckResponse.up("Plenty of capacity"));
    }
}
