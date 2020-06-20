package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.core.domain.service.GameLoopService;
import me.pcasaes.hexoids.core.domain.service.GameTimeService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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
