package me.pcasaes.hexoids.infrastructure.health;

import me.pcasaes.hexoids.infrastructure.disruptor.QueueMetric;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@Liveness
@ApplicationScoped
public class StalledHealthCheck implements HealthCheck {

    private final List<QueueMetric> queueMetricList;

    @Inject
    public StalledHealthCheck(List<QueueMetric> queueMetricList) {
        this.queueMetricList = queueMetricList;
    }

    @Override
    public HealthCheckResponse call() {
        return queueMetricList
                .stream()
                .sorted(QueueMetric::compare)
                .filter(QueueMetric::isStalled)
                .map(m -> "Stalled. Queue '" + m.getName() + "' was checked  " + m.getLastCheckTimeAgo() + "ms ago.")
                .map(HealthCheckResponse::down)
                .findFirst()
                .orElse(HealthCheckResponse.up("Queue is up to date"));
    }
}
