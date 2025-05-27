package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.quarkus.runtime.Startup
import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.shareddata.AsyncMap
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.EventRecord
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.OnOverflow
import pcasaes.hexoids.proto.Event
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

@ApplicationScoped
@Startup
class JoinedPlayersCache @Inject constructor(
    private val vertx: Vertx,
    @Channel("join-game-out-broadcast") @OnOverflow(OnOverflow.Strategy.UNBOUNDED_BUFFER)
    private val joinGameEmitter: Emitter<EventRecord>,
    private val delayedStartConsumerHandler: DelayedStartConsumerHandler,

    ) : AbstractVerticle() {

    private var joinedPlayers: AsyncMap<UUID, ByteArray>? = null

    private val futures = ConcurrentLinkedDeque<Pair<EventRecord, CompletableFuture<EventRecord>>>()

    private val log: Logger = Logger.getLogger(JoinedPlayersCache::class.java.name)

    private val contextHolder = AtomicReference<Context?>(null)

    init {
        vertx.deployVerticle(this)
    }

    override fun start(startPromise: Promise<Void?>) {
        vertx.sharedData().getAsyncMap<UUID, ByteArray>("joinedPlayers") { res ->
            val context = this.context
            contextHolder.set(context)
            context.runOnContext {
                if (res.succeeded()) {
                    val store = res.result()

                    val counter = AtomicInteger(0)
                    store.entries()
                        .onSuccess { h ->
                            context.runOnContext {
                                val builder = Event.newBuilder()
                                h.forEach { (key, v) ->
                                    val value = builder
                                        .clear()
                                        .mergeFrom(v)
                                        .build()

                                    val record = EventRecord.of(key, value)
                                    joinGameEmitter.send(record)
                                    counter.incrementAndGet()
                                }

                                var future: Pair<EventRecord, CompletableFuture<EventRecord>>? =
                                    futures.poll()
                                while (future != null) {
                                    updateStore(store, future.first, future.second)

                                    future = futures.poll()
                                }

                                joinedPlayers = store

                                delayedStartConsumerHandler.start()
                                startPromise.complete()

                                log.info { "Loaded $counter players from cache" }
                            }
                        }


                } else {
                    log.severe { "Could not start player cache ${res.cause()}" }
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        if (this.contextHolder.get() != null) {
            vertx.undeploy(this.deploymentID())
        }
    }


    private fun updateStore(
        store: AsyncMap<UUID, ByteArray>,
        consumerRecord: EventRecord,
        future: CompletableFuture<EventRecord>
    ) {
        val event = consumerRecord.event
        if (event != null) {
            store.put(consumerRecord.key, event.toByteArray()) { res ->
                if (!res.succeeded()) {
                    log.warning { "Failed to add player to cache: ${res.cause()}" }
                }
                future.complete(consumerRecord)
            }
        } else {
            store.remove(consumerRecord.key) { res ->
                if (!res.succeeded()) {
                    log.warning { "Failed to remove player from cache: ${res.cause()}" }
                }
                future.complete(consumerRecord)
            }
        }

    }

    @Incoming("join-game-out")
    fun sendJoinGame(consumerRecord: EventRecord) {
        val context = contextHolder.plain ?: contextHolder.get()

        val future = CompletableFuture<EventRecord>()
        if (context == null) {
            futures.add(consumerRecord to future)
        } else {
            context.runOnContext {
                val store = this.joinedPlayers
                if (store != null) {
                    updateStore(store, consumerRecord, future)
                } else {
                    futures.add(consumerRecord to future)
                }
            }
        }

        future.whenComplete { r, throwable ->
            if (throwable != null) {
                log.warning { "Failed to process record: ${throwable.message}" }
            } else {
                joinGameEmitter.send(r)
            }
        }
    }

}