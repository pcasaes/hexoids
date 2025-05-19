package me.pcasaes.hexoids.infrastructure.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import me.pcasaes.hexoids.core.domain.metrics.GameMetric
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics
import me.pcasaes.hexoids.core.domain.model.Game
import pcasaes.hexoids.proto.ClientPlatforms
import java.util.function.Consumer

@ApplicationScoped
class DomainMetrics @Inject constructor(private val metricRegistry: MeterRegistry) {
    private val commonTags: Tags = Tags.of(Tag.of("layer", "domain"))

    fun startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) event: StartupEvent) {
        GameMetrics
            .get()
            .getMetrics()
            .forEach(Consumer { gameMetric -> this.registry(gameMetric) })

        Gauge
            .builder("total-number-of-players", this) { it.getTotalNumberOfPlayers().toDouble() }
            .description("Current total number of players.")
            .tags(commonTags)
            .register(metricRegistry)

        Gauge
            .builder("number-of-connected-players", this) { it.getNumberOfConnectedPlayers().toDouble() }
            .description("Current number of players connected to this node.")
            .tags(commonTags)
            .register(metricRegistry)

        Gauge
            .builder("number-of-active-bolts", this) { it.getTotalNumberOfActiveBolts().toDouble() }
            .description("Current total number of active bolts.")
            .tags(commonTags)
            .register(metricRegistry)
    }

    private fun registry(gameMetric: GameMetric) {
        metricRegistry
            .gauge(
                gameMetric.getName(),
                commonTags.and(Tag.of("client_platform", "ALL")),
                gameMetric
            )
            { obj -> obj.getTotal().toDouble() }

        ClientPlatforms.entries
            .forEach { clientPlatform -> registry(gameMetric, clientPlatform) }
    }

    private fun registry(gameMetric: GameMetric, clientPlatform: ClientPlatforms) {
        metricRegistry
            .gauge(
                gameMetric.getName(),
                commonTags.and(Tag.of("client_platform", clientPlatform.name)),
                gameMetric
            )
            { q -> q.getTotalByClientPlatform(clientPlatform).toDouble() }
    }

    fun getTotalNumberOfPlayers(): Int {
        return Game.get().getPlayers().getTotalNumberOfPlayers()
    }

    fun getNumberOfConnectedPlayers(): Int {
        return Game.get().getPlayers().getNumberOfConnectedPlayers()
    }

    fun getTotalNumberOfActiveBolts(): Int {
        return Game.get().getBolts().getTotalNumberOfActiveBolts()
    }
}
