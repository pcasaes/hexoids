package me.pcasaes.hexoids.infrastructure.metrics;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.core.domain.metrics.GameMetric;
import me.pcasaes.hexoids.core.domain.metrics.GameMetrics;
import me.pcasaes.hexoids.core.domain.model.Game;
import org.eclipse.microprofile.metrics.MetadataBuilder;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import pcasaes.hexoids.proto.ClientPlatforms;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import java.util.Arrays;

@ApplicationScoped
public class DomainMetrics {

    private final MetricRegistry metricRegistry;

    @Inject
    public DomainMetrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public void startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        GameMetrics
                .get()
                .getMetrics()
                .forEach(this::registry);
    }

    private void registry(GameMetric gameMetric) {
        org.eclipse.microprofile.metrics.Gauge<Long> gauge = gameMetric::getTotal;

        metricRegistry
                .register(
                        new MetadataBuilder()
                                .withName(gameMetric.getName())
                                .withDisplayName(gameMetric.getName())
                                .withType(MetricType.GAUGE)
                                .withUnit(MetricUnits.NONE)
                                .build(),
                        gauge,
                        new Tag("layer", "domain")
                );

        Arrays.stream(ClientPlatforms.values())
                .forEach(clientPlatform -> registry(gameMetric, clientPlatform));
    }

    private void registry(GameMetric gameMetric, ClientPlatforms clientPlatform) {
        org.eclipse.microprofile.metrics.Gauge<Long> gauge = () -> gameMetric.getTotalByClientPlatform(clientPlatform);

        metricRegistry
                .register(
                        new MetadataBuilder()
                                .withName(gameMetric.getName() + "." + clientPlatform.name())
                                .withDisplayName(gameMetric.getName() + "." + clientPlatform.name())
                                .withType(MetricType.GAUGE)
                                .withUnit(MetricUnits.NONE)
                                .build(),
                        gauge,
                        new Tag("layer", "domain")
                );
    }

    @Gauge(
            name = "total-number-of-players",
            unit = MetricUnits.NONE,
            description = "Current total number of players.",
            absolute = true,
            tags = "layer=domain"
    )
    public int getTotalNumberOfPlayers() {
        return Game.get().getPlayers().getTotalNumberOfPlayers();
    }

    @Gauge(
            name = "number-of-connected-players",
            unit = MetricUnits.NONE,
            description = "Current number of players connected to this node.",
            absolute = true,
            tags = "layer=domain"
    )
    public int getNumberOfConnectedPlayers() {
        return Game.get().getPlayers().getNumberOfConnectedPlayers();
    }

    @Gauge(
            name = "number-of-active-bolts",
            unit = MetricUnits.NONE,
            description = "Current total number of active bolts.",
            absolute = true,
            tags = "layer=domain"
    )
    public int getTotalNumberOfActiveBolts() {
        return Game.get().getBolts().getTotalNumberOfActiveBolts();
    }


}
