package me.paulo.casaes.bbop.util;

public class TrigUtil {

    public static final float PI = (float) Math.PI;

    public static final float HALF_CIRCLE_IN_RADIANS = (float) Math.PI;

    public static final float FULL_CIRCLE_IN_RADIANS = 2f * (float) Math.PI;


    private TrigUtil() {
    }

    /**
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

    /**
     * https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
     *
     * (x1, y1) -> (x2, y2) => two point in a line
     * (x, y) => point from which to get distance to the line
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param x
     * @param y
     * @return distance
     */
    public static float calculateShortestDistanceFromPointToLine(float x1, float y1,
                                                                 float x2, float y2,
                                                                 float x, float y) {
        return Math.abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) /
                (float) Math.sqrt(Math.pow(y2 - y1, 2.) + Math.pow(x2 - x1, 2.));
    }
}
