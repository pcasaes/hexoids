package me.pcasaes.hexoids.core.domain.index

import me.pcasaes.hexoids.core.domain.model.Barrier

/**
 * Allows us to search for barriers using a spatial index.
 * The domain does not provide an implementation.
 * A simple implementation would return all barriers in [Barrier]
 */
interface BarrierSpatialIndex {
    fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Barrier>

    fun update(barriers: Iterable<Barrier>) {
        // default implementation does nothing
    }
}
