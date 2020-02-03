package me.pcasaes.hexoids.model.vector;

import me.pcasaes.hexoids.model.Config;
import me.pcasaes.hexoids.util.TrigUtil;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

public class PositionVector {

    private final Configuration configuration;
    private final Float maxMagnitude;

    /*
    Velocity here is distance unit per second (not millis!)

    velocity is what got us from previous position to currentPosition
     */
    private final Vector2 velocity;
    private final Vector2 previousVelocity;

    private long previousTimestamp;
    private long currentTimestamp;

    private Vector2 currentPosition;
    private Vector2 previousPosition;

    private PositionVector(Vector2 velocity,
                           float startX,
                           float startY,
                           long startTime,
                           Configuration configuration) {
        this.previousVelocity = velocity;
        this.velocity = velocity;
        this.currentPosition = Vector2.fromXY(startX, startY);
        this.previousPosition = Vector2.fromXY(startX, startY);
        this.currentTimestamp = this.previousTimestamp = startTime;
        this.configuration = configuration;
        if (configuration.maxMagnitude().isPresent()) {
            this.maxMagnitude = (float) configuration.maxMagnitude().getAsDouble();
        } else {
            this.maxMagnitude = null;
        }
    }

    public static PositionVector of(
            float startX,
            float startY,
            float angle,
            float magnitude,
            long startTime,
            Configuration configuration) {
        return new PositionVector(
                Vector2.fromAngleMagnitude(angle, magnitude),
                startX,
                startY,
                startTime,
                configuration
        );
    }

    public static PositionVector of(
            float startX,
            float startY,
            float angle,
            float magnitude,
            long startTime) {
        return new PositionVector(
                Vector2.fromAngleMagnitude(angle, magnitude),
                startX,
                startY,
                startTime,
                DEFAULT_CONFIGURATION
        );
    }


    public void initialized(float x, float y, long timestamp) {
        this.velocity.setAngleMagnitude(0, 0);
        this.previousVelocity.setAngleMagnitude(0, 0);
        this.previousPosition.setXY(x, y);
        this.currentPosition.setXY(x, y);
        this.currentTimestamp = this.previousTimestamp = timestamp;
    }

    /**
     * Set position to x, y and velocity to angle and magnitude
     *
     * @param x         the x to move to
     * @param y         the y to move to
     * @param angle     the velocity's angle
     * @param magnitude the velocity's magnitude
     * @param timestamp the time to base elapsed time against. Should be "now",
     *                  or at least greater than the value used in the previous call.
     */
    public void moved(float x, float y, float angle, float magnitude, long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return;
        }
        this.previousPosition.set(this.currentPosition);
        this.currentPosition.setXY(x, y);
        this.previousVelocity.set(this.velocity);
        this.velocity.setAngleMagnitude(angle, magnitude);
        this.previousTimestamp = this.currentTimestamp;
        this.currentTimestamp = timestamp;
    }

    /**
     * Move the relative position and updates is velocity accordingly.
     *
     * @param moveX     the x to move by
     * @param moveY     the y to move by
     * @param timestamp the time to base elapsed time against. Should be "now",
     *                  or at least greater than the value used in the previous call.
     * @return true if the position/velocity was changed
     */
    public boolean moveBy(float moveX, float moveY, long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return false;
        }

        /*
        limiting min move here has to do with the minimum "resolution" of movement.
        It's not a game play limitation like maxMove/maxMagnitude. We do it cheaply
        here by comparing horizontal and vertical movements independently.
         */
        float minMove = Config.get().getMinMove();
        if (Math.abs(moveX) <= minMove &&
                Math.abs(moveY) <= minMove
        ) {
            return false;
        }

        Vector2 moveVector = Vector2.fromXY(moveX, moveY).add(this.velocity);

        if (maxMagnitude != null) {
            moveVector = moveVector.absMax(maxMagnitude);
        }

        this.previousVelocity.set(this.velocity);
        this.velocity.set(moveVector);

        float x = getX();
        float y = getY();
        update(timestamp, 0);

        return currentPosition.getX() != x || currentPosition.getY() != y;
    }

    /**
     * Updates this vector's position (x,y) based on it's velocity and elapsed time.
     *
     * @param timestamp the time to base elapsed time against. Should be "now",
     *                  or at least greater than the value used in the previous call.
     * @return
     */
    public PositionVector update(long timestamp) {
        return update(timestamp, configuration.dampenMagnitudeCoefficient());
    }

    private static Vector2 calculateDampenedVelocity(Vector2 velocity, float dampMagCoef, float minMove, long elapsed) {
        if (dampMagCoef < 0f && velocity.getMagnitude() != 0f) {
            float mag = calculateDampenedMagnitude(velocity, dampMagCoef, elapsed);
            if (mag < minMove) {
                mag = 0f;
            }
            return Vector2.fromAngleMagnitude(velocity.getAngle(), mag);
        }
        return velocity;
    }

    private static Vector2 calculateMoveDelta(Vector2 velocity, float minMove, long elapsed) {
        float velocityDelta = velocity.getMagnitude() * elapsed / 1000f;

        float mx = TrigUtil.calculateXComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta);
        float my = TrigUtil.calculateYComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta);

        boolean mxAboveMinMove = Math.abs(mx) > minMove;
        boolean myAboveMinMove = Math.abs(my) > minMove;

        if (mxAboveMinMove && myAboveMinMove) {
            return Vector2.fromXY(mx, my);
        } else if (mxAboveMinMove) {
            return Vector2.fromXY(mx, 0f);
        } else if (myAboveMinMove) {
            return Vector2.fromXY(0, my);
        }
        return Vector2.ZERO;
    }

    private PositionVector update(long timestamp, float dampMagCoef) {
        if (timestamp <= this.currentTimestamp) {
            return this;
        }

        this.previousVelocity.set(this.velocity);

        long elapsed = (timestamp - this.currentTimestamp);

        float minMove = Config.get().getMinMove();

        this.velocity.set(calculateDampenedVelocity(this.velocity, dampMagCoef, minMove, elapsed));

        this.previousPosition.set(this.currentPosition);
        Vector2 moveDelta = calculateMoveDelta(this.velocity, minMove, elapsed);
        this.currentPosition.addXY(moveDelta.getX(), moveDelta.getY());

        if (configuration.atBounds() == Configuration.AtBoundsOptions.STOP) {
            this.velocity.set(Vector2.fromAngleMagnitude(this.velocity.getAngle(), 0f));
        } else if (configuration.atBounds() == Configuration.AtBoundsOptions.BOUNCE) {
            if (this.currentPosition.getX() <= 0f || this.currentPosition.getX() >= 1f) {
                velocity.set(velocity.invertX());
            }
            if (this.currentPosition.getY() <= 0f || this.currentPosition.getY() >= 1f) {
                velocity.set(velocity.invertY());
            }
        }

        this.currentPosition.setXY(
                configuration.atBounds().bound(this.currentPosition.getX()),
                configuration.atBounds().bound(this.currentPosition.getY())
        );

        this.previousTimestamp = this.currentTimestamp;
        this.currentTimestamp = timestamp;

        return this;
    }

    public float getXat(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getX();
        }
        float r = velocity.getMagnitude() * (timestamp - this.currentTimestamp) / 1000f;
        float x = getX() + (float) Math.cos(velocity.getAngle()) * r;
        return configuration.atBounds().bound(x);
    }

    public float getYat(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getY();
        }
        float r = velocity.getMagnitude() * (timestamp - this.currentTimestamp) / 1000f;
        float y = this.getY() + (float) Math.sin(velocity.getAngle()) * r;
        return configuration.atBounds().bound(y);
    }

    public float getX() {
        return this.currentPosition.getX();
    }

    public float getY() {
        return this.currentPosition.getY();
    }

    public long getTimestamp() {
        return currentTimestamp;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public Vector2 getVectorAt(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getVelocity();
        }

        float dampMagCoef = Config.get().getInertiaDampenCoefficient();
        if (dampMagCoef < 0f && velocity.getMagnitude() != 0f) {
            long elapsed = (timestamp - this.currentTimestamp);

            float minMove = Config.get().getMinMove();

            float mag = calculateDampenedMagnitude(this.velocity, dampMagCoef, elapsed);
            if (mag < minMove) {
                mag = 0f;
            }
            return Vector2.fromAngleMagnitude(
                    velocity.getAngle(),
                    mag
            );
        }
        return getVelocity();
    }

    private static float calculateDampenedMagnitude(Vector2 velocity, float dampMagCoef, long elapsed) {
        return 0.999994f * (float) Math.exp(dampMagCoef * elapsed) * velocity.getMagnitude();
    }

    public boolean isOutOfBounds() {
        return currentPosition.getX() < 0f || currentPosition.getX() > 1f ||
                currentPosition.getY() < 0f || currentPosition.getY() > 1f;
    }

    private static boolean detectCollision(Vector2 aPos, Vector2 aVel,
                                           Vector2 bPos, Vector2 bVel,
                                           float intersectionThreshold,
                                           float collisionTimeInMillis) {
        Vector2 relativePosition = bPos.minus(aPos);
        Vector2 relativeVelocity = bVel.minus(aVel);

        float timeToCollisionInSeconds = relativePosition.dot(relativeVelocity) /
                (relativeVelocity.getMagnitude() * relativeVelocity.getMagnitude() * -1f);

        if (timeToCollisionInSeconds * 1000f > collisionTimeInMillis) {
            return false;
        }

        float minSeparation = relativePosition.getMagnitude() - relativeVelocity.getMagnitude() * timeToCollisionInSeconds;
        return minSeparation <= intersectionThreshold;
    }

    private boolean intersectedWithSegment(PositionVector b, float intersectionThreshold) {
        float minMove = Config.get().getMinMove();

        // https://gamedev.stackexchange.com/questions/125011/given-the-position-and-velocity-of-an-object-how-can-i-detect-possible-collision
        Vector2 bAdjustedPreviousPosition;
        if (b.previousTimestamp != this.previousTimestamp) {
            long timeDifference;
            Vector2 vel;
            if (b.previousTimestamp < this.previousTimestamp) {
                timeDifference = this.previousTimestamp - b.previousTimestamp;
                vel = b.velocity;
            } else {
                timeDifference = b.previousTimestamp - this.previousTimestamp;
                vel = b.previousVelocity.invert();
            }

            Vector2 moveDelta = calculateMoveDelta(vel, minMove, timeDifference);
            bAdjustedPreviousPosition = b.previousPosition.add(moveDelta);

        } else {
            bAdjustedPreviousPosition = b.previousPosition;
        }

        if (detectCollision(
                previousPosition,
                velocity,
                bAdjustedPreviousPosition,
                b.velocity,
                intersectionThreshold,
                Config.get().getUpdateFrequencyInMillisWithAdded20Percent())) {
            return true;
        }

        if (b.currentTimestamp < this.currentTimestamp) {
            Vector2 bAdjustedCurrentPosition;
            Vector2 bAdjustedVelocity;

            long timeDifference;
            timeDifference = this.currentTimestamp - b.currentTimestamp;

            bAdjustedVelocity = calculateDampenedVelocity(
                    b.velocity,
                    b.configuration.dampenMagnitudeCoefficient(),
                    minMove,
                    timeDifference);

            Vector2 moveDelta = calculateMoveDelta(bAdjustedVelocity, minMove, timeDifference);
            bAdjustedCurrentPosition = b.currentPosition.add(moveDelta);

            return detectCollision(
                    currentPosition,
                    velocity,
                    bAdjustedCurrentPosition,
                    bAdjustedVelocity,
                    intersectionThreshold,
                    timeDifference + 10f);

        } else {
            return false;
        }
    }

    public boolean intersectedWith(PositionVector b, float intersectionThreshold) {
        return intersectedWithSegment(b, intersectionThreshold);
    }

    public interface Configuration {

        enum AtBoundsOptions {
            IGNORE(v -> v),
            STOP(v -> Math.min(1f, Math.max(0f, v))),
            BOUNCE(v -> {
                if (v < 0f) {
                    return -v;
                } else if (v > 1f) {
                    return 1f - (v - 1f);
                }
                return v;
            });

            final DoubleUnaryOperator operator;

            AtBoundsOptions(DoubleUnaryOperator operator) {
                this.operator = operator;
            }

            float bound(float v) {
                return (float) operator.applyAsDouble(v);
            }
        }

        default AtBoundsOptions atBounds() {
            return AtBoundsOptions.IGNORE;
        }

        default OptionalDouble maxMagnitude() {
            return OptionalDouble.empty();
        }

        /**
         * Muse be a negative value. Values below -0.2 are equivalent to immediate dampening (no inertia).
         * Values above negative will disable dampening (infinite inertia).
         * <p>
         * Dampening is the current magnitude scaled by following function:
         * <p>
         * f(t) = 0.999994 * e ^ (c * t)
         * <p>
         * t => time in millis
         * c => dampening magnitude coefficient
         *
         * @return
         */
        default float dampenMagnitudeCoefficient() {
            return 0f;
        }
    }

    public static final Configuration DEFAULT_CONFIGURATION = new Configuration() {
    };

}
