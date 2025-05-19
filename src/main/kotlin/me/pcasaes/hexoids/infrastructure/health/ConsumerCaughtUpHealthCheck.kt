package me.pcasaes.hexoids.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class ConsumerCaughtUpHealthCheck implements HealthCheck {


    private final ApplicationConsumers.HaveStarted consumerHaveStarted;

    @Inject
    public ConsumerCaughtUpHealthCheck(ApplicationConsumers.HaveStarted consumerHaveStarted) {
        this.consumerHaveStarted = consumerHaveStarted;
    }

    @Override
    public HealthCheckResponse call() {
        return consumerHaveStarted.getAsBoolean() ? HealthCheckResponse.up("Kafka Consumers caught up") : HealthCheckResponse.down("Consumers aren't caught up yes");
    }
}
