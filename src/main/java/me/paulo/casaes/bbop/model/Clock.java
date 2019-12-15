package me.paulo.casaes.bbop.model;

public abstract class Clock {

    public abstract long getTime();

    public static Clock get() {
        return SingletonProvider.getClock();
    }

    static {
        SingletonProvider.setClock(() -> IMPLEMENTATION.INSTANCE);
    }

    static class IMPLEMENTATION extends Clock {

        public static final Clock INSTANCE = new IMPLEMENTATION();



        private final long adjustment;

        private IMPLEMENTATION() {
            long cpuTimeMillis = System.nanoTime() / 1_000_000;
            long systemTimeMillis = System.currentTimeMillis();
            this.adjustment = systemTimeMillis - cpuTimeMillis;
        }

        public static Clock get() {
            return INSTANCE;
        }

        @Override
        public long getTime() {
            long currentCpuTimeMillis = System.nanoTime() / 1000000;
            return currentCpuTimeMillis + adjustment;
        }
    }

}
