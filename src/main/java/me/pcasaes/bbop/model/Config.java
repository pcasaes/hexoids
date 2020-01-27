package me.pcasaes.bbop.model;

import me.pcasaes.bbop.util.TrigUtil;

public class Config {

    private long inertiaDampenTimeMillis;
    private float minMove;
    private float playerMaxMove;
    private float playerMaxAngle;
    private String playerResetPosition;
    private int maxBolts;
    private long boltMaxDuration;
    private float boltSpeed;
    private float boltCollisionRadius;
    private boolean boltInertiaEnabled;
    private float boltInertiaRejectionMax;
    private float boltInertiaProjectionMax;
    private long expungeSinceLastSpawnTimeout;


    private static class ConfigHolder {
        static final Config INSTANCE = new Config();
    }

    public static Config get() {
        return ConfigHolder.INSTANCE;
    }

    public long getInertiaDampenTimeMillis() {
        return inertiaDampenTimeMillis;
    }

    public void setInertiaDampenTimeMillis(long inertiaDampenTimeMillis) {
        this.inertiaDampenTimeMillis = inertiaDampenTimeMillis;
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

    public float getBoltInertiaRejectionMax() {
        return boltInertiaRejectionMax;
    }

    public void setBoltInertiaRejectionMax(float boltInertiaRejectionMax) {
        this.boltInertiaRejectionMax = boltInertiaRejectionMax;
    }

    public float getBoltInertiaProjectionMax() {
        return boltInertiaProjectionMax;
    }

    public void setBoltInertiaProjectionMax(float boltInertiaProjectionMax) {
        this.boltInertiaProjectionMax = boltInertiaProjectionMax;
    }

    public long getExpungeSinceLastSpawnTimeout() {
        return expungeSinceLastSpawnTimeout;
    }

    public void setExpungeSinceLastSpawnTimeout(long expungeSinceLastSpawnTimeout) {
        this.expungeSinceLastSpawnTimeout = expungeSinceLastSpawnTimeout;
    }
}
