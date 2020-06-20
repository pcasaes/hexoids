package me.pcasaes.hexoids.core.domain.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameMetrics {

    private static final GameMetrics INSTANCE = new GameMetrics();

    public static GameMetrics get() {
        return INSTANCE;
    }

    private final GameMetric playerDestroyed;
    private final GameMetric playerSpawned;
    private final GameMetric playerJoined;
    private final GameMetric playerLeft;
    private final GameMetric boltFired;
    private final GameMetric boltExhausted;

    private final List<GameMetric> metrics;

    private GameMetrics() {
        this.playerDestroyed = GameMetric.of("player-destroyed");
        this.playerSpawned = GameMetric.of("player-spawned");
        this.playerJoined = GameMetric.of("player-joined");
        this.playerLeft = GameMetric.of("player-left");
        this.boltFired = GameMetric.of("bolt-fired");
        this.boltExhausted = GameMetric.of("bolt-exhausted");

        List<GameMetric> list = new ArrayList<>(6);
        list.add(playerDestroyed);
        list.add(playerSpawned);
        list.add(playerJoined);
        list.add(playerLeft);
        list.add(boltFired);
        list.add(boltExhausted);

        this.metrics = new CopyOnWriteArrayList<>(list);
    }

    public List<GameMetric> getMetrics() {
        return metrics;
    }

    public GameMetric getPlayerDestroyed() {
        return playerDestroyed;
    }

    public GameMetric getBoltFired() {
        return boltFired;
    }

    public GameMetric getBoltExhausted() {
        return boltExhausted;
    }

    public GameMetric getPlayerSpawned() {
        return playerSpawned;
    }

    public GameMetric getPlayerJoined() {
        return playerJoined;
    }

    public GameMetric getPlayerLeft() {
        return playerLeft;
    }


}
