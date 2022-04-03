package me.pcasaes.hexoids.configuration.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.runtime.Startup;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;
import me.pcasaes.hexoids.core.domain.metrics.PhysicsMetrics;
import me.pcasaes.hexoids.core.domain.model.Game;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Startup
@Singleton
public class PhysicsMetricsConfiguration {

    private final Vertx vertx;

    private final Fetch fetch;

    @Inject
    public PhysicsMetricsConfiguration(
            GameQueue gameQueue,
            Vertx vertx,
            MeterRegistry meterRegistry) {
        this.vertx = vertx;
        this.fetch = new Fetch(gameQueue, Game.get().getPhysicsMetrics(), meterRegistry);
    }

    @PostConstruct
    void start() {
        this.vertx.deployVerticle(fetch);
    }

    @PreDestroy
    void stop() {
        this.vertx.undeploy(fetch.deploymentID());
    }


    private static class Fetch extends AbstractVerticle {

        private final GameQueue gameQueue;

        private final PhysicsMetrics physicsMetrics;

        private final MeterRegistry meterRegistry;

        private final Map<String, Timer> timers = new HashMap<>();

        private long timerId = 0L;

        private Fetch(GameQueue gameQueue, PhysicsMetrics physicsMetrics, MeterRegistry meterRegistry) {
            this.gameQueue = gameQueue;
            this.physicsMetrics = physicsMetrics;
            this.meterRegistry = meterRegistry;
        }

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            context.runOnContext(h -> timerId = getVertx().setPeriodic(1000L, h2 -> fetch()));
            super.start(startPromise);
        }

        @Override
        public void stop(Promise<Void> stopPromise) throws Exception {
            context
                    .runOnContext(h -> {
                        if (timerId != 0L) {
                            getVertx().cancelTimer(timerId);
                        }
                    });
            super.stop(stopPromise);
        }

        private void fetch() {
            this.gameQueue
                    .enqueue(() -> this.physicsMetrics.flush(this::measure));
        }

        private void measure(String name, List<Long> measurements) {
            context
                    .runOnContext(h -> {
                        Timer timer = timers.computeIfAbsent(name, k -> Timer.builder("physics_fixed_update")
                                .tag("name", k)
                                .publishPercentiles(0.5, 0.75, 0.90, 0.95)
                                .register(meterRegistry)
                        );
                        measurements
                                .forEach(t -> timer.record(t, TimeUnit.NANOSECONDS));
                    });
        }
    }
}
