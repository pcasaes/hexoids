package me.paulo.casaes.bbop.service;

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

    private final String environment;


    /**
     * Required for CDI Normal Scoped beans
     */
    ConfigurationService() {
        this.environment = null;
    }

    @Inject
    public ConfigurationService(
            @ConfigProperty(
                    name = "bbop.config.environment",
                    defaultValue = "PRODUCTION"
            ) String environment) {
        this.environment = environment;
    }

    public void startup(@Observes @Initialized(ApplicationScoped.class) Object env) {
        LOGGER.info("Eager load Configuration");
    }

    @PostConstruct
    public void start() {
        LOGGER.info("\tbbop.config.environment=" + getEnvironment());
    }


    public String getEnvironment() {
        return environment;
    }
}
