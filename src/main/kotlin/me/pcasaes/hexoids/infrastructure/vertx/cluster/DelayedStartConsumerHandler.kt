package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.UniEmitter
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import me.pcasaes.hexoids.core.application.eventhandlers.ApplicationConsumers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

@ApplicationScoped
class DelayedStartConsumerHandler {

    private val emitters = ConcurrentHashMap.newKeySet<UniEmitter<in Unit>>()


    private val started = AtomicBoolean(false)


    @PreDestroy
    fun stop() {
        start()
    }

    fun start() {
        if (this.started.compareAndSet(false, true)) {
            this.emitters
                .forEach { ue -> ue.complete(Unit) }
            LOGGER.info("Started up delayed consumers")
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
        return this.started.plain || this.started.get()
    }


    @Produces
    fun getHaveStarted(): ApplicationConsumers.HaveStarted = ApplicationConsumers.HaveStarted { this.hasStarted() }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(DelayedStartConsumerHandler::class.java.getName())
    }
}