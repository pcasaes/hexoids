package me.pcasaes.hexoids.infrastructure.clock;

import java.time.Instant;

public final class HRClock {

    private static final long START_TIME_SECONDS = System.currentTimeMillis() / 1000L;

    public static long nanoTime() {
        Instant now = Instant.now();
        return (now.getEpochSecond() - START_TIME_SECONDS) * 1_000_000_000L + now.getNano();
    }

    private HRClock() {
    }
}
