package me.pcasaes.hexoids.infrastructure.vertx.cluster

import com.google.protobuf.InvalidProtocolBufferException
import io.quarkus.runtime.Startup
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import me.pcasaes.hexoids.core.domain.model.EventRecord
import me.pcasaes.hexoids.core.domain.model.GameTopic
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

@ApplicationScoped
@Startup
class ClusteredReceiver(
    private val vertx: Vertx,
    private val delayedStartConsumerHandler: DelayedStartConsumerHandler,
    @ConfigProperty(name = "hexoids.service.subscriber", defaultValue = "true") isSubscriber: Boolean,
    @Channel("player-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) playerActionEmitter: Emitter<EventRecord>,
    @Channel("bolt-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltActionEmitter: Emitter<EventRecord>,
    @Channel("bolt-life-cycle") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltLifeCycleEmitter: Emitter<EventRecord>,
    @Channel("score-board-update") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardUpdateEmitter: Emitter<EventRecord>,
    @Channel("score-board-control") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) scoreBoardControlEmitter: Emitter<EventRecord>,
    @Channel("join-game") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) joinGameEmitter: Emitter<EventRecord>,
) {
    private val emitters: Array<Emitter<EventRecord>?>

    private val consumers = CopyOnWriteArrayList<MessageConsumer<ByteArray?>>()

    init {
        val ems = arrayOfNulls<Emitter<EventRecord>?>(GameTopic.entries.size)

        ems[GameTopic.PLAYER_ACTION_TOPIC.ordinal] = playerActionEmitter
        ems[GameTopic.BOLT_ACTION_TOPIC.ordinal] = boltActionEmitter
        ems[GameTopic.JOIN_GAME_TOPIC.ordinal] = joinGameEmitter
        ems[GameTopic.SCORE_BOARD_UPDATE_TOPIC.ordinal] = scoreBoardUpdateEmitter

        if (isSubscriber) {
            ems[GameTopic.SCORE_BOARD_CONTROL_TOPIC.ordinal] = scoreBoardControlEmitter
            ems[GameTopic.BOLT_LIFECYCLE_TOPIC.ordinal] = boltLifeCycleEmitter
        } else {
            LOGGER.warning("bolt lifecycle topic consumer disabled")
        }

        this.emitters = ems
    }

    @PostConstruct
    fun startup() {
        GameTopic.entries
            .filter { topic -> emitters[topic.ordinal] != null }
            .forEach { topic ->
                val emitter = emitters[topic.ordinal]
                if (emitter != null) {
                    if (topic == GameTopic.JOIN_GAME_TOPIC) {
                        this.configureConsumer(topic, emitter)
                    } else {
                        this.delayedStartConsumerHandler.onStarted()
                            .subscribe().with {
                                this.configureConsumer(topic, emitter)
                            }
                    }
                }
            }
    }

    @PreDestroy
    fun shutdown() {
        consumers
            .forEach { obj -> obj.unregister() }
    }

    private fun configureConsumer(topic: GameTopic, emitter: Emitter<EventRecord>) {
        val eventBus = vertx.eventBus()
        val consumer = eventBus.consumer<ByteArray>(
            topic.name
        ) { receivedMessage ->
            try {
                val eventRecord = pcasaes.hexoids.record.proto.EventRecord.newBuilder().mergeFrom(receivedMessage.body()).build()
                val key = UUID(
                    eventRecord.key.mostSignificantDigits,
                    eventRecord.key.leastSignificantDigits
                )
                val event = if (eventRecord.hasEvent()) {
                    eventRecord.event
                } else {
                    null
                }
                emitter.send(EventRecord.of(key, event))
            } catch (ex: InvalidProtocolBufferException) {
                LOGGER.severe("Could not process record: $ex")
            }
        }
        consumer.completionHandler { res ->
            if (res.failed()) {
                LOGGER.severe { "Failed to subscribe to $topic: ${res.cause()}" }
            } else {
                LOGGER.info { "Subscribed to $topic" }
            }
        }

        this.consumers.add(consumer)
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(ClusteredReceiver::class.java.getName())
    }
}
