package me.pcasaes.hexoids.core.domain.model

import me.pcasaes.hexoids.core.domain.model.PhysicsQueue.Companion.create
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.function.LongPredicate
import java.util.stream.IntStream

class PhysicsQueueTest {
    @Test
    fun testEmpty() {
        val physicsQueue = create()

        Assertions.assertEquals(0, physicsQueue.fixedUpdate(0L))
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun testNonRecurring(processorsCount: Int) {
        val physicsQueue = create()

        val processors = IntStream
            .range(0, processorsCount)
            .mapToObj(IntFunction { i: Int -> AtomicLong(0L) })
            .toList()

        processors
            .forEach { p ->
                physicsQueue.enqueue { t ->
                    p.set(t)
                    false
                }
            }

        val timestamp = 12345L

        Assertions.assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp))

        processors
            .forEach { p -> Assertions.assertEquals(timestamp, p.get()) }

        Assertions.assertEquals(0, physicsQueue.fixedUpdate(timestamp + 1))
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    fun testRecurring(processorsCount: Int) {
        val physicsQueue = create()

        val processors = IntStream
            .range(0, processorsCount)
            .mapToObj { AtomicLong(0L) }
            .toList()

        processors
            .forEach(Consumer { p ->
                physicsQueue.enqueue { t: Long ->
                    p.set(t)
                    true
                }
            })

        val timestamp1 = 12345L

        Assertions.assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp1))

        processors
            .forEach { p -> Assertions.assertEquals(timestamp1, p.get()) }

        val timestamp2 = 55555L

        Assertions.assertEquals(processorsCount, physicsQueue.fixedUpdate(timestamp2))

        processors
            .forEach { p -> Assertions.assertEquals(timestamp2, p.get()) }
    }
}