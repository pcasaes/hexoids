package me.paulo.casaes.bbop.model;

import io.quarkus.arc.Arc;
import me.paulo.casaes.bbop.service.ConfigurationService;

public class Config {

    public enum Environment {
        PRODUCTION,
        DEV
    }

    private ConfigurationService configurationService;

    private Environment env;


    private static class ConfigHolder {
        static final Config INSTANCE = new Config();
    }

    public static Config getConfig() {
        return ConfigHolder.INSTANCE;
    }

    public Environment getEnv() {
        if (this.env == null) {
            this.env = Environment.valueOf(getConfigurationService().getEnvironment());
        }
        return this.env;
    }

    private ConfigurationService getConfigurationService() {
        if (this.configurationService == null) {
            this.configurationService = Arc.container().instance(ConfigurationService.class).get();
        }
        return this.configurationService;
    }
}
