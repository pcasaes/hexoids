package me.pcasaes.hexoids.core.domain.index;

import me.pcasaes.hexoids.core.domain.model.Player;

/**
 * Allows us to search for player using a spatial index.
 * The domain does not provide an implementation.
 * A simple implementation would return all players in {@link me.pcasaes.hexoids.core.domain.model.Players}
 */
public interface PlayerSpatialIndex {
    Iterable<Player> search(float x1, float y1, float x2, float y2, float distance);
}
