package me.pcasaes.hexoids.core.domain.metrics;

import pcasaes.hexoids.proto.ClientPlatforms;

import java.util.Arrays;

public class GameMetric {

    private final String name;

    private long total = 0L;

    private final long[] totalByClientPlatform;

    private GameMetric(String name) {
        this.name = name;
        totalByClientPlatform = Arrays
                .stream(ClientPlatforms.values())
                .mapToLong(a -> 0L)
                .toArray();
    }

    static GameMetric of(String name) {
        return new GameMetric(name);
    }

    public void increment(ClientPlatforms clientPlatform) {
        total++;
        if (clientPlatform == null) {
            totalByClientPlatform[ClientPlatforms.UNKNOWN.ordinal()]++;
        } else {
            totalByClientPlatform[clientPlatform.ordinal()]++;
        }
    }

    public long getTotal() {
        return total;
    }

    public long getTotalByClientPlatform(ClientPlatforms clientPlatform) {
        return totalByClientPlatform[clientPlatform.ordinal()];
    }

    public String getName() {
        return name;
    }
}
