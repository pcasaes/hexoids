package me.pcasaes.hexoids.configuration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.pcasaes.hexoids.core.application.commands.ApplicationCommands;
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue;


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
