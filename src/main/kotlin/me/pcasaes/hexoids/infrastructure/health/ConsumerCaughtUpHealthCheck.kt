package me.pcasaes.hexoids.infrastructure.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers.HaveStarted
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness

@Readiness
@ApplicationScoped
class ConsumerCaughtUpHealthCheck @Inject constructor(
    private val consumerHaveStarted: HaveStarted
) : HealthCheck {
    override fun call(): HealthCheckResponse {
        return if (consumerHaveStarted.asBoolean) {
            HealthCheckResponse.up("Kafka Consumers caught up")
        } else HealthCheckResponse.down(
            "Consumers aren't caught up yes"
        )
    }
}
