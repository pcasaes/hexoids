package me.pcasaes.hexoids.core.domain.model;

/**
 * Used to get the current time in millis;
 */
public interface Clock {

    long getTime();

    long getNanos();

    static Clock create() {
        return Implementation.holder;
    }

    /**
     * This implementation uses {@link System#nanoTime()} to provide a
     * monotonic clock.
     */
    class Implementation implements Clock {

        static Implementation holder = new Implementation();

        private final long adjustment;

        private Implementation() {
            long cpuTimeMillis = System.nanoTime() / 1_000_000L;
            long systemTimeMillis = System.currentTimeMillis();
            this.adjustment = systemTimeMillis - cpuTimeMillis;
        }

        @Override
        public long getTime() {
            long currentCpuTimeMillis = System.nanoTime() / 1000000L;
            return currentCpuTimeMillis + adjustment;
        }

        @Override
        public long getNanos() {
            return System.nanoTime() % 1000000L;
        }
    }

}
