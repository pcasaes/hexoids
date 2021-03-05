package me.pcasaes.hexoids.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.core.domain.metrics.GameMetric;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.model.Game;
import pcasaes.hexoids.proto.ClientPlatforms;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Arrays;

@ApplicationScoped
public class DomainMetrics {

    private final MeterRegistry metricRegistry;

    private final Tags commonTags = Tags.of(Tag.of("layer", "domain"));

    @Inject
    public DomainMetrics(MeterRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        GameMetrics
                .get()
                .getMetrics()
                .forEach(this::registry);

        Gauge
                .builder("total-number-of-players", this, DomainMetrics::getTotalNumberOfPlayers)
                .description("Current total number of players.")
                .tags(commonTags)
                .register(metricRegistry);

        Gauge
                .builder("number-of-connected-players", this, DomainMetrics::getNumberOfConnectedPlayers)
                .description("Current number of players connected to this node.")
                .tags(commonTags)
                .register(metricRegistry);

        Gauge
                .builder("number-of-active-bolts", this, DomainMetrics::getTotalNumberOfActiveBolts)
                .description("Current total number of active bolts.")
                .tags(commonTags)
                .register(metricRegistry);

    }

    private void registry(GameMetric gameMetric) {
        metricRegistry
                .gauge(
                        gameMetric.getName(),
                        commonTags.and(Tag.of("client_platform", "ALL")),
                        gameMetric,
                        GameMetric::getTotal);

        Arrays.stream(ClientPlatforms.values())
                .forEach(clientPlatform -> registry(gameMetric, clientPlatform));
    }

    private void registry(GameMetric gameMetric, ClientPlatforms clientPlatform) {
        metricRegistry
                .gauge(
                        gameMetric.getName(),
                        commonTags.and(Tag.of("client_platform", clientPlatform.name())),
                        gameMetric,
                        q -> q.getTotalByClientPlatform(clientPlatform));
    }

    public int getTotalNumberOfPlayers() {
        return Game.get().getPlayers().getTotalNumberOfPlayers();
    }

    public int getNumberOfConnectedPlayers() {
        return Game.get().getPlayers().getNumberOfConnectedPlayers();
    }

    public int getTotalNumberOfActiveBolts() {
        return Game.get().getBolts().getTotalNumberOfActiveBolts();
    }


}
