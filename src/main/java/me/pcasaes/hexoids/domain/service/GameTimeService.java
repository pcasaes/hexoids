package me.pcasaes.hexoids.domain.service;

import me.pcasaes.hexoids.domain.model.Game;

public class GameTimeService {

    private static final GameTimeService INSTANCE = new GameTimeService();

    public static GameTimeService getInstance() {
        return INSTANCE;
    }

    private GameTimeService() {
    }

    public long getTime() {
        return Game.get().getClock().getTime();
    }
}
