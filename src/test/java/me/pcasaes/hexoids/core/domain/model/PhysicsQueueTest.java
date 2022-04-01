package me.pcasaes.hexoids.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhysicsQueueTest {

    @Test
    void testEmpty() {

        PhysicsQueue physicsQueue = PhysicsQueue.create();

        assertEquals(0, physicsQueue.fixedUpdate(0L));

    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void testNonRecurring(int processorsCount) {

        var physicsQueue = PhysicsQueue.create();

        List<AtomicLong> processors = IntStream
                .range(0, processorsCount)
                .mapToObj(i -> new AtomicLong(0L))
                .toList();

        processors
                .forEach(p -> {
                    physicsQueue.enqueue(t -> {
                        p.set(t);
                        return false;
                    });
                });

        var timestamp = 12345L;

        assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp));

        processors
                .forEach(p -> assertEquals(timestamp, p.get()));

        assertEquals(0, physicsQueue.fixedUpdate(timestamp + 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void testRecurring(int processorsCount) {

        var physicsQueue = PhysicsQueue.create();

        List<AtomicLong> processors = IntStream
                .range(0, processorsCount)
                .mapToObj(i -> new AtomicLong(0L))
                .toList();

        processors
                .forEach(p -> {
                    physicsQueue.enqueue(t -> {
                        p.set(t);
                        return true;
                    });
                });

        var timestamp1 = 12345L;

        assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp1));

        processors
                .forEach(p -> assertEquals(timestamp1, p.get()));

        var timestamp2 = 55555L;

        assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp2));

        processors
                .forEach(p -> assertEquals(timestamp2, p.get()));
    }
}