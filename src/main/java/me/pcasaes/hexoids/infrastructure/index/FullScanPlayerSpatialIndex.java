package me.pcasaes.hexoids.infrastructure.index;

import io.quarkus.arc.DefaultBean;
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex;
import me.pcasaes.hexoids.core.domain.model.Game;
import me.pcasaes.hexoids.core.domain.model.Player;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.logging.Logger;

@DefaultBean
@ApplicationScoped
public class FullScanPlayerSpatialIndex implements PlayerSpatialIndex {

    private static final Logger LOGGER = Logger.getLogger(FullScanPlayerSpatialIndex.class.getName());

    @Inject
    public FullScanPlayerSpatialIndex() {
        LOGGER.info("Using Full Scan Spatial Index");
    }

    @Override
    public Iterable<Player> search(float x1, float y1, float x2, float y2, float distance) {
        return Game.get().getPlayers();
    }
}
