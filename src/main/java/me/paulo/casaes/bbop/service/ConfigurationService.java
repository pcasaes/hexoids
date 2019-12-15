package me.paulo.casaes.bbop.service;


import me.paulo.casaes.bbop.model.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.logging.Logger;

@ApplicationScoped
public class ConfigurationService {

    private static final Logger LOGGER = Logger.getLogger(ConfigurationService.class.getName());

    private String environment;
    private int maxBolts;


    /**
     * Required for CDI Normal Scoped beans
     */
    ConfigurationService() {
    }

    @Inject
    public ConfigurationService(
            @ConfigProperty(
                    name = "bbop.config.environment",
                    defaultValue = "PRODUCTION"
            ) String environment,

            @ConfigProperty(
                    name = "bbop.config.player.max.bolts",
                    defaultValue = "10"
            ) int maxBolts
    ) {
        this.environment = environment;
        this.maxBolts = maxBolts;
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object env) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {
        LOGGER.info("\tbbop.config.environment=" + getEnvironment());
        LOGGER.info("\tbbop.config.player.max.bolts=" + getMaxBolts());

        Config.get().setEnv(getEnvironment());
        Config.get().setMaxBolts(getMaxBolts());
    }


    public String getEnvironment() {
        return environment;
    }

    public int getMaxBolts() {
        return maxBolts;
    }
}
