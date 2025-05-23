package me.pcasaes.hexoids.core.domain.vector

import me.pcasaes.hexoids.core.domain.utils.TrigUtil.angleFromComponents
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.magnitudeFromComponents
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.xComponentFromAngleAndMagnitude
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.yComponentFromAngleAndMagnitude
import me.pcasaes.hexoids.core.domain.utils.isSame
import me.pcasaes.hexoids.core.domain.utils.isZero
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A 2D vector class that can be represented in two ways:
 * 1. Angle and magnitude
 * 2. X and Y components
 *
 * Angle is represented in radians.
 */
class Vector2 private constructor(
    private var _angle: Float? = null,
    private var _magnitude: Float? = null,

    private var _x: Float? = null,
    private var _y: Float? = null,
) {

    init {
        val hasXY = _x != null && _y != null
        val hasAngleMagnitude = _angle != null && _magnitude != null
        require(hasXY || hasAngleMagnitude) {
            "Either angle/magnitude or x/y must be provided"
        }
    }

    // Public accessors
    var angle: Float
        get() = _angle ?: calculateAngle().also { _angle = it }
        private set(value) {
            _angle = value
        }

    var magnitude: Float
        get() = _magnitude ?: calculateMagnitude().also { _magnitude = it }
        private set(value) {
            _magnitude = value
        }

    var x: Float
        get() = _x ?: calculateX().also { _x = it }
        private set(value) {
            _x = value
        }

    var y: Float
        get() = _y ?: calculateY().also { _y = it }
        private set(value) {
            _y = value
        }

    // Lazy derivation
    private fun calculateAngle(): Float {
        val x = this._x
        val y = this._y
        require(x != null && y != null) {
            "Cannot calculate angle without x and y components"
        }
        return angleFromComponents(x, y)
    }

    private fun calculateMagnitude(): Float {
        val x = this._x
        val y = this._y
        require(x != null && y != null) {
            "Cannot calculate magnitude without x and y components"
        }
        return magnitudeFromComponents(x, y)
    }

    private fun calculateX(): Float {
        val angle = this._angle
        val magnitude = this._magnitude
        require(angle != null && magnitude != null) {
            "Cannot calculate x without angle and magnitude"
        }
        return xComponentFromAngleAndMagnitude(angle, magnitude)
    }

    private fun calculateY(): Float {
        val angle = this._angle
        val magnitude = this._magnitude
        require(angle != null && magnitude != null) {
            "Cannot calculate y without angle and magnitude"
        }
        return yComponentFromAngleAndMagnitude(angle, magnitude)
    }


    fun setAngleMagnitude(angle: Float, magnitude: Float) {
        this._angle = angle
        this._magnitude = magnitude
        this._x = null
        this._y = null
    }

    fun setXY(x: Float, y: Float) {
        this._x = x
        this._y = y
        this._angle = null
        this._magnitude = null
    }

    fun addXY(x: Float, y: Float) {
        setXY(x = this.x + x, y = this.y + y)
    }

    fun copyFrom(vector: Vector2) {
        if (this !== vector) {
            this._x = vector._x
            this._y = vector._y
            this._angle = vector._angle
            this._magnitude = vector._magnitude
        }
    }

    fun scale(scaler: Float): Vector2 {
        return fromAngleMagnitude(this.angle, this.magnitude * scaler)
    }

    /**
     * If magnitude is less then the absolute value of this vector's magnitude will
     * return a new vector with the same angle and the supplied magnitude respecting
     * this magnitude's sign.
     *
     * @param magnitude (must be positive)
     * @return
     */
    fun absMax(magnitude: Float): Vector2 {
        return if (this.magnitude < 0) {
            val negMagnitude = -magnitude
            if (negMagnitude < this.magnitude) {
                this
            } else {
                fromAngleMagnitude(this.angle, negMagnitude)
            }
        } else {
            if (magnitude > this.magnitude) {
                this
            } else {
                fromAngleMagnitude(this.angle, magnitude)
            }
        }
    }

    fun add(b: Vector2): Vector2 {
        return add(b.x, b.y)
    }

    fun add(x: Float, y: Float): Vector2 {
        return fromXY(
            this.x + x,
            this.y + y
        )
    }

    fun minus(b: Vector2): Vector2 {
        return fromXY(
            x - b.x,
            y - b.y
        )
    }

    fun dot(b: Vector2): Float {
        return b.x * x + b.y * y
    }

    fun cross(b: Vector2): Float {
        return x * b.y - y * b.x
    }

    fun magnitudeFrom(b: Vector2): Float {
        val diff = this.minus(b)
        return sqrt(diff.x.toDouble().pow(2.0) + diff.y.toDouble().pow(2.0)).toFloat()
    }

    fun projection(b: Vector2): Vector2 {
        val denom = b.dot(b)
        return if (denom.isZero()) {
            zero()
        } else {
            b.scale((b.dot(this)) / denom)
        }
    }

    fun rejection(b: Vector2): Vector2 {
        return this.minus(projection(b))
    }

    fun invert(): Vector2 {
        if (x.isZero() && y.isZero()) {
            return this
        }
        return fromXY(
            -x,
            -y
        )
    }

    fun invertX(): Vector2 {
        if (x.isZero()) {
            return this
        }
        return fromXY(-x, y)
    }

    fun invertY(): Vector2 {
        if (y.isZero()) {
            return this
        }
        return fromXY(x, -y)
    }

    fun reflect(normal: Vector2): Vector2 {
        return normal.scale(-2 * this.dot(normal)).add(this)
    }

    fun isSameXY(x: Float, y: Float): Boolean {
        return this.x.isSame(x) &&
                this.y.isSame(y)
    }

    fun isZero(): Boolean = isSameXY(0F, 0F)

    fun isNotZero(): Boolean = !isZero()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector2) return false
        return isSameXY(other.x, other.y)
    }

    override fun hashCode(): Int {
        return 31 * x.hashCode() + y.hashCode()
    }

    /**
     * Return true if both vectors have the same equivalent angle.
     *
     * @param b
     * @return
     */
    fun sameDirection(b: Vector2): Boolean {
        val cross = cross(b)
        val dot = dot(b)

        val areParallel = cross.isZero()
        val sameDirection = dot > 0F
        return areParallel && sameDirection
    }

    override fun toString(): String {
        return "Vector2(angle=$_angle, magnitude=$_magnitude, x=$_x, y=$_y)"
    }

    companion object {

        fun zero(): Vector2 {
            return fromXY(0F, 0F)
        }

        fun fromAngleMagnitude(angle: Float, magnitude: Float): Vector2 {
            return if (magnitude.isZero()) {
                zero()
            } else {
                Vector2(
                    _angle = angle,
                    _magnitude = magnitude,
                )
            }
        }

        fun fromXY(x: Float, y: Float): Vector2 {
            return Vector2(
                _x = x,
                _y = y,
            )
        }

        /**
         * https://stackoverflow.com/a/1968345
         * @param a1
         * @param a2
         * @param b1
         * @param b2
         * @return
         */
        fun intersectedWith(
            a1: Vector2, a2: Vector2,
            b1: Vector2, b2: Vector2
        ): Vector2? {
            val s1_x = a2.x - a1.x
            val s1_y = a2.y - a1.y
            val s2_x = b2.x - b1.x
            val s2_y = b2.y - b1.y

            val s =
                (-s1_y * (a1.x - b1.x) + s1_x * (a1.y - b1.y)) / (-s2_x * s1_y + s1_x * s2_y)
            val t =
                (s2_x * (a1.y - b1.y) - s2_y * (a1.x - b1.x)) / (-s2_x * s1_y + s1_x * s2_y)

            if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                return fromXY(a1.x + (t * s1_x), a1.y + (t * s1_y))
            }

            return null
        }

        fun calculateMoveDelta(velocity: Vector2, minMove: Float, elapsed: Long): Vector2 {
            val velocityDelta = velocity.magnitude * elapsed / 1000F

            val mx = xComponentFromAngleAndMagnitude(velocity.angle, velocityDelta)
            val my = yComponentFromAngleAndMagnitude(velocity.angle, velocityDelta)

            val mxAboveMinMove = abs(mx) > minMove
            val myAboveMinMove = abs(my) > minMove

            return when {
                mxAboveMinMove && myAboveMinMove -> {
                    fromXY(mx, my)
                }

                mxAboveMinMove -> {
                    fromXY(mx, 0F)
                }

                myAboveMinMove -> {
                    fromXY(0F, my)
                }

                else -> zero()
            }
        }
    }
}
