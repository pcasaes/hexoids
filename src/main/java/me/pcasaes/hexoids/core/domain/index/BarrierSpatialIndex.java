package me.pcasaes.hexoids.core.domain.index;

import me.pcasaes.hexoids.core.domain.model.Barrier;

/**
 * Allows us to search for barriers using a spatial index.
 * The domain does not provide an implementation.
 * A simple implementation would return all barriers in {@link me.pcasaes.hexoids.core.domain.model.Barrier}
 */
public interface BarrierSpatialIndex {
    Iterable<Barrier> search(float x1, float y1, float x2, float y2, float distance);

    default void update(Iterable<Barrier> barriers) {

    }
}
