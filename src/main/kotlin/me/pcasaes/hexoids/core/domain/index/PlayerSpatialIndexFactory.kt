package me.pcasaes.hexoids.core.domain.index

object PlayerSpatialIndexFactory {

    private lateinit var playerSpatialIndex: PlayerSpatialIndex

    fun get(): PlayerSpatialIndex {
        return playerSpatialIndex
    }

    fun setPlayerSpatialIndex(playerSpatialIndex: PlayerSpatialIndex) {
        this.playerSpatialIndex = playerSpatialIndex
    }
}
