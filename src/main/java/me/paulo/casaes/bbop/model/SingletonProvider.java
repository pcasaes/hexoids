package me.paulo.casaes.bbop.model;

import java.util.function.Supplier;

class SingletonProvider {

    private static Supplier<Clock> clock;

    private static Supplier<Players> players;

    private SingletonProvider() {
    }

    static Clock getClock() {
        if (clock == null) {
            throw new IllegalStateException("Factory ot set for " + Clock.class.getName());
        }
        return clock.get();
    }

    static void setClock(Supplier<Clock> clock) {
        SingletonProvider.clock = clock;
    }

    static Players getPlayers() {
        if (players == null) {
            throw new IllegalStateException("Factory ot set for " + Players.class.getName());
        }
        return players.get();
    }

    static void setPlayers(Supplier<Players> players) {
        SingletonProvider.players = players;
    }
}
