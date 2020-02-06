package me.pcasaes.hexoids.model;

import me.pcasaes.hexoids.util.TrigUtil;

public class Config {

    private long updateFrequencyInMillis;
    private float updateFrequencyInMillisWithAdded20Percent;
    private float updateFrequencyInMillisWithSubstract10Percent;
    private float inertiaDampenCoefficient;

    /**
     * The smallest value for a valid magnitude. Should be 1/boundSizeInPixels
     */
    private float minMove;
    private int playerNameLength;
    private float playerMaxMove;
    private float playerMaxAngle;
    private String playerResetPosition;
    private int maxBolts;
    private long boltMaxDuration;
    private float boltSpeed;
    private float boltCollisionRadius;
    private boolean boltInertiaEnabled;
    private float boltInertiaRejectionScale;
    private float boltInertiaProjectionScale;
    private float boltInertiaNegativeProjectionScale;
    private long expungeSinceLastSpawnTimeout;


    private static class ConfigHolder {
        static final Config INSTANCE = new Config();
    }

    public static Config get() {
        return ConfigHolder.INSTANCE;
    }

    public long getUpdateFrequencyInMillis() {
        return updateFrequencyInMillis;
    }

    public void setUpdateFrequencyInMillis(long updateFrequencyInMillis) {
        this.updateFrequencyInMillis = updateFrequencyInMillis;
        this.updateFrequencyInMillisWithAdded20Percent = updateFrequencyInMillis * 1.2f;
        this.updateFrequencyInMillisWithSubstract10Percent = updateFrequencyInMillis * 0.9f;
    }

    public float getUpdateFrequencyInMillisWithAdded20Percent() {
        return updateFrequencyInMillisWithAdded20Percent;
    }

    public float getUpdateFrequencyInMillisWithSubstract10Percent() {
        return updateFrequencyInMillisWithSubstract10Percent;
    }

    public float getInertiaDampenCoefficient() {
        return inertiaDampenCoefficient;
    }

    public void setInertiaDampenCoefficient(float inertiaDampenCoefficient) {
        this.inertiaDampenCoefficient = inertiaDampenCoefficient;
    }

    public int getMaxBolts() {
        return maxBolts;
    }

    public float getMinMove() {
        return minMove;
    }

    public void setMinMove(float minMove) {
        this.minMove = minMove;
    }

    public float getPlayerMaxMove() {
        return playerMaxMove;
    }

    public float getPlayerMaxAngle() {
        return playerMaxAngle;
    }

    public int getPlayerNameLength() {
        return playerNameLength;
    }

    public void setPlayerNameLength(int playerNameLength) {
        this.playerNameLength = playerNameLength;
    }

    public void setPlayerMaxAngleDivisor(float playerMaxAngleDivisor) {
        this.playerMaxAngle = TrigUtil.PI / playerMaxAngleDivisor;
    }

    public void setPlayerMaxMove(float playerMaxMove) {
        this.playerMaxMove = playerMaxMove;
    }

    public String getPlayerResetPosition() {
        return playerResetPosition;
    }

    public void setPlayerResetPosition(String playerResetPosition) {
        this.playerResetPosition = playerResetPosition;
    }

    public void setMaxBolts(int maxBolts) {
        this.maxBolts = maxBolts;
    }

    public long getBoltMaxDuration() {
        return boltMaxDuration;
    }

    public void setBoltMaxDuration(long boltMaxDuration) {
        this.boltMaxDuration = boltMaxDuration;
    }

    public float getBoltSpeed() {
        return boltSpeed;
    }

    public void setBoltSpeed(float boltSpeed) {
        this.boltSpeed = boltSpeed;
    }

    public float getBoltCollisionRadius() {
        return boltCollisionRadius;
    }

    public void setBoltCollisionRadius(float boltCollisionRadius) {
        this.boltCollisionRadius = boltCollisionRadius;
    }

    public boolean isBoltInertiaEnabled() {
        return boltInertiaEnabled;
    }

    public void setBoltInertiaEnabled(boolean boltInertiaEnabled) {
        this.boltInertiaEnabled = boltInertiaEnabled;
    }

    public float getBoltInertiaRejectionScale() {
        return boltInertiaRejectionScale;
    }

    public void setBoltInertiaRejectionScale(float boltInertiaRejectionScale) {
        this.boltInertiaRejectionScale = boltInertiaRejectionScale;
    }

    public float getBoltInertiaProjectionScale() {
        return boltInertiaProjectionScale;
    }

    public float getBoltInertiaNegativeProjectionScale() {
        return boltInertiaNegativeProjectionScale;
    }

    public void setBoltInertiaNegativeProjectionScale(float boltInertiaNegativeProjectionScale) {
        this.boltInertiaNegativeProjectionScale = boltInertiaNegativeProjectionScale;
    }

    public void setBoltInertiaProjectionScale(float boltInertiaProjectionScale) {
        this.boltInertiaProjectionScale = boltInertiaProjectionScale;
    }

    public long getExpungeSinceLastSpawnTimeout() {
        return expungeSinceLastSpawnTimeout;
    }

    public void setExpungeSinceLastSpawnTimeout(long expungeSinceLastSpawnTimeout) {
        this.expungeSinceLastSpawnTimeout = expungeSinceLastSpawnTimeout;
    }
}
