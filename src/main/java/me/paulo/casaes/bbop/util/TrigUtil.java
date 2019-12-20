package me.paulo.casaes.bbop.util;

public class TrigUtil {

    public static final float PI = (float) Math.PI;

    public static final float HALF_CIRCLE_IN_RADIANS = (float) Math.PI;

    public static final float FULL_CIRCLE_IN_RADIANS = 2f * (float) Math.PI;


    private TrigUtil() {
    }

    /**
     *
     * https://stackoverflow.com/a/30887154
     *
     * @param a
     * @param b
     * @return
     */
    public static float calculateAngleDistance(float a, float b) {
        float abDiff = a - b;

        float d = Math.abs(abDiff) % FULL_CIRCLE_IN_RADIANS;
        float r = d > HALF_CIRCLE_IN_RADIANS ? FULL_CIRCLE_IN_RADIANS - d : d;


        return (abDiff >= 0 && abDiff <= HALF_CIRCLE_IN_RADIANS) ||
                (abDiff <= -HALF_CIRCLE_IN_RADIANS && abDiff >= -FULL_CIRCLE_IN_RADIANS) ? r : -r;
    }
}
