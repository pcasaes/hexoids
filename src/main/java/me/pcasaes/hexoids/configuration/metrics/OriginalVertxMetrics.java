package me.pcasaes.hexoids.configuration.metrics;

import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.VertxMetricsFactory;
import io.vertx.core.spi.VertxServiceProvider;
import io.vertx.micrometer.impl.VertxMetricsFactoryImpl;

/**
 * Enable original vertx metrics
 * Requires disabling quarkus-vertx metrics
 * `quarkus,micrometer.binder.vertx.enabled=true`
 */
public class OriginalVertxMetrics implements VertxServiceProvider {

    private final VertxMetricsFactory factory = new VertxMetricsFactoryImpl();

    @Override
    public void init(VertxBuilder builder) {
        builder.options()
                .setMetricsOptions(new MetricsOptions(new JsonObject("{\"enabled\":true}")));

        factory.init(builder);
    }
}
