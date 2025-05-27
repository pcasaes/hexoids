package me.pcasaes.hexoids.infrastructure.vertx.cluster

import io.quarkus.runtime.Startup
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.mutiny.core.Vertx
import io.vertx.mutiny.core.shareddata.AsyncMap
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.spi.ObserverMethod
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.model.EntityId
import me.pcasaes.hexoids.core.domain.repostiory.ScoreBoardRepository
import me.pcasaes.hexoids.core.domain.repostiory.ScoreBoardRepositoryFactory
import pcasaes.hexoids.record.proto.PlayerScore
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

@ApplicationScoped
@Startup(ObserverMethod.DEFAULT_PRIORITY - 700)
class ClusteredScoreBoardRepository @Inject constructor(
    private val vertx: Vertx
) : ScoreBoardRepository, AbstractVerticle() {

    private val log: Logger = Logger.getLogger(ClusteredScoreBoardRepository::class.java.name)

    private val store = AtomicReference<AsyncMap<UUID, ByteArray>>(null)

    init {
        vertx.deployVerticle(this).subscribe().with { }
        ScoreBoardRepositoryFactory.register(this)
    }

    override fun start(startPromise: Promise<Void?>?) {
        vertx.sharedData().getAsyncMap<UUID, ByteArray>("playerScores")
            .onItem()
            .invoke { playerScores ->
                store.set(playerScores)
                startPromise?.complete()
                log.info { "Player Scores Loaded" }
            }
            .subscribe().with { }
    }

    private fun getStore(): AsyncMap<UUID, ByteArray>? {
        return store.plain ?: store.get()
    }

    override fun fetchPlayerScore(playerId: EntityId): Uni<PlayerScore?> {
        val s = getStore()
        return if (s != null) {
            s[playerId.getId()]
                .onItem().transform { bytes ->
                    bytes?.let { PlayerScore.parseFrom(it) }
                }
        } else {
            Uni.createFrom().nullItem()
        }
    }

    override fun savePlayerScore(playerScore: PlayerScore): Uni<Unit> {
        val s = getStore()
        return if (s != null) {
            s.put(EntityId.of(playerScore.playerId).getId(), playerScore.toByteArray())
                .onItem().transform { Unit }
        } else {
            Uni.createFrom().nullItem()
        }
    }

    override fun reset(playerId: EntityId): Uni<Unit> {
        val s = getStore()
        return if (s != null) {
            s.remove(playerId.getId())
                .onItem().transform { Unit }
        } else {
            Uni.createFrom().nullItem()
        }
    }
}