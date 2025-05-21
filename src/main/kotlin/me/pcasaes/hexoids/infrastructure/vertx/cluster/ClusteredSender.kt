package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.quarkus.runtime.Startup
import io.smallrye.reactive.messaging.kafka.Record
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.GameTopic
import org.eclipse.microprofile.reactive.messaging.Incoming
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.record.proto.EventRecord
import pcasaes.hexoids.record.proto.UUIDKey
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Startup
class ClusteredSender @Inject constructor(private val bootVertx: Vertx) : AbstractVerticle() {
    private var started = false

    private val key: UUIDKey.Builder = UUIDKey.newBuilder()
    private val eventRecord: EventRecord.Builder = EventRecord
        .newBuilder()
        .setKey(key)


    @PostConstruct
    fun startup() {
        bootVertx.deployVerticle(this)
    }

    @PreDestroy
    fun shutdown() {
        if (this.started) {
            bootVertx.undeploy(this.deploymentID())
        }
    }

    @Throws(Exception::class)
    override fun start(startPromise: Promise<Void?>) {
        this.started = true
        super.start(startPromise)
        LOGGER.info("Started")
    }

    @Throws(Exception::class)
    override fun stop(stopPromise: Promise<Void?>) {
        this.started = false
        super.stop(stopPromise)
        LOGGER.info("Stopped")
    }

    @Incoming("player-action-out")
    fun sendPlayerAction(consumerRecord: Record<UUID, Event?>) {
        trySendInContext(consumerRecord, GameTopic.PLAYER_ACTION_TOPIC, true)
    }

    @Incoming("bolt-action-out")
    fun sendBoltAction(consumerRecord: Record<UUID, Event?>) {
        trySendInContext(consumerRecord, GameTopic.BOLT_ACTION_TOPIC, true)
    }

    @Incoming("bolt-life-cycle-out")
    fun sendBoltLifeCycle(consumerRecord: Record<UUID, Event?>) {
        trySendInContext(consumerRecord, GameTopic.BOLT_LIFECYCLE_TOPIC, false)
    }

    private fun trySendInContext(consumerRecord: Record<UUID, Event?>, topic: GameTopic, broadcast: Boolean) {
        if (this.started) {
            context.runOnContext { send(consumerRecord, topic, broadcast) }
        } else {
            LOGGER.warning("Not started yet. dropping message")
        }
    }

    private fun send(consumerRecord: Record<UUID, Event?>, topic: GameTopic, broadcast: Boolean) {
        val eventBus = getVertx().eventBus()

        this.key.setMostSignificantDigits(consumerRecord.key().mostSignificantBits)
            .setLeastSignificantDigits(consumerRecord.key().leastSignificantBits)

        this.eventRecord.setKey(this.key)

        if (consumerRecord.value() != null) {
            this.eventRecord.setEvent(consumerRecord.value())
        } else {
            this.eventRecord.clearEvent()
        }

        if (broadcast) {
            eventBus.publish(
                topic.name, eventRecord
                    .build().toByteArray()
            )
        } else {
            eventBus.send(
                topic.name, eventRecord
                    .build().toByteArray()
            )
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ClusteredSender::class.java.getName())
    }
}
