package me.pcasaes.hexoids.core.domain.vector

import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateAngleFromComponents
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateMagnitudeFromComponents
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateXComponentFromAngleAndMagnitude
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateYComponentFromAngleAndMagnitude
import java.util.Objects
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

open class Vector2(
    private var angle: Float,
    private var magnitude: Float,
    private var initializedAM: Boolean,
    private var x: Float,
    private var y: Float,
    private var initializedXY: Boolean
) {
    open fun setAngleMagnitude(angle: Float, magnitude: Float) {
        this.angle = angle
        this.magnitude = magnitude
        this.initializedAM = true
        this.initializedXY = false
    }

    open fun setXY(x: Float, y: Float) {
        this.x = x
        this.y = y
        this.initializedXY = true
        this.initializedAM = false
    }

    open fun addXY(x: Float, y: Float) {
        setXY(x = getX() + x, y = getY() + y)
    }

    open fun set(vector: Vector2) {
        if (this !== vector) {
            if (vector.initializedAM && vector.initializedXY) {
                this.x = vector.x
                this.y = vector.y
                this.angle = vector.angle
                this.magnitude = vector.magnitude
                this.initializedXY = true
                this.initializedAM = true
            } else if (vector.initializedXY) {
                setXY(vector.x, vector.y)
            } else {
                setAngleMagnitude(vector.angle, vector.magnitude)
            }
        }
    }

    private fun lazyInitAM() {
        if (!this.initializedAM) {
            this.angle = calculateAngleFromComponents(this.x, this.y)
            this.magnitude = calculateMagnitudeFromComponents(this.x, this.y)
            this.initializedAM = true
        }
    }

    private fun lazyInitXY() {
        if (!this.initializedXY) {
            this.x = calculateXComponentFromAngleAndMagnitude(this.angle, this.magnitude)
            this.y = calculateYComponentFromAngleAndMagnitude(this.angle, this.magnitude)
            this.initializedXY = true
        }
    }

    fun getAngle(): Float {
        lazyInitAM()
        return angle
    }

    fun getMagnitude(): Float {
        lazyInitAM()
        return magnitude
    }

    fun getX(): Float {
        lazyInitXY()
        return x
    }

    fun getY(): Float {
        lazyInitXY()
        return y
    }

    fun scale(scaler: Float): Vector2 {
        return fromAngleMagnitude(this.getAngle(), this.getMagnitude() * scaler)
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
        return if (this.getMagnitude() < 0) {
            val negMagnitude = -magnitude
            if (negMagnitude < this.getMagnitude()) {
                this
            } else {
                fromAngleMagnitude(this.getAngle(), negMagnitude)
            }
        } else {
            if (magnitude > this.getMagnitude()) {
                this
            } else {
                fromAngleMagnitude(this.getAngle(), magnitude)
            }
        }
    }

    fun add(b: Vector2): Vector2 {
        return add(b.getX(), b.getY())
    }

    fun add(x: Float, y: Float): Vector2 {
        return fromXY(
            getX() + x,
            getY() + y
        )
    }

    fun minus(b: Vector2): Vector2 {
        return fromXY(
            getX() - b.getX(),
            getY() - b.getY()
        )
    }

    fun dot(b: Vector2): Float {
        return b.getX() * getX() + b.getY() * getY()
    }

    fun magnitudeFrom(b: Vector2): Float {
        val diff = this.minus(b)
        return sqrt(diff.getX().toDouble().pow(2.0) + diff.getY().toDouble().pow(2.0)).toFloat()
    }

    fun projection(b: Vector2): Vector2 {
        return b.scale((b.dot(this)) / (b.dot(b)))
    }

    fun rejection(b: Vector2): Vector2 {
        return this.minus(projection(b))
    }

    fun invert(): Vector2 {
        if (getX() == 0F && getY() == 0F) {
            return this
        }
        return fromXY(
            -getX(),
            -getY()
        )
    }

    fun invertX(): Vector2 {
        if (getX() == 0F) {
            return this
        }
        return fromXY(-getX(), getY())
    }

    fun invertY(): Vector2 {
        if (getY() == 0F) {
            return this
        }
        return fromXY(getX(), -getY())
    }

    fun reflect(normal: Vector2): Vector2 {
        return normal.scale(-2 * this.dot(normal)).add(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector2) return false
        val vector2 = other
        return vector2.getX().compareTo(getX()) == 0 &&
                vector2.getY().compareTo(getY()) == 0
    }

    override fun hashCode(): Int {
        return Objects.hash(getX(), getY())
    }

    /**
     * Return true if both vectors have the same equivalent angle.
     * This does take magnitude sign into account.
     *
     * @param b
     * @return
     */
    fun sameDirection(b: Vector2): Boolean {
        val sameAngle = b.getAngle() == getAngle()
        val sameSign = sign(b.getMagnitude()) == sign(getMagnitude())
        return sameAngle == sameSign
    }

    override fun toString(): String {
        return "Vector2{" +
                "angle=" + angle +
                ", magnitude=" + magnitude +
                ", initializedAM=" + initializedAM +
                ", x=" + x +
                ", y=" + y +
                ", initializedXY=" + initializedXY +
                '}'
    }

    companion object {
        val ZERO: Vector2 = object : Vector2(0F, 0F, false, 0F, 0F, true) {
            override fun setAngleMagnitude(angle: Float, magnitude: Float) {
                // do nothing
            }

            override fun setXY(x: Float, y: Float) {
                // do nothing
            }

            override fun addXY(x: Float, y: Float) {
                // do nothing
            }

            override fun set(vector: Vector2) {
                // do nothing
            }
        }

        @JvmStatic
        fun fromAngleMagnitude(angle: Float, magnitude: Float): Vector2 {
            return Vector2(angle, magnitude, true, 0F, 0F, false)
        }

        @JvmStatic
        fun fromXY(x: Float, y: Float): Vector2 {
            return Vector2(0F, 0F, false, x, y, true)
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
            val s1_x = a2.getX() - a1.getX()
            val s1_y = a2.getY() - a1.getY()
            val s2_x = b2.getX() - b1.getX()
            val s2_y = b2.getY() - b1.getY()

            val s =
                (-s1_y * (a1.getX() - b1.getX()) + s1_x * (a1.getY() - b1.getY())) / (-s2_x * s1_y + s1_x * s2_y)
            val t =
                (s2_x * (a1.getY() - b1.getY()) - s2_y * (a1.getX() - b1.getX())) / (-s2_x * s1_y + s1_x * s2_y)

            if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
                return fromXY(a1.getX() + (t * s1_x), a1.getY() + (t * s1_y))
            }

            return null
        }

        fun calculateMoveDelta(velocity: Vector2, minMove: Float, elapsed: Long): Vector2 {
            val velocityDelta = velocity.getMagnitude() * elapsed / 1000F

            val mx = calculateXComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta)
            val my = calculateYComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta)

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

                else -> ZERO
            }
        }
    }
}
