package me.pcasaes.hexoids.core.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.LongPredicate;

/**
 * Every 3 minutes this object generates a deterministic RNG which is used to potentially generate events like
 * blackholes, etc.
 *
 * At startup will replay the last 3 minutes worth of events in a deterministic way.
 *
 */
public class EventScheduler {

    static final long HOUR_IN_MILLIS = 3_600_000L;

    static final long MINUTE_IN_MILLIS = 60_000L;

    static final int GRANULAR_TIME_IN_MINUTES = 3;

    static final long GRANULAR_TIME_IN_MILLIS = GRANULAR_TIME_IN_MINUTES * MINUTE_IN_MILLIS;

    private final PhysicsQueueEnqueue physicsQueueEnqueue;

    private final List<EventFactory> eventFactories = new ArrayList<>();

    long currentTime = -1;
    boolean replayed = false;

    private EventScheduler(PhysicsQueueEnqueue physicsQueueEnqueue) {
        this.physicsQueueEnqueue = physicsQueueEnqueue;
    }

    public static EventScheduler create(PhysicsQueueEnqueue physicsQueueEnqueue) {
        return new EventScheduler(physicsQueueEnqueue);
    }

    public void register(EventFactory eventFactory) {
        this.eventFactories.add(eventFactory);
    }

    public void fixedUpdate(long timestamp) {
        if (!replayed) {
            replay(timestamp);
            replayed = true;
        }
        timer(timestamp);
    }

    void timer(long timestamp) {

        long truncateToHour = (timestamp / HOUR_IN_MILLIS) * HOUR_IN_MILLIS;

        long inHour = timestamp - truncateToHour;

        long truncateToMinute = (inHour / MINUTE_IN_MILLIS) * MINUTE_IN_MILLIS;

        long timeOfHour = truncateToMinute / GRANULAR_TIME_IN_MILLIS;

        if (currentTime != timeOfHour) {
            currentTime = timeOfHour;

            long start = truncateToHour + (timeOfHour * GRANULAR_TIME_IN_MILLIS);

            Random rng = new Random(start);

            long end = start + GRANULAR_TIME_IN_MILLIS;

            eventFactories
                    .stream()
                    .map(ef -> ef.createEvent(rng, start, end))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(physicsQueueEnqueue::enqueue);

        }

    }

    public void replay(long now) {
        for (int i = GRANULAR_TIME_IN_MINUTES; i > 0; i--) {
            timer(now - i * MINUTE_IN_MILLIS);
        }
    }

    public interface EventFactory {
        Optional<LongPredicate> createEvent(Random rng, long start, long end);
    }
}
