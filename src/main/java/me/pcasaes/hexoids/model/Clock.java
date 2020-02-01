package me.pcasaes.hexoids.model;

public interface Clock {

    long getTime();


    static Clock create() {
        return new Implementation();
    }

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
