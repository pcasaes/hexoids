package me.pcasaes.hexoids.core.domain.index

object BarrierSpatialIndexFactory {

    private lateinit var barrierSpatialIndex: BarrierSpatialIndex

    fun get(): BarrierSpatialIndex {
        return barrierSpatialIndex
    }

    fun setBarrierSpatialIndex(barrierSpatialIndex: BarrierSpatialIndex) {
        this.barrierSpatialIndex = barrierSpatialIndex
    }
}
