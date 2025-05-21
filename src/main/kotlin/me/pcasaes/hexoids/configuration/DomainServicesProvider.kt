package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import me.pcasaes.hexoids.core.domain.service.GameLoopService
import me.pcasaes.hexoids.core.domain.service.GameTimeService

@ApplicationScoped
class DomainServicesProvider {

    @Produces
    @Singleton
    fun getGameTimeService(): GameTimeService {
        return GameTimeService
    }

    @Produces
    @Singleton
    fun getGameLoopService(): GameLoopService {
        return GameLoopService
    }
}
