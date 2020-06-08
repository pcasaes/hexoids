package me.pcasaes.hexoids.infrastructure.providers;

import me.pcasaes.hexoids.domain.service.GameLoopService;
import me.pcasaes.hexoids.domain.service.GameTimeService;

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
