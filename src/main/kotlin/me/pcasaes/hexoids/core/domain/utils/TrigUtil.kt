package me.pcasaes.hexoids.core.domain.utils

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object TrigUtil {
    const val PI: Float = Math.PI.toFloat()

    const val HALF_CIRCLE: Float = PI

    const val QUARTER_CIRCLE: Float = PI / 2F

    const val FULL_CIRCLE: Float = 2F * PI


    /**
     * https://stackoverflow.com/a/30887154
     *
     * @param a
     * @param b
     * @return
     */
    fun angleDistance(a: Float, b: Float): Float {
        val delta = (a - b + PI) % FULL_CIRCLE - PI
        return if (delta < -PI) delta + FULL_CIRCLE else delta
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

        val delta = angleDistance(nextAngle, currentAngle)
        val clampedDelta = delta.coerceIn(-maxAngleDelta, maxAngleDelta)
        return currentAngle + clampedDelta

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
    fun shortestDistanceFromPointToLine(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x: Float, y: Float
    ): Float {
        // divide by zero guard
        return if (x1.isSame(x2) && y1.isSame(y2)) {
            Float.POSITIVE_INFINITY
        } else {
            abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1) / sqrt(
                (y2 - y1).toDouble().pow(2.0) + (x2 - x1).toDouble().pow(2.0)
            ).toFloat()
        }
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
    fun angleBetweenTwoPoints(
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        return atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()).toFloat()
    }

    fun angleFromComponents(x: Float, y: Float): Float {
        return atan2(y.toDouble(), x.toDouble()).toFloat()
    }

    fun magnitudeFromComponents(x: Float, y: Float): Float {
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    fun xComponentFromAngleAndMagnitude(angle: Float, speed: Float): Float {
        return speed * cos(angle.toDouble()).toFloat()
    }

    fun yComponentFromAngleAndMagnitude(angle: Float, speed: Float): Float {
        return speed * sin(angle.toDouble()).toFloat()
    }
}
