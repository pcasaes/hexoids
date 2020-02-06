package me.pcasaes.hexoids.model;

/**
 * Used to get the current time in millis;
 */
public interface Clock {

    long getTime();


    static Clock create() {
        return new Implementation();
    }

    /**
     * This implementation uses {@link System#nanoTime()} to provide a
     * monotonic clock.
     */
    class Implementation implements Clock {

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
    }

}
