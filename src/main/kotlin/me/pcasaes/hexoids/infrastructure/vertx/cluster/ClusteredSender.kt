package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.quarkus.runtime.Startup
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.GameTopic
import me.pcasaes.hexoids.core.domain.model.EventRecord
import org.eclipse.microprofile.reactive.messaging.Incoming
import pcasaes.hexoids.record.proto.UUIDKey
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

@ApplicationScoped
@Startup
class ClusteredSender @Inject constructor(
    private val bootVertx: Vertx,
) : AbstractVerticle() {
    private val started = AtomicBoolean(false)

    private val key: UUIDKey.Builder = UUIDKey.newBuilder()
    private val eventRecord = pcasaes.hexoids.record.proto.EventRecord
        .newBuilder()
        .setKey(key)

    @PostConstruct
    fun startup() {
        bootVertx.deployVerticle(this)
    }

    @PreDestroy
    fun shutdown() {
        if (this.started.get()) {
            bootVertx.undeploy(this.deploymentID())
        }
    }

    @Throws(Exception::class)
    override fun start(startPromise: Promise<Void?>) {
        this.started.set(true)
        super.start(startPromise)
        LOGGER.info("Started")
    }

    @Throws(Exception::class)
    override fun stop(stopPromise: Promise<Void?>) {
        this.started.set(false)
        super.stop(stopPromise)
        LOGGER.info("Stopped")
    }

    @Incoming("player-action-out")
    fun sendPlayerAction(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.PLAYER_ACTION_TOPIC, true)
    }

    @Incoming("bolt-action-out")
    fun sendBoltAction(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.BOLT_ACTION_TOPIC, true)
    }

    @Incoming("bolt-life-cycle-out")
    fun sendBoltLifeCycle(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.BOLT_LIFECYCLE_TOPIC, false)
    }

    @Incoming("score-board-update-out")
    fun sendScoreBoardUpdate(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.SCORE_BOARD_UPDATE_TOPIC, true)
    }

    @Incoming("score-board-control-out")
    fun sendScoreBoardControl(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.SCORE_BOARD_CONTROL_TOPIC, false)
    }

    @Incoming("join-game-out-broadcast")
    fun sendJoinGame(consumerRecord: EventRecord) {
        trySendInContext(consumerRecord, GameTopic.JOIN_GAME_TOPIC, true)
    }

    private fun trySendInContext(consumerRecord: EventRecord, topic: GameTopic, broadcast: Boolean) {
        if (this.started.get()) {
            context.runOnContext { send(consumerRecord, topic, broadcast) }
        } else {
            LOGGER.warning("Not started yet. dropping message")
        }
    }

    private fun send(
        consumerRecord: EventRecord,
        topic: GameTopic,
        broadcast: Boolean,
    ) {
        val eventBus = getVertx().eventBus()

        this.key.setMostSignificantDigits(consumerRecord.key.mostSignificantBits)
            .setLeastSignificantDigits(consumerRecord.key.leastSignificantBits)

        this.eventRecord.setKey(this.key)

        val event = consumerRecord.event
        if (event != null) {
            this.eventRecord.setEvent(event)
        } else {
            this.eventRecord.clearEvent()
        }

        val channel = topic.name

        if (broadcast) {
            eventBus.publish(
                channel, eventRecord
                    .build().toByteArray()
            )
        } else {
            eventBus.send(
                channel, eventRecord
                    .build().toByteArray()
            )
        }
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ClusteredSender::class.java.getName())
    }
}
