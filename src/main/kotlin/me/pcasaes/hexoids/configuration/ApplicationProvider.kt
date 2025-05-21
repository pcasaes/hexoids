package me.pcasaes.hexoids.configuration

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import me.pcasaes.hexoids.core.application.commands.ApplicationCommands
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue

@ApplicationScoped
class ApplicationProvider @Inject constructor(private val gameQueue: GameQueue) {


    @Singleton
    @Produces
    fun getApplicationCommands(): ApplicationCommands =
        ApplicationCommands.create(gameQueue)
}
