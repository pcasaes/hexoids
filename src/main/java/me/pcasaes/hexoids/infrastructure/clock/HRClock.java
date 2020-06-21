package me.pcasaes.hexoids.infrastructure.clock;

public final class HRClock {

    private static final Long START_TIME_SECONDS = System.currentTimeMillis() / 1000L;

    public static long nanoTime() {
        return System.nanoTime();
    }

    private HRClock() {
    }
}
