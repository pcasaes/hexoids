package me.pcasaes.hexoids.infrastructure.metrics;

import io.quarkus.runtime.StartupEvent;
import me.pcasaes.hexoids.core.domain.model.Game;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.interceptor.Interceptor;

@ApplicationScoped
public class DomainMetrics {

    public void startup(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) StartupEvent event) {
        // do nothing
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
