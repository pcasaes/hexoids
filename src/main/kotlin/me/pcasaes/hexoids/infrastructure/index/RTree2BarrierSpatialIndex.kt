package me.pcasaes.hexoids.infrastructure.index

import com.github.davidmoten.rtree2.Entries
import com.github.davidmoten.rtree2.Entry
import com.github.davidmoten.rtree2.RTree
import com.github.davidmoten.rtree2.geometry.Geometries
import com.github.davidmoten.rtree2.geometry.Rectangle
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import me.pcasaes.hexoids.core.domain.index.BarrierSpatialIndex
import me.pcasaes.hexoids.core.domain.model.Barrier
import kotlin.math.max
import kotlin.math.min

@ApplicationScoped
class RTree2BarrierSpatialIndex : BarrierSpatialIndex {
    private var index: RTree<Barrier, Rectangle>? = null

    private val results = ArrayList<Barrier>()

    fun startup(@Observes event: StartupEvent) {
        // eager startup
    }

    override fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Barrier> {

        val localIndex = this.index
        return if (localIndex == null) {
            listOf()
        } else {
            this.results.clear()
            localIndex
                .search(
                    Geometries
                        .rectangle(
                            min(x1, x2), min(y1, y2),
                            max(x1, x2), max(y1, y2)
                        ), distance.toDouble()
                )
                .forEach { entry: Entry<Barrier, Rectangle> -> this.results.add(entry.value()) }

            this.results
        }
    }

    override fun update(barriers: Iterable<Barrier>) {
        index = RTree.create(
            barriers
                .map { p ->
                    Entries.entry(
                        p, Geometries
                            .rectangle(
                                min(p.from.x, p.to.x),
                                min(p.from.y, p.to.y),
                                max(p.from.x, p.to.x),
                                max(p.from.y, p.to.y)
                            )
                    )
                }
        )
    }
}
