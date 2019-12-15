package me.paulo.casaes.bbop.model;

public class Config {

    private static final float MIN_MOVE = 0.000000001f;

    public enum Environment {
        PRODUCTION,
        DEV
    }

    private Environment env;
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

    public float getMinMove() {
        return MIN_MOVE;
    }
}
