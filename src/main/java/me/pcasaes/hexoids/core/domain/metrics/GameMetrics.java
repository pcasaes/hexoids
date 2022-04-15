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
    private final GameMetric playerStalled;
    private final GameMetric boltFired;
    private final GameMetric boltExhausted;
    private final GameMetric movedByShockwave;
    private final GameMetric massCollapsedIntoBlackhole;
    private final GameMetric blackholeEvaporated;
    private final GameMetric movedByBlackhole;
    private final GameMetric destroyedByBlackhole;

    private final List<GameMetric> metrics;

    private GameMetrics() {
        this.playerDestroyed = GameMetric.of("player-destroyed-total");
        this.playerSpawned = GameMetric.of("player-spawned-total");
        this.playerJoined = GameMetric.of("player-joined-total");
        this.playerLeft = GameMetric.of("player-left-total");
        this.playerStalled = GameMetric.of("player-stalled-total");
        this.boltFired = GameMetric.of("bolt-fired-total");
        this.boltExhausted = GameMetric.of("bolt-exhausted-total");
        this.movedByShockwave = GameMetric.of("moved-by-shockwave-total");
        this.massCollapsedIntoBlackhole = GameMetric.of("mass-collapsed-into-blackhole-total");
        this.blackholeEvaporated = GameMetric.of("blackhole-evaporated-total");
        this.movedByBlackhole = GameMetric.of("moved-by-blackhole-total");
        this.destroyedByBlackhole = GameMetric.of("destroyed-by-blackhole-total");

        List<GameMetric> list = new ArrayList<>(12);
        list.add(playerDestroyed);
        list.add(playerSpawned);
        list.add(playerJoined);
        list.add(playerLeft);
        list.add(playerStalled);
        list.add(boltFired);
        list.add(boltExhausted);
        list.add(movedByShockwave);
        list.add(massCollapsedIntoBlackhole);
        list.add(blackholeEvaporated);
        list.add(movedByBlackhole);
        list.add(destroyedByBlackhole);

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

    public GameMetric getPlayerStalled() {
        return playerStalled;
    }

    public GameMetric getMovedByShockwave() {
        return movedByShockwave;
    }

    public GameMetric getMassCollapsedIntoBlackhole() {
        return massCollapsedIntoBlackhole;
    }

    public GameMetric getBlackholeEvaporated() {
        return blackholeEvaporated;
    }

    public GameMetric getMovedByBlackhole() {
        return movedByBlackhole;
    }

    public GameMetric getDestroyedByBlackhole() {
        return destroyedByBlackhole;
    }
}
