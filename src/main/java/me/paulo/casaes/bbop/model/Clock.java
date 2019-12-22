package me.paulo.casaes.bbop.model;

public interface Clock {

    long getTime();

    class Factory {

        private Factory() {
        }

        public static Clock get() {
            return Implementation.INSTANCE;
        }
    }

    class Implementation implements Clock {

        public static final Clock INSTANCE = new Implementation();


        private final long adjustment;

        private Implementation() {
            long cpuTimeMillis = System.nanoTime() / 1_000_000L;
            long systemTimeMillis = System.currentTimeMillis();
            this.adjustment = systemTimeMillis - cpuTimeMillis;
        }

        public static Clock get() {
            return INSTANCE;
        }

        @Override
        public long getTime() {
            long currentCpuTimeMillis = System.nanoTime() / 1000000L;
            return currentCpuTimeMillis + adjustment;
        }
    }

}
