package me.paulo.casaes.bbop.model;

public class Config {

    public enum Environment {
        PRODUCTION,
        DEV
    }

    private Environment env;
    private int maxBolts;


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
}
