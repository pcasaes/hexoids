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
    private long boltMaxDuration;
    private float boltSpeed;
    private float boltCollisionRadius;


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
            ) int maxBolts,

            @ConfigProperty(
                    name = "bbop.config.bolt.max.duration",
                    defaultValue = "10000"
            ) long boltMaxDuration,

            @ConfigProperty(
                    name = "bbop.config.bolt.speed",
                    defaultValue = "0.07"
            ) float boltSpeed,

            @ConfigProperty(
                    name = "bbop.config.bolt.collision.radius",
                    defaultValue = "0.001"
            ) float boltCollisionRadius
    ) {
        this.environment = environment;
        this.maxBolts = maxBolts;
        this.boltMaxDuration = boltMaxDuration;
        this.boltSpeed = boltSpeed;
        this.boltCollisionRadius = boltCollisionRadius;
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object env) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {
        LOGGER.info("bbop.config.environment=" + getEnvironment());
        LOGGER.info("bbop.config.player.max.bolts=" + getMaxBolts());
        LOGGER.info("bbop.config.bolt.max.duration=" + getBoltMaxDuration());
        LOGGER.info("bbop.config.bolt.speed=" + getBoltSpeed());
        LOGGER.info("bbop.config.bolt.collision.radius=" + getBoltCollisionRadius());

        Config.get().setEnv(getEnvironment());
        Config.get().setMaxBolts(getMaxBolts());
        Config.get().setBoltMaxDuration(getBoltMaxDuration());
        Config.get().setBoltSpeed(getBoltSpeed());
        Config.get().setBoltCollisionRadius(getBoltCollisionRadius());
    }


    public String getEnvironment() {
        return environment;
    }

    public int getMaxBolts() {
        return maxBolts;
    }

    public long getBoltMaxDuration() {
        return boltMaxDuration;
    }

    public float getBoltSpeed() {
        return boltSpeed;
    }

    public float getBoltCollisionRadius() {
        return boltCollisionRadius;
    }
}
