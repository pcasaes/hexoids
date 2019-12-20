package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.util.TrigUtil;

public class Config {

    public enum Environment {
        PRODUCTION,
        DEV
    }

    private Environment env;
    private float minMove;
    private float playerMaxMove;
    private float playerMaxAngle;
    private int maxBolts;
    private long boltMaxDuration;
    private float boltSpeed;
    private float boltCollisionRadius;


    private static class ConfigHolder {
        static final Config INSTANCE = new Config();
    }

    public static Config get() {
        return ConfigHolder.INSTANCE;
    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = Environment.valueOf(env);
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

}
