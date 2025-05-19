package me.pcasaes.hexoids.configuration.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.quarkus.runtime.Startup
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import jakarta.inject.Singleton
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.metrics.PhysicsMetrics
import me.pcasaes.hexoids.core.domain.model.Game
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@Startup
@Singleton
class PhysicsMetricsConfiguration @Inject constructor(
    gameQueue: GameQueue,
    private val vertx: Vertx,
    meterRegistry: MeterRegistry
) {
    private val fetch: Fetch = Fetch(gameQueue, Game.get().getPhysicsMetrics(), meterRegistry)

    @PostConstruct
    fun start() {
        this.vertx.deployVerticle(fetch)
    }

    @PreDestroy
    fun stop() {
        this.vertx.undeploy(fetch.deploymentID())
    }


    private class Fetch(
        private val gameQueue: GameQueue,
        private val physicsMetrics: PhysicsMetrics,
        private val meterRegistry: MeterRegistry
    ) : AbstractVerticle() {
        private val timers = HashMap<String, Timer>()

        private var timerId = 0L

        @Throws(Exception::class)
        override fun start(startPromise: Promise<Void>) {
            context.runOnContext { h ->
                timerId = getVertx().setPeriodic(1000L) { _ -> fetch() }
            }
            super.start(startPromise)
        }

        @Throws(Exception::class)
        override fun stop(stopPromise: Promise<Void>) {
            context
                .runOnContext { _ ->
                    if (timerId != 0L) {
                        getVertx().cancelTimer(timerId)
                    }
                }
            super.stop(stopPromise)
        }

        fun fetch() {
            this.gameQueue
                .enqueue {
                    this.physicsMetrics.flush { name: String, measurements: List<Long> ->
                        this.measure(
                            name,
                            measurements
                        )
                    }
                }
        }

        fun measure(name: String, measurements: List<Long>) {
            context
                .runOnContext { _ ->
                    val timer = timers.computeIfAbsent(name) { k ->
                        Timer.builder("physics_fixed_update")
                            .tag("name", k)
                            .publishPercentiles(0.5, 0.75, 0.90, 0.95)
                            .register(meterRegistry)
                    }
                    measurements
                        .forEach(Consumer { t -> timer.record(t, TimeUnit.NANOSECONDS) })
                }
        }
    }
}
