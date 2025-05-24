package me.pcasaes.hexoids.infrastructure.vertx.cluster

import com.google.protobuf.InvalidProtocolBufferException
import io.quarkus.runtime.Startup
import io.smallrye.mutiny.subscription.Cancellable
import io.smallrye.reactive.messaging.kafka.Record
import io.vertx.core.Vertx
import io.vertx.core.eventbus.MessageConsumer
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import me.pcasaes.hexoids.core.domain.model.GameTopic
import me.pcasaes.hexoids.infrastructure.kafka.DelayedStartConsumerHandler
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.record.proto.EventRecord
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger
import kotlin.concurrent.Volatile

@ApplicationScoped
@Startup
class ClusteredReceiver(
    private val vertx: Vertx,
    private val delayedStartConsumerHandler: DelayedStartConsumerHandler,
    @ConfigProperty(name = "hexoids.service.subscriber", defaultValue = "true") isSubscriber: Boolean,
    @Channel("player-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) playerActionEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("bolt-action") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltActionEmitter: Emitter<Record<UUID, Event?>>,
    @Channel("bolt-life-cycle") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER) boltLifeCycleEmitter: Emitter<Record<UUID, Event?>>
) {
    private val emitters: Array<Emitter<Record<UUID, Event?>>?>

    private val consumers = CopyOnWriteArrayList<MessageConsumer<ByteArray?>>()

    @Volatile
    private lateinit var startupSubscription: Cancellable

    init {
        val ems = arrayOfNulls<Emitter<Record<UUID, Event?>>?>(GameTopic.entries.size)

        ems[GameTopic.PLAYER_ACTION_TOPIC.ordinal] = playerActionEmitter
        ems[GameTopic.BOLT_ACTION_TOPIC.ordinal] = boltActionEmitter

        if (isSubscriber) {
            ems[GameTopic.BOLT_LIFECYCLE_TOPIC.ordinal] = boltLifeCycleEmitter
        } else {
            LOGGER.warning("bolt lifecycle topic consumer disabled")
        }

        this.emitters = ems
    }

    @PostConstruct
    fun startup() {
        this.startupSubscription = delayedStartConsumerHandler
            .onStarted()
            .invoke(Runnable {
                GameTopic.entries
                    .filter { topic -> emitters[topic.ordinal] != null }
                    .forEach { topic ->
                        val emitter = emitters[topic.ordinal]
                        if (emitter != null) {
                            this.configureConsumer(topic, emitter)
                        }
                    }
            }
            ).subscribe()
            .with { }
    }

    @PreDestroy
    fun shutdown() {
        this.startupSubscription.cancel()
        consumers
            .forEach { obj -> obj.unregister() }
    }

    private fun configureConsumer(topic: GameTopic, emitter: Emitter<Record<UUID, Event?>>) {
        val eventBus = vertx.eventBus()
        val consumer = eventBus.consumer<ByteArray>(
            topic.name
        ) { receivedMessage ->
            try {
                val eventRecord = EventRecord.newBuilder().mergeFrom(receivedMessage.body()).build()
                val key = UUID(
                    eventRecord.key.mostSignificantDigits,
                    eventRecord.key.leastSignificantDigits
                )
                emitter.send(Record.of<UUID, Event?>(key, eventRecord.event))
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
