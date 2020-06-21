package me.pcasaes.hexoids.infrastructure.clock;

import java.lang.reflect.Method;

public final class HRClock {

    private static final Long START_TIME_SECONDS = System.currentTimeMillis() / 1000L;

    private static final Method TIME_METHOD;

    static {
        try {
            Class cls = Class.forName("jdk.internal.misc.VM");
            TIME_METHOD = cls.getMethod("getNanoTimeAdjustment", Long.TYPE);
            TIME_METHOD.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static long nanoTime() {
        try {
            return ((Number) TIME_METHOD.invoke(null, START_TIME_SECONDS)).longValue();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private HRClock() {
    }
}
