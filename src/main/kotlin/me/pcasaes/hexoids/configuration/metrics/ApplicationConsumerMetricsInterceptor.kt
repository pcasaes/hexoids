package me.pcasaes.hexoids.configuration.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.inject.Inject
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext
import me.pcasaes.hexoids.core.domain.model.Clock
import me.pcasaes.hexoids.core.domain.model.DomainEvent
import me.pcasaes.hexoids.core.domain.utils.OptimisticConcurrentHashMap
import pcasaes.hexoids.proto.Event
import java.util.concurrent.TimeUnit

@Interceptor
@ApplicationConsumerMetrics
class ApplicationConsumerMetricsInterceptor @Inject constructor(
    private val meterRegistry: MeterRegistry,
) {

    private val timers = OptimisticConcurrentHashMap<String, Timer>()

    private val clock: Clock = Clock.create()

    @AroundInvoke
    @Throws(Exception::class)
    fun invoke(context: InvocationContext): Any? {
        if (context.parameters.size > 0 && context.parameters[0] is DomainEvent) {
            val domainEvent = context.parameters[0] as DomainEvent
            val event: Event? = domainEvent.event
            if (event != null) {
                var timestamp = -1L
                var eventName: String? = null
                when {
                    event.hasBoltExhausted() -> {
                        timestamp = event.getBoltExhausted().timestamp
                        eventName = "BoltExhausted"
                    }

                    event.hasBoltFired() -> {
                        timestamp = event.getBoltFired().startTimestamp
                        eventName = "BoltFired"
                    }

                    event.hasPlayerDestroyed() -> {
                        timestamp = event.getPlayerDestroyed().destroyedTimestamp
                        eventName = "PlayerDestroyed"
                    }

                    event.hasPlayerFired() -> {
                        timestamp = event.getPlayerFired().startTimestamp
                        eventName = "PlayerFired"
                    }

                    event.hasPlayerMoved() -> {
                        timestamp = event.getPlayerMoved().timestamp
                        eventName = "PlayerMoved"
                    }
                }

                if (eventName != null) {
                    register(timestamp, eventName)
                }
            }
        }

        return context.proceed()
    }

    private fun register(timestamp: Long, eventName: String) {
        var diff = clock.getTime() - timestamp
        diff = 1000000L * diff + clock.getNanos()
        timers.computeIfAbsent(eventName) { k ->
            Timer.builder("hexoids_event_consumer_received")
                .tag("event", k)
                .description("Measures time between an event production and consumption")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95, 0.99)
                .register(meterRegistry)
        }.record(diff, TimeUnit.NANOSECONDS)
    }
}
