package me.pcasaes.hexoids.core.domain.index;

public class BarrierSpatialIndexFactory {

    private static final BarrierSpatialIndexFactory FACTORY = new BarrierSpatialIndexFactory();

    private BarrierSpatialIndexFactory() {
    }

    public static BarrierSpatialIndexFactory factory() {
        return FACTORY;
    }

    private BarrierSpatialIndex barrierSpatialIndex;

    public BarrierSpatialIndex get() {
        return barrierSpatialIndex;
    }

    public void setBarrierSpatialIndex(BarrierSpatialIndex barrierSpatialIndex) {
        this.barrierSpatialIndex = barrierSpatialIndex;
    }
}
