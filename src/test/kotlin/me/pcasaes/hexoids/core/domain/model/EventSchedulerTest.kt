package me.pcasaes.hexoids.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;

import static me.pcasaes.hexoids.core.domain.model.EventScheduler.GRANULAR_TIME_IN_MILLIS;
import static me.pcasaes.hexoids.core.domain.model.EventScheduler.HOUR_IN_MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventSchedulerTest {

    @Test
    void testEnqueueOnCreate() {
        PhysicsQueueEnqueue physicsQueueEnqueue = mock(PhysicsQueueEnqueue.class);
        EventScheduler scheduler = EventScheduler.create(physicsQueueEnqueue);

        scheduler.register((r, s, e) -> Optional.of(l -> true));

        scheduler.timer(0L);

        verify(physicsQueueEnqueue, times(1)).enqueue(any(LongPredicate.class));
    }

    @Test
    void testReplay() {
        PhysicsQueueEnqueue physicsQueueEnqueue = mock(PhysicsQueueEnqueue.class);
        EventScheduler scheduler = EventScheduler.create(physicsQueueEnqueue);

        AtomicInteger counter = new AtomicInteger(0);
        scheduler.register((r, s, e) -> {

            int c = counter.getAndIncrement();
            assertEquals(c * GRANULAR_TIME_IN_MILLIS + 0L, s);
            assertEquals(c * GRANULAR_TIME_IN_MILLIS + GRANULAR_TIME_IN_MILLIS, e);

            return Optional.of(l -> true);
        });
        scheduler.fixedUpdate(180_000L);

        assertEquals(2, counter.get());

        verify(physicsQueueEnqueue, times(2)).enqueue(any(LongPredicate.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25})
    void testZeroTimestamp(int hour) {
        long hoursInMillis = hour * HOUR_IN_MILLIS;

        PhysicsQueueEnqueue physicsQueueEnqueue = mock(PhysicsQueueEnqueue.class);
        EventScheduler scheduler = EventScheduler.create(physicsQueueEnqueue);

        scheduler.register((r, s, e) -> {

            assertEquals(hoursInMillis + 0L, s);
            assertEquals(hoursInMillis + GRANULAR_TIME_IN_MILLIS, e);

            return Optional.of(l -> true);
        });

        scheduler.timer(hoursInMillis + 0L);

        verify(physicsQueueEnqueue, times(1)).enqueue(any(LongPredicate.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25})
    void test180Minus1Timestamp(int hour) {
        long hoursInMillis = hour * HOUR_IN_MILLIS;

        PhysicsQueueEnqueue physicsQueueEnqueue = mock(PhysicsQueueEnqueue.class);
        EventScheduler scheduler = EventScheduler.create(physicsQueueEnqueue);

        scheduler.register((r, s, e) -> {

            assertEquals(hoursInMillis + 0L, s);
            assertEquals(hoursInMillis + GRANULAR_TIME_IN_MILLIS, e);

            return Optional.of(l -> true);
        });

        scheduler.timer(hoursInMillis + GRANULAR_TIME_IN_MILLIS - 1);

        verify(physicsQueueEnqueue, times(1)).enqueue(any(LongPredicate.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25})
    void test180Timestamp(int hour) {
        long hoursInMillis = hour * HOUR_IN_MILLIS;

        PhysicsQueueEnqueue physicsQueueEnqueue = mock(PhysicsQueueEnqueue.class);
        EventScheduler scheduler = EventScheduler.create(physicsQueueEnqueue);

        scheduler.register((r, s, e) -> {

            assertEquals(hoursInMillis + GRANULAR_TIME_IN_MILLIS, s);
            assertEquals(hoursInMillis + 2 * GRANULAR_TIME_IN_MILLIS, e);

            return Optional.of(l -> true);
        });

        scheduler.timer(hoursInMillis + GRANULAR_TIME_IN_MILLIS);

        verify(physicsQueueEnqueue, times(1)).enqueue(any(LongPredicate.class));
    }
}