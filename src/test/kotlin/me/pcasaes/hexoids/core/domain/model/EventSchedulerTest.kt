package me.pcasaes.hexoids.core.domain.model

import io.mockk.mockk
import io.mockk.verify
import me.pcasaes.hexoids.core.domain.model.EventScheduler.Companion.create
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.LongPredicate

class EventSchedulerTest {
    @Test
    fun testEnqueueOnCreate() {
        val physicsQueueEnqueue = mockk<PhysicsQueueEnqueue>(relaxed = true)
        val scheduler = create(physicsQueueEnqueue)

        scheduler.register { r, s, e ->
            LongPredicate { true }
        }

        scheduler.timer(0L)

        verify(exactly = 1) { physicsQueueEnqueue.enqueue(any()) }
    }

    @Test
    fun testReplay() {
        val physicsQueueEnqueue = mockk<PhysicsQueueEnqueue>(relaxed = true)
        val scheduler = create(physicsQueueEnqueue)

        val counter = AtomicInteger(0)
        scheduler.register { r, s, e ->
            val c = counter.getAndIncrement()
            Assertions.assertEquals(c * EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS + 0L, s)
            Assertions.assertEquals(
                c * EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS,
                e
            )
            LongPredicate { true }
        }
        scheduler.fixedUpdate(180000L)

        Assertions.assertEquals(2, counter.get())

        verify(exactly = 2) { physicsQueueEnqueue.enqueue(any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25])
    fun testZeroTimestamp(hour: Int) {
        val hoursInMillis = hour * EventScheduler.Companion.HOUR_IN_MILLIS

        val physicsQueueEnqueue = mockk<PhysicsQueueEnqueue>(relaxed = true)
        val scheduler = create(physicsQueueEnqueue)

        scheduler.register { r, s, e ->
            Assertions.assertEquals(hoursInMillis + 0L, s)
            Assertions.assertEquals(hoursInMillis + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS, e)
            LongPredicate { true }
        }

        scheduler.timer(hoursInMillis + 0L)

        verify(exactly = 1) { physicsQueueEnqueue.enqueue(any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25])
    fun test180Minus1Timestamp(hour: Int) {
        val hoursInMillis = hour * EventScheduler.Companion.HOUR_IN_MILLIS

        val physicsQueueEnqueue = mockk<PhysicsQueueEnqueue>(relaxed = true)
        val scheduler = create(physicsQueueEnqueue)

        scheduler.register { r, s, e ->
            Assertions.assertEquals(hoursInMillis + 0L, s)
            Assertions.assertEquals(hoursInMillis + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS, e)
            LongPredicate { true }
        }

        scheduler.timer(hoursInMillis + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS - 1)

        verify(exactly = 1) { physicsQueueEnqueue.enqueue(any()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25])
    fun test180Timestamp(hour: Int) {
        val hoursInMillis = hour * EventScheduler.Companion.HOUR_IN_MILLIS

        val physicsQueueEnqueue = mockk<PhysicsQueueEnqueue>(relaxed = true)
        val scheduler = create(physicsQueueEnqueue)

        scheduler.register { r, s, e ->
            Assertions.assertEquals(hoursInMillis + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS, s)
            Assertions.assertEquals(hoursInMillis + 2 * EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS, e)
            LongPredicate { true }
        }

        scheduler.timer(hoursInMillis + EventScheduler.Companion.GRANULAR_TIME_IN_MILLIS)

        verify(exactly = 1) { physicsQueueEnqueue.enqueue(any()) }
    }
}