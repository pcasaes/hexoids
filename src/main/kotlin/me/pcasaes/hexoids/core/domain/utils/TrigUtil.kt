package me.pcasaes.hexoids.core.domain.utils

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object TrigUtil {
    const val PI: Float = Math.PI.toFloat()

    const val HALF_CIRCLE_IN_RADIANS: Float = Math.PI.toFloat()

    const val QUARTER_CIRCLE_IN_RADIANS: Float = Math.PI.toFloat() / 2F

    const val FULL_CIRCLE_IN_RADIANS: Float = 2F * Math.PI.toFloat()


    /**
     * https://stackoverflow.com/a/30887154
     *
     * @param a
     * @param b
     * @return
     */
    fun calculateAngleDistance(a: Float, b: Float): Float {
        val abDiff = a - b

        val d = abs(abDiff) % FULL_CIRCLE_IN_RADIANS
        val r = if (d > HALF_CIRCLE_IN_RADIANS) FULL_CIRCLE_IN_RADIANS - d else d


        return if ((abDiff >= 0 && abDiff <= HALF_CIRCLE_IN_RADIANS) ||
            (abDiff <= -HALF_CIRCLE_IN_RADIANS && abDiff >= -FULL_CIRCLE_IN_RADIANS)
        ) r else -r
    }

    fun limitRotation(currentAngle: Float, nextAngle: Float, maxAngleDelta: Float): Float {
        /*
               Optimization of TrigUtil#calculateAngleDistance
       
               if the abs of the angle difference is is less that max angle delta than it's good enough to
               answer with nextAngle
                */

        val abDiff = nextAngle - currentAngle
        val abDiffAbs = abs(abDiff)
        if (abDiffAbs <= maxAngleDelta) {
            return nextAngle
        }

        val d = abDiffAbs % FULL_CIRCLE_IN_RADIANS
        val r = if (d > HALF_CIRCLE_IN_RADIANS) FULL_CIRCLE_IN_RADIANS - d else d


        var aDiff1 = if ((abDiff >= 0 && abDiff <= HALF_CIRCLE_IN_RADIANS) ||
            (abDiff <= -HALF_CIRCLE_IN_RADIANS && abDiff >= -FULL_CIRCLE_IN_RADIANS)
        ) r else -r


        if (aDiff1 > maxAngleDelta) {
            aDiff1 = maxAngleDelta
            return currentAngle + aDiff1
        } else if (aDiff1 < -maxAngleDelta) {
            aDiff1 = -maxAngleDelta
            return currentAngle + aDiff1
        } else {
            return nextAngle
        }
    }

    /**
     * https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
     *
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
    fun calculateShortestDistanceFromPointToLine(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x: Float, y: Float
    ): Float {
        return abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) / sqrt(
            (y2 - y1).toDouble().pow(2.0) + (x2 - x1).toDouble().pow(2.0)
        ).toFloat()
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
    @JvmStatic
    fun calculateAngleBetweenTwoPoints(
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        return atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()).toFloat()
    }

    @JvmStatic
    fun calculateAngleFromComponents(x: Float, y: Float): Float {
        return atan2(y.toDouble(), x.toDouble()).toFloat()
    }

    @JvmStatic
    fun calculateMagnitudeFromComponents(x: Float, y: Float): Float {
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    @JvmStatic
    fun calculateXComponentFromAngleAndMagnitude(angle: Float, speed: Float): Float {
        return speed * cos(angle.toDouble()).toFloat()
    }

    @JvmStatic
    fun calculateYComponentFromAngleAndMagnitude(angle: Float, speed: Float): Float {
        return speed * sin(angle.toDouble()).toFloat()
    }
}
