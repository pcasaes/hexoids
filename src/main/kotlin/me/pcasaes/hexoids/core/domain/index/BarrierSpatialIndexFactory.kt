package me.pcasaes.hexoids.core.domain.index

class BarrierSpatialIndexFactory private constructor() {

    private lateinit var barrierSpatialIndex: BarrierSpatialIndex

    fun get(): BarrierSpatialIndex {
        return barrierSpatialIndex
    }

    fun setBarrierSpatialIndex(barrierSpatialIndex: BarrierSpatialIndex) {
        this.barrierSpatialIndex = barrierSpatialIndex
    }

    companion object {
        private val FACTORY = BarrierSpatialIndexFactory()

        @JvmStatic
        fun factory(): BarrierSpatialIndexFactory {
            return FACTORY
        }
    }
}
