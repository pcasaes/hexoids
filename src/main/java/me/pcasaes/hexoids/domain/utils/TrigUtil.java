package me.pcasaes.hexoids.domain.utils;

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

    public static float limitRotation(float currentAngle, float nextAngle, float maxAngleDelta) {

        /*
        Optimization of TrigUtil#calculateAngleDistance

        if the abs of the angle difference is is less that max angle delta than it's good enough to
        answer with nextAngle
         */
        float abDiff = nextAngle - currentAngle;
        float abDiffAbs = Math.abs(abDiff);
        if (abDiffAbs <= maxAngleDelta) {
            return nextAngle;
        }

        float d = abDiffAbs % FULL_CIRCLE_IN_RADIANS;
        float r = d > HALF_CIRCLE_IN_RADIANS ? FULL_CIRCLE_IN_RADIANS - d : d;


        float aDiff1 = (abDiff >= 0 && abDiff <= HALF_CIRCLE_IN_RADIANS) ||
                (abDiff <= -HALF_CIRCLE_IN_RADIANS && abDiff >= -FULL_CIRCLE_IN_RADIANS) ? r : -r;


        if (aDiff1 > maxAngleDelta) {
            aDiff1 = maxAngleDelta;
            return currentAngle + aDiff1;
        } else if (aDiff1 < -maxAngleDelta) {
            aDiff1 = -maxAngleDelta;
            return currentAngle + aDiff1;
        } else {
            return nextAngle;
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
     * <p>
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

    /**
     * Calculates the angle in radians between two points.
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static float calculateAngleBetweenTwoPoints(float x1, float y1,
                                                       float x2, float y2) {
        return (float) Math.atan2(y2 - y1, x2 - x1);
    }

    public static float calculateAngleFromComponents(float x, float y) {
        return (float) Math.atan2(y, x);
    }

    public static float calculateMagnitudeFromComponents(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static float calculateXComponentFromAngleAndMagnitude(float angle, float speed) {
        return speed * (float) Math.cos(angle);
    }

    public static float calculateYComponentFromAngleAndMagnitude(float angle, float speed) {
        return speed * (float) Math.sin(angle);
    }
}
