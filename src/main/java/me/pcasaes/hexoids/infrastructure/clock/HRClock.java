package me.pcasaes.hexoids.infrastructure.clock;

public final class HRClock {

    public static long nanoTime() {
        return System.nanoTime();
    }

    private HRClock() {
    }
}
