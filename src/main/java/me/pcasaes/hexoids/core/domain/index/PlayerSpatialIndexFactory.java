package me.pcasaes.hexoids.core.domain.index;

public class PlayerSpatialIndexFactory {

    private static final PlayerSpatialIndexFactory FACTORY = new PlayerSpatialIndexFactory();

    private PlayerSpatialIndexFactory() {
    }

    public static PlayerSpatialIndexFactory factory() {
        return FACTORY;
    }

    private PlayerSpatialIndex playerSpatialIndex;

    public PlayerSpatialIndex get() {
        return playerSpatialIndex;
    }

    public void setPlayerSpatialIndex(PlayerSpatialIndex playerSpatialIndex) {
        this.playerSpatialIndex = playerSpatialIndex;
    }
}
