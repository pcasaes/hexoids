package me.pcasaes.hexoids.core.domain.index

class PlayerSpatialIndexFactory private constructor() {

    private lateinit var playerSpatialIndex: PlayerSpatialIndex

    fun get(): PlayerSpatialIndex {
        return playerSpatialIndex
    }

    fun setPlayerSpatialIndex(playerSpatialIndex: PlayerSpatialIndex) {
        this.playerSpatialIndex = playerSpatialIndex
    }

    companion object {
        private val FACTORY = PlayerSpatialIndexFactory()

        @JvmStatic
        fun factory(): PlayerSpatialIndexFactory {
            return FACTORY
        }
    }
}
