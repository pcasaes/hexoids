package me.pcasaes.hexoids.infrastructure.index

import io.quarkus.arc.DefaultBean
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import me.pcasaes.hexoids.core.domain.index.PlayerSpatialIndex
import me.pcasaes.hexoids.core.domain.model.Game
import me.pcasaes.hexoids.core.domain.model.Player
import java.util.logging.Logger

@DefaultBean
@ApplicationScoped
class FullScanPlayerSpatialIndex @Inject constructor() : PlayerSpatialIndex {
    init {
        LOGGER.info("Using Full Scan Spatial Index")
    }

    override fun search(x1: Float, y1: Float, x2: Float, y2: Float, distance: Float): Iterable<Player> {
        return Game.get().getPlayers()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(FullScanPlayerSpatialIndex::class.java.getName())
    }
}
