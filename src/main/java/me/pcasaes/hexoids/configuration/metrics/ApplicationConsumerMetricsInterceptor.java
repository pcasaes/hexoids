package me.pcasaes.hexoids.configuration.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import me.pcasaes.hexoids.core.domain.model.Clock;
import me.pcasaes.hexoids.core.domain.model.DomainEvent;
import pcasaes.hexoids.proto.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Interceptor
@ApplicationConsumerMetrics
public class ApplicationConsumerMetricsInterceptor {

    @Inject
    MeterRegistry meterRegistry;

    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    private final Clock clock = Clock.create();

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        if (context.getParameters().length > 0 && context.getParameters()[0] instanceof DomainEvent domainEvent) {

            Event event = domainEvent.getEvent();
            if (event != null) {
                long timestamp = -1L;
                String eventName = null;
                if (event.hasBoltExhausted()) {
                    timestamp = event.getBoltExhausted().getTimestamp();
                    eventName = "BoltExhausted";
                } else if (event.hasBoltFired()) {
                    timestamp = event.getBoltFired().getStartTimestamp();
                    eventName = "BoltFired";
                } else if(event.hasPlayerDestroyed()) {
                    timestamp = event.getPlayerDestroyed().getDestroyedTimestamp();
                    eventName = "PlayerDestroyed";
                } else if (event.hasPlayerFired()) {
                    timestamp = event.getPlayerFired().getStartTimestamp();
                    eventName = "PlayerFired";
                } else if (event.hasPlayerMoved()) {
                    timestamp = event.getPlayerMoved().getTimestamp();
                    eventName = "PlayerMoved";
                }

                if (eventName != null) {
                    register(timestamp,eventName);
                }
            }

        }

        return context.proceed();
    }

    private void register(long timestamp, String eventName) {
        long diff = clock.getTime() - timestamp;
        diff = 1_000_000L * diff + clock.getNanos();
        timers.computeIfAbsent(eventName, k -> Timer.builder("hexoids_event_consumer_received")
                .tag("event", k)
                .description("Measures time between an event production and consumption")
                .publishPercentiles(0.5,0.75,0.90,0.95,0.99)
                .register(meterRegistry)).record(diff, TimeUnit.NANOSECONDS);
    }
}
