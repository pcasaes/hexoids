package me.pcasaes.hexoids.core.domain.vector

import me.pcasaes.hexoids.core.domain.config.Config.Companion.get
import me.pcasaes.hexoids.core.domain.utils.TrigUtil.calculateAngleBetweenTwoPoints
import me.pcasaes.hexoids.core.domain.vector.PositionVector.Configuration.AtBoundsOptions
import java.util.OptionalDouble
import java.util.function.DoubleUnaryOperator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class PositionVector private constructor(
    private val previousVelocity: Vector2,
    startX: Float,
    startY: Float,
    startTime: Long,
    configuration: Configuration
) {
    private val configuration: Configuration
    private val maxMagnitude: Float?

    /*
    Velocity here is distance unit per second (not millis!)

    velocity is what got us from previous position to currentPosition
     */
    private val velocity: Vector2
    private val scheduledMove: Vector2 = Vector2.fromXY(0F, 0F)

    private var previousTimestamp: Long
    private var currentTimestamp: Long

    private val currentPosition: Vector2
    private val previousPosition: Vector2

    private var movedByScheduledMove = false

    fun initialized(x: Float, y: Float, angle: Float, magnitude: Float, timestamp: Long) {
        initialized(x, y, timestamp)
        this.velocity.setAngleMagnitude(angle, magnitude)
        this.previousVelocity.setAngleMagnitude(angle, magnitude)
    }

    fun initialized(x: Float, y: Float, timestamp: Long) {
        this.velocity.setAngleMagnitude(0F, 0F)
        this.previousVelocity.setAngleMagnitude(0F, 0F)
        this.previousPosition.setXY(x, y)
        this.currentPosition.setXY(x, y)
        this.previousTimestamp = timestamp
        this.currentTimestamp = this.previousTimestamp
    }

    fun teleport(x: Float, y: Float, timestamp: Long) {
        this.previousPosition.setXY(x, y)
        this.currentPosition.setXY(x, y)
        this.previousTimestamp = timestamp
        this.currentTimestamp = this.previousTimestamp
    }

    fun reflect(at: Vector2, normal: Vector2, bodySize: Float, dampen: Float) {
        val bodyVector = Vector2.fromAngleMagnitude(velocity.getAngle(), bodySize)
        val atWithBody = at.minus(bodyVector)

        this.velocity.set(velocity.reflect(normal).scale(dampen))


        val reflectMag = currentPosition.minus(atWithBody).getMagnitude()

        val reflectMagWithVelAngle = Vector2.fromAngleMagnitude(velocity.getAngle(), reflectMag).absMax(bodySize)

        this.currentPosition.set(atWithBody.add(reflectMagWithVelAngle))
    }

    fun noMovement(): Boolean {
        return this.previousPosition == this.currentPosition
    }

    /**
     * Set position to x, y and velocity to angle and magnitude
     *
     * @param x         the x to move to
     * @param y         the y to move to
     * @param angle     the velocity's angle
     * @param magnitude the velocity's magnitude
     * @param timestamp the time to base elapsed time against. Should be "now",
     * or at least greater than the value used in the previous call.
     */
    fun moved(x: Float, y: Float, angle: Float, magnitude: Float, timestamp: Long) {
        if (timestamp <= this.currentTimestamp) {
            return
        }
        this.previousPosition.set(this.currentPosition)
        this.currentPosition.setXY(x, y)
        this.previousVelocity.set(this.velocity)
        this.velocity.setAngleMagnitude(angle, magnitude)
        this.previousTimestamp = this.currentTimestamp
        this.currentTimestamp = timestamp
    }

    /**
     * Move the relative position and updates its velocity accordingly.
     *
     * @param moveX     the x to move by
     * @param moveY     the y to move by
     * @param timestamp the time to base elapsed time against. Should be "now",
     * or at least greater than the value used in the previous call.
     * @return true if the position/velocity was changed
     */
    fun moveBy(moveX: Float, moveY: Float, timestamp: Long): Boolean {
        if (timestamp <= this.currentTimestamp) {
            return false
        }

        /*
        limiting min move here has to do with the minimum "resolution" of movement.
        It's not a game play limitation like maxMove/maxMagnitude. We do it cheaply
        here by comparing horizontal and vertical movements independently.
         */
        val minMove = get().getMinMove()
        if (abs(moveX) <= minMove &&
            abs(moveY) <= minMove
        ) {
            return false
        }

        var moveVector = Vector2.fromXY(moveX, moveY).add(this.velocity)

        if (maxMagnitude != null) {
            moveVector = moveVector.absMax(maxMagnitude)
        }

        this.previousVelocity.set(this.velocity)
        this.velocity.set(moveVector)

        val x = getX()
        val y = getY()
        update(timestamp, 0F)

        return currentPosition.getX() != x || currentPosition.getY() != y
    }

    fun scheduleMove(moveX: Float, moveY: Float) {
        val minMove = get().getMinMove()
        if (abs(moveX) <= minMove &&
            abs(moveY) <= minMove
        ) {
            return
        }

        this.scheduledMove.addXY(moveX, moveY)
    }

    /**
     * Updates this vector's position (x,y) based on it's velocity and elapsed time.
     *
     * @param timestamp the time to base elapsed time against. Should be "now",
     * or at least greater than the value used in the previous call.
     * @return
     */
    fun update(timestamp: Long): PositionVector {
        return update(timestamp, configuration.dampenMagnitudeCoefficient())
    }

    private fun update(timestamp: Long, dampMagCoef: Float): PositionVector {
        if (timestamp <= this.currentTimestamp) {
            return this
        }

        this.previousVelocity.set(this.velocity)

        val elapsed = (timestamp - this.currentTimestamp)

        val minMove = get().getMinMove()

        this.velocity.set(calculateDampenedVelocity(this.velocity, dampMagCoef, minMove, elapsed))

        if (hasScheduledMove()) {
            var moveVector = scheduledMove.add(this.velocity)

            if (maxMagnitude != null) {
                moveVector = moveVector.absMax(maxMagnitude)
            }

            this.velocity.set(moveVector)
            scheduledMove.setXY(0F, 0F)
            this.movedByScheduledMove = true
        } else {
            this.movedByScheduledMove = false
        }

        this.previousPosition.set(this.currentPosition)
        val moveDelta = Vector2.calculateMoveDelta(this.velocity, minMove, elapsed)
        this.currentPosition.addXY(moveDelta.getX(), moveDelta.getY())

        if (configuration.atBounds() == AtBoundsOptions.STOP) {
            this.velocity.set(Vector2.fromAngleMagnitude(this.velocity.getAngle(), 0F))
        } else if (configuration.atBounds() == AtBoundsOptions.BOUNCE) {
            if (this.currentPosition.getX() <= 0F || this.currentPosition.getX() >= 1F) {
                velocity.set(velocity.invertX())
            }
            if (this.currentPosition.getY() <= 0F || this.currentPosition.getY() >= 1F) {
                velocity.set(velocity.invertY())
            }
        }

        this.currentPosition.setXY(
            configuration.atBounds().bound(this.currentPosition.getX()),
            configuration.atBounds().bound(this.currentPosition.getY())
        )

        this.previousTimestamp = this.currentTimestamp
        this.currentTimestamp = timestamp

        return this
    }

    fun getXat(timestamp: Long): Float {
        if (timestamp <= this.currentTimestamp) {
            return getX()
        }
        val r = velocity.getMagnitude() * (timestamp - this.currentTimestamp) / 1000F
        val x = getX() + cos(velocity.getAngle().toDouble()).toFloat() * r
        return configuration.atBounds().bound(x)
    }

    fun getYat(timestamp: Long): Float {
        if (timestamp <= this.currentTimestamp) {
            return getY()
        }
        val r = velocity.getMagnitude() * (timestamp - this.currentTimestamp) / 1000F
        val y = this.getY() + sin(velocity.getAngle().toDouble()).toFloat() * r
        return configuration.atBounds().bound(y)
    }

    fun getX(): Float {
        return this.currentPosition.getX()
    }

    fun getY(): Float {
        return this.currentPosition.getY()
    }

    fun getPreviousX(): Float {
        return this.previousPosition.getX()
    }

    fun getPreviousY(): Float {
        return this.previousPosition.getY()
    }

    fun getTimestamp(): Long {
        return currentTimestamp
    }

    fun getVelocity(): Vector2 {
        return velocity
    }

    fun getVectorAt(timestamp: Long): Vector2 {
        if (timestamp <= this.currentTimestamp) {
            return getVelocity()
        }

        val dampMagCoef = get().getInertiaDampenCoefficient()
        if (dampMagCoef < 0F && velocity.getMagnitude() != 0F) {
            val elapsed = (timestamp - this.currentTimestamp)

            val minMove = get().getMinMove()

            var mag: Float = calculateDampenedMagnitude(this.velocity, dampMagCoef, elapsed)
            if (mag < minMove) {
                mag = 0F
            }
            return Vector2.fromAngleMagnitude(
                velocity.getAngle(),
                mag
            )
        }
        return getVelocity()
    }

    fun isOutOfBounds(): Boolean =
        currentPosition.getX() < 0F || currentPosition.getX() > 1F || currentPosition.getY() < 0F || currentPosition.getY() > 1F

    private fun intersectedWithSegment(b: PositionVector, intersectionThreshold: Float): Boolean {
        val minMove = get().getMinMove()

        // https://gamedev.stackexchange.com/questions/125011/given-the-position-and-velocity-of-an-object-how-can-i-detect-possible-collision
        if (b.currentTimestamp > previousTimestamp) {
            val bAdjustedPreviousPosition: Vector2
            val timeUntilCollision: Float
            if (b.previousTimestamp != this.previousTimestamp) {
                val timeDifference: Long
                val vel: Vector2
                if (b.previousTimestamp < this.previousTimestamp) {
                    timeDifference = this.previousTimestamp - b.previousTimestamp
                    vel = b.velocity
                } else {
                    timeDifference = b.previousTimestamp - this.previousTimestamp
                    vel = b.previousVelocity.invert()
                }

                val moveDelta = Vector2.calculateMoveDelta(vel, minMove, timeDifference)
                bAdjustedPreviousPosition = b.previousPosition.add(moveDelta)


                timeUntilCollision = if (currentTimestamp > b.currentTimestamp) {
                    (b.currentTimestamp - previousTimestamp) + 10F
                } else {
                    get().getUpdateFrequencyInMillisWithAdded20Percent()
                }
            } else {
                bAdjustedPreviousPosition = b.previousPosition
                timeUntilCollision = get().getUpdateFrequencyInMillisWithAdded20Percent()
            }

            if (detectCollision(
                    previousPosition,
                    velocity,
                    bAdjustedPreviousPosition,
                    b.velocity,
                    intersectionThreshold,
                    timeUntilCollision
                )
            ) {
                return true
            }
        }

        return if (b.currentTimestamp < this.currentTimestamp) {
            val bAdjustedCurrentPosition: Vector2
            val bAdjustedVelocity: Vector2
            val timeDifference: Long = this.currentTimestamp - b.currentTimestamp

            bAdjustedVelocity = calculateDampenedVelocity(
                b.velocity,
                b.configuration.dampenMagnitudeCoefficient(),
                minMove,
                timeDifference
            )

            val moveDelta = Vector2.calculateMoveDelta(bAdjustedVelocity, minMove, timeDifference)
            bAdjustedCurrentPosition = b.currentPosition.add(moveDelta)

            detectCollision(
                currentPosition,
                velocity,
                bAdjustedCurrentPosition,
                bAdjustedVelocity,
                intersectionThreshold,
                timeDifference + 10F
            )
        } else {
            false
        }
    }

    fun intersectedWith(b: PositionVector, intersectionThreshold: Float): Boolean {
        return intersectedWithSegment(b, intersectionThreshold)
    }

    fun intersectedWith(fixedFrom: Vector2, fixedTo: Vector2, intersectionThreshold: Float): Vector2? {
        val extendCurrentPosition = currentPosition.add(
            Vector2.fromAngleMagnitude(
                calculateAngleBetweenTwoPoints(
                    previousPosition.getX(), previousPosition.getY(),
                    currentPosition.getX(), currentPosition.getY()
                ),
                intersectionThreshold
            )
        )

        return Vector2.intersectedWith(previousPosition, extendCurrentPosition, fixedFrom, fixedTo)
    }

    private fun hasScheduledMove(): Boolean {
        return Vector2.ZERO != scheduledMove
    }

    /**
     * Will return true if the last call to update consumed moves from scheduleMove
     * @return
     */
    fun movedByScheduledMove(): Boolean {
        return this.movedByScheduledMove
    }


    interface Configuration {
        enum class AtBoundsOptions(val operator: DoubleUnaryOperator) {
            IGNORE(DoubleUnaryOperator { v: Double -> v }),
            STOP(DoubleUnaryOperator { v: Double -> min(1.0, max(0.0, v)) }),
            BOUNCE(DoubleUnaryOperator { v: Double ->
                if (v < 0F) {
                    return@DoubleUnaryOperator -v
                } else if (v > 1F) {
                    return@DoubleUnaryOperator 1F - (v - 1F)
                }
                v
            });

            fun bound(v: Float): Float {
                return operator.applyAsDouble(v.toDouble()).toFloat()
            }
        }

        fun atBounds(): AtBoundsOptions {
            return AtBoundsOptions.IGNORE
        }

        fun maxMagnitude(): OptionalDouble {
            return OptionalDouble.empty()
        }

        /**
         * Muse be a negative value. Values below -0.2 are equivalent to immediate dampening (no inertia).
         * Values above negative will disable dampening (infinite inertia).
         *
         *
         * Dampening is the current magnitude scaled by following function:
         *
         *
         * f(t) = 0.999994 * e ^ (c * t)
         *
         *
         * t => time in millis
         * c => dampening magnitude coefficient
         *
         * @return
         */
        fun dampenMagnitudeCoefficient(): Float {
            return 0F
        }
    }

    init {
        this.velocity = previousVelocity
        this.currentPosition = Vector2.fromXY(startX, startY)
        this.previousPosition = Vector2.fromXY(startX, startY)
        this.previousTimestamp = startTime
        this.currentTimestamp = this.previousTimestamp
        this.configuration = configuration
        if (configuration.maxMagnitude().isPresent()) {
            this.maxMagnitude = configuration.maxMagnitude().getAsDouble().toFloat()
        } else {
            this.maxMagnitude = null
        }
    }

    companion object {
        @JvmStatic
        fun of(
            startX: Float,
            startY: Float,
            angle: Float,
            magnitude: Float,
            startTime: Long,
            configuration: Configuration
        ): PositionVector {
            return PositionVector(
                Vector2.fromAngleMagnitude(angle, magnitude),
                startX,
                startY,
                startTime,
                configuration
            )
        }

        @JvmStatic
        fun of(
            startX: Float,
            startY: Float,
            angle: Float,
            magnitude: Float,
            startTime: Long
        ): PositionVector {
            return PositionVector(
                Vector2.fromAngleMagnitude(angle, magnitude),
                startX,
                startY,
                startTime,
                DEFAULT_CONFIGURATION
            )
        }

        private fun calculateDampenedVelocity(
            velocity: Vector2,
            dampMagCoef: Float,
            minMove: Float,
            elapsed: Long
        ): Vector2 {
            if (dampMagCoef < 0F && velocity.getMagnitude() != 0F) {
                var mag: Float = calculateDampenedMagnitude(velocity, dampMagCoef, elapsed)
                if (mag < minMove) {
                    mag = 0F
                }
                return Vector2.fromAngleMagnitude(velocity.getAngle(), mag)
            }
            return velocity
        }

        private fun calculateDampenedMagnitude(velocity: Vector2, dampMagCoef: Float, elapsed: Long): Float {
            return 0.999994F * exp((dampMagCoef * elapsed).toDouble()).toFloat() * velocity.getMagnitude()
        }

        private fun detectCollision(
            aPos: Vector2, aVel: Vector2,
            bPos: Vector2, bVel: Vector2,
            intersectionThreshold: Float,
            collisionTimeInMillis: Float
        ): Boolean {
            val relativePosition = bPos.minus(aPos)
            val relativeVelocity = bVel.minus(aVel)

            val timeToCollisionInSeconds = relativePosition.dot(relativeVelocity) /
                    (relativeVelocity.getMagnitude() * relativeVelocity.getMagnitude() * -1F)

            if (timeToCollisionInSeconds * 1000F > collisionTimeInMillis) {
                return false
            }

            val minSeparation =
                relativePosition.getMagnitude() - relativeVelocity.getMagnitude() * timeToCollisionInSeconds
            return minSeparation <= intersectionThreshold
        }

        @JvmField
        val DEFAULT_CONFIGURATION: Configuration = object : Configuration {
        }
    }
}
