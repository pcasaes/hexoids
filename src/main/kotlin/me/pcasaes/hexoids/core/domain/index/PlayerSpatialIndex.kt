package me.pcasaes.hexoids.core.domain.index

import me.pcasaes.hexoids.core.domain.model.Player

/**
 * Allows us to search for player using a spatial index.
 * The domain does not provide an implementation.
 * A simple implementation would return all players in [me.pcasaes.hexoids.core.domain.model.Players]
 */
fun interface PlayerSpatialIndex {
    fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Player>
}
