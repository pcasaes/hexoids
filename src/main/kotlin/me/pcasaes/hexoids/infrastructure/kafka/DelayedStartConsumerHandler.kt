package me.pcasaes.hexoids.infrastructure.kafka

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.UniEmitter
import io.smallrye.mutiny.tuples.Tuple3
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers.HaveStarted
import org.apache.kafka.common.TopicPartition
import org.eclipse.microprofile.reactive.messaging.Incoming
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

@ApplicationScoped
class DelayedStartConsumerHandler {

    private val emitters = ConcurrentHashMap.newKeySet<UniEmitter<in Unit>>()

    private val lastOffsets = ConcurrentHashMap<TopicPartition, Long>()

    private val started = AtomicBoolean(false)
    private val caughtUp = AtomicBoolean(false)


    @PreDestroy
    fun stop() {
        this.lastOffsets.clear()
        this.tryStartup()
    }

    fun start() {
        if (this.started.compareAndSet(false, true)) {
            this.emitters
                .forEach { ue -> ue.complete(Unit) }
            LOGGER.info("Started up delayed consumers")
        }
    }

    fun register(nextOffsets: MutableMap<TopicPartition, Long?>) {
        nextOffsets
            .entries
            .stream()
            .forEach { entry ->
                val v = entry.value
                if (v != null && v > 0) {
                    LOGGER.info { "Last offset: $entry" }
                    this.lastOffsets.put(entry.key, v - 1)
                }
            }

        this.tryStartup()
    }

    @Incoming("join-game-metadata")
    fun joinGameTime(metadata: Tuple3<String, Int, Long>) {
        if (!caughtUp.plain && !caughtUp.get()) {
            val topicPartition = TopicPartition(metadata.getItem1(), metadata.getItem2())
            val last = this.lastOffsets[topicPartition]
            if (last != null && last <= metadata.getItem3()) {
                LOGGER.info { "Reach offset $last for $topicPartition" }
                this.lastOffsets.remove(topicPartition)
            }
            this.tryStartup()
        }
    }

    private fun tryStartup() {
        if (this.lastOffsets.isEmpty() && this.caughtUp.compareAndSet(false, true)) {
            LOGGER.info("Starting up delayed consumers")
            this.start()
        }
    }

    fun onStarted(): Uni<Unit> {
        return Uni
            .createFrom()
            .emitter { uniEmitter ->
                if (this.started.get()) {
                    uniEmitter.complete(Unit)
                } else {
                    emitters.add(uniEmitter)
                }
            }
    }


    private fun hasStarted(): Boolean {
        return this.caughtUp.plain || this.caughtUp.get()
    }


    @Produces
    fun getHaveStarted(): HaveStarted = HaveStarted { this.hasStarted() }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(DelayedStartConsumerHandler::class.java.getName())
    }
}
