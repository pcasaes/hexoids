package me.pcasaes.hexoids.core.domain.metrics;

public class GameMetric {

    private final String name;

    private long total = 0L;

    private GameMetric(String name) {
        this.name = name;
    }

    static GameMetric of(String name) {
        return new GameMetric(name);
    }

    public void increment() {
        total++;
    }

    public long getTotal() {
        return total;
    }

    public String getName() {
        return name;
    }
}
