package me.pcasaes.hexoids.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import me.pcasaes.hexoids.core.domain.service.GameLoopService;
import me.pcasaes.hexoids.core.domain.service.GameTimeService;


@ApplicationScoped
public class DomainServicesProvider {

    @Produces
    @Singleton
    public GameTimeService getGameTimeService() {
        return GameTimeService.getInstance();
    }

    @Produces
    @Singleton
    public GameLoopService getGameLoopService() {
        return GameLoopService.getInstance();
    }

}
