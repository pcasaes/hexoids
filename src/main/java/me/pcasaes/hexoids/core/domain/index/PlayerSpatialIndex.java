package me.pcasaes.hexoids.core.domain.index;

import me.pcasaes.hexoids.core.domain.model.Player;

public interface PlayerSpatialIndex {
    Iterable<Player> search(float x1, float y1, float x2, float y2, float distance);
}
