package me.pcasaes.hexoids.infrastructure.index

import com.github.davidmoten.rtree2.Entries
import com.github.davidmoten.rtree2.RTree
import com.github.davidmoten.rtree2.geometry.Geometries
import com.github.davidmoten.rtree2.geometry.Point
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.quarkus.runtime.StartupEvent
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex
import me.pcasaes.hexoids.core.domain.model.Game
import me.pcasaes.hexoids.core.domain.model.Player
import java.util.concurrent.TimeUnit
import java.util.function.LongPredicate
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

@ApplicationScoped
class RTree2PlayerSpatialIndex @Inject constructor(
    gameQueue: GameQueue,
    meterRegistry: MeterRegistry,
    private val vertx: Vertx
) : PlayerSpatialIndex {


    companion object {
        private val LOGGER: Logger = Logger.getLogger(RTree2PlayerSpatialIndex::class.java.getName())
    }

    private val results = ArrayList<Player>()

    private val updater: Updater = Updater(gameQueue, meterRegistry)

    init {
        LOGGER.info("Using RTree2 Spatial Index")
    }

    fun startup(@Observes event: StartupEvent) {
        this.vertx.deployVerticle(updater)
    }

    @PreDestroy
    fun stop() {
        this.vertx.undeploy(updater.deploymentID())
    }


    override fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Player> {
        val index = this.updater.index
        return if (index == null) {
            emptyList()
        } else {
            this.results.clear()
            index
                .search(
                    Geometries
                        .rectangle(
                            min(x1, x2), min(y1, y2),
                            max(x1, x2), max(y1, y2)
                        ), distance.toDouble()
                )
                .forEach { entry -> this.results.add(entry.value()) }

            this.results
        }
    }

    private class Updater(
        private val gameQueue: GameQueue,
        meterRegistry: MeterRegistry
    ) : AbstractVerticle(), LongPredicate {
        private val timer: Timer
        private var running = false
        var index: RTree<Player, Point>? = null
            private set

        init {
            this.timer = Timer.builder("player_spatial_index_update")
                .description("Time to update the player spatial index.")
                .publishPercentiles(0.5, 0.75, 0.90, 0.95)
                .register(meterRegistry)
        }

        @Throws(Exception::class)
        override fun start(startPromise: Promise<Void?>) {
            this.context.runOnContext { _ ->
                running = true
                this.gameQueue.enqueue { Game.get().getPhysicsQueue().enqueue(this) }
                startPromise.complete()
            }
        }

        @Throws(Exception::class)
        override fun stop(stopPromise: Promise<Void?>) {
            this.context
                .runOnContext { _ ->
                    running = false
                    stopPromise.complete()
                }
        }

        override fun test(l: Long): Boolean {
            val now = System.nanoTime()
            val list = ArrayList<Player>()
            Game.get()
                .getPlayers()
                .forEach { list.add(it) }

            update(list, now)

            return false
        }

        fun update(players: List<Player>, start: Long) {
            this.context
                .runOnContext { _ ->
                    if (running) {
                        val rtree = RTree.create(
                            players
                                .map { p ->
                                    Entries.entry(
                                        p,
                                        Geometries.point(p.getX(), p.getY())
                                    )
                                }
                        )
                        finishedUpdate(rtree, start)
                    }
                }
        }

        fun finishedUpdate(result: RTree<Player, Point>?, start: Long) {
            this.gameQueue.enqueue {
                this.index = result
                timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
                Game.get().getPhysicsQueue().enqueue(this)
            }
        }
    }

}
