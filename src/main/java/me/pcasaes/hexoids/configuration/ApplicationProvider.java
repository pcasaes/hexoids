package me.pcasaes.hexoids.configuration;

import me.pcasaes.hexoids.core.application.commands.ApplicationCommands;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@ApplicationScoped
public class ApplicationProvider {

    private final GameQueue gameQueue;

    @Inject
    public ApplicationProvider(GameQueue gameQueue) {
        this.gameQueue = gameQueue;
    }

    @Produces
    @Singleton
    public ApplicationCommands getApplicationCommands() {
        return ApplicationCommands.create(gameQueue);
    }
}
