package me.pcasaes.hexoids.core.domain.model

import java.util.Optional
import java.util.Random
import java.util.function.LongPredicate

/**
 * Every 3 minutes this object generates a deterministic RNG which is used to potentially generate events like
 * blackholes, etc.
 *
 * At startup will replay the last 3 minutes worth of events in a deterministic way.
 *
 */
class EventScheduler private constructor(private val physicsQueueEnqueue: PhysicsQueueEnqueue) {
    private val eventFactories: MutableList<EventFactory> = ArrayList<EventFactory>()

    var currentTime: Long = -1
    var replayed: Boolean = false

    fun register(eventFactory: EventFactory) {
        this.eventFactories.add(eventFactory)
    }

    fun fixedUpdate(timestamp: Long) {
        if (!replayed) {
            replay(timestamp)
            replayed = true
        }
        timer(timestamp)
    }

    fun timer(timestamp: Long) {
        val truncateToHour: Long = (timestamp / HOUR_IN_MILLIS) * HOUR_IN_MILLIS

        val inHour = timestamp - truncateToHour

        val truncateToMinute: Long = (inHour / MINUTE_IN_MILLIS) * MINUTE_IN_MILLIS

        val timeOfHour: Long = truncateToMinute / GRANULAR_TIME_IN_MILLIS

        if (currentTime != timeOfHour) {
            currentTime = timeOfHour

            val start: Long = truncateToHour + (timeOfHour * GRANULAR_TIME_IN_MILLIS)

            val rng = Random(start)

            val end: Long = start + GRANULAR_TIME_IN_MILLIS

            eventFactories
                .stream()
                .map { ef -> ef.createEvent(rng, start, end) }
                .filter { obj -> obj.isPresent }
                .map { obj -> obj.get() }
                .forEach { action -> physicsQueueEnqueue.enqueue(action) }
        }
    }

    fun replay(now: Long) {
        for (i in GRANULAR_TIME_IN_MINUTES downTo 1) {
            timer(now - i * MINUTE_IN_MILLIS)
        }
    }

    fun interface EventFactory {
        fun createEvent(rng: Random, start: Long, end: Long): Optional<LongPredicate>
    }

    companion object {
        const val HOUR_IN_MILLIS: Long = 3600000L

        const val MINUTE_IN_MILLIS: Long = 60000L

        const val GRANULAR_TIME_IN_MINUTES: Int = 3

        const val GRANULAR_TIME_IN_MILLIS: Long = GRANULAR_TIME_IN_MINUTES * MINUTE_IN_MILLIS

        @JvmStatic
        fun create(physicsQueueEnqueue: PhysicsQueueEnqueue): EventScheduler {
            return EventScheduler(physicsQueueEnqueue)
        }
    }
}
