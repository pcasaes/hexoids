package me.pcasaes.hexoids.domain.service;

import me.pcasaes.hexoids.domain.model.Game;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.util.function.LongSupplier;

@ApplicationScoped
public class GameTimeProvider {

    @Produces
    @Singleton
    @GameTime
    public LongSupplier getGameTime() {
        return () -> Game.get().getClock().getTime();
    }

}
