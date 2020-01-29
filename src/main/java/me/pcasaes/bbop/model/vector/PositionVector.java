package me.pcasaes.bbop.model.vector;

import me.pcasaes.bbop.model.Config;
import me.pcasaes.bbop.util.TrigUtil;

import java.util.OptionalDouble;
import java.util.function.DoubleUnaryOperator;

public class PositionVector {

    private final Configuration configuration;
    private final Float maxMagnitude;

    private Vector2 vector;

    private float currentX;
    private float currentY;
    private long currentTimestamp;

    private float previousX;
    private float previousY;

    private PositionVector(Vector2 vector,
                           float startX,
                           float startY,
                           long startTime,
                           Configuration configuration) {
        this.vector = vector;
        this.currentX = this.previousX = startX;
        this.currentY = this.previousY = startY;
        this.currentTimestamp = startTime;
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
        this.vector.setAngleMagnitude(0, 0);
        this.previousX = this.currentX = x;
        this.previousY = this.currentY = y;
        this.currentTimestamp = timestamp;
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
        this.previousX = this.currentX;
        this.previousY = this.currentY;
        this.currentX = x;
        this.currentY = y;
        this.vector.setAngleMagnitude(angle, magnitude);
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

        Vector2 moveVector = Vector2.fromXY(moveX, moveY).add(this.vector);

        if (maxMagnitude != null) {
            moveVector = moveVector.absMax(maxMagnitude);
        }

        this.vector = moveVector;

        float x = getX();
        float y = getY();
        update(timestamp, 0);

        return currentX != x || currentY != y;
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

    private PositionVector update(long timestamp, float dampMagCoef) {
        if (timestamp <= this.currentTimestamp) {
            return this;
        }

        long elapsed = (timestamp - this.currentTimestamp);

        float minMove = Config.get().getMinMove();

        if (dampMagCoef < 0f && vector.getMagnitude() != 0f) {
            float mag = calculateDampenedMagnitude(dampMagCoef, elapsed);
            if (mag < minMove) {
                mag = 0f;
            }
            vector.setAngleMagnitude(
                    vector.getAngle(),
                    mag
            );
        }

        float r = vector.getMagnitude() * elapsed / 1000f;


        float mx = (float) Math.cos(vector.getAngle()) * r;
        float my = (float) Math.sin(vector.getAngle()) * r;

        if (Math.abs(mx) > minMove) {
            this.previousX = this.currentX;
            this.currentX += mx;
        }
        if (Math.abs(my) > minMove) {
            this.previousY = this.currentY;
            this.currentY += my;
        }

        if (configuration.atBounds() == Configuration.AtBoundsOptions.STOP) {
            this.vector = Vector2.fromAngleMagnitude(this.vector.getAngle(), 0f);
        } else if (configuration.atBounds() == Configuration.AtBoundsOptions.BOUNCE) {
            if (this.currentX <= 0f || this.currentX >= 1f) {
                vector = vector.invertX();
            }
            if (this.currentY <= 0f || this.currentY >= 1f) {
                vector = vector.invertY();
            }
        }
        this.currentY = configuration.atBounds().bound(this.currentY);
        this.currentX = configuration.atBounds().bound(this.currentX);

        this.currentTimestamp = timestamp;

        return this;
    }

    public float getXat(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getX();
        }
        float r = vector.getMagnitude() * (timestamp - this.currentTimestamp) / 1000f;
        float x = getX() + (float) Math.cos(vector.getAngle()) * r;
        return configuration.atBounds().bound(x);
    }

    public float getYat(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getY();
        }
        float r = vector.getMagnitude() * (timestamp - this.currentTimestamp) / 1000f;
        float y = this.getY() + (float) Math.sin(vector.getAngle()) * r;
        return configuration.atBounds().bound(y);
    }

    public float getX() {
        return this.currentX;
    }

    public float getY() {
        return this.currentY;
    }

    public long getTimestamp() {
        return currentTimestamp;
    }

    public Vector2 getVector() {
        return vector;
    }

    public Vector2 getVectorAt(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return getVector();
        }

        float dampMagCoef = Config.get().getInertiaDampenCoefficient();
        if (dampMagCoef < 0f && vector.getMagnitude() != 0f) {
            long elapsed = (timestamp - this.currentTimestamp);

            float minMove = Config.get().getMinMove();

            float mag = calculateDampenedMagnitude(dampMagCoef, elapsed);
            if (mag < minMove) {
                mag = 0f;
            }
            return Vector2.fromAngleMagnitude(
                    vector.getAngle(),
                    mag
            );
        }
        return getVector();
    }

    private float calculateDampenedMagnitude(float dampMagCoef, long elapsed) {
        return 0.999994f * (float) Math.exp(dampMagCoef * elapsed) * vector.getMagnitude();
    }

    public boolean isOutOfBounds() {
        return currentX < 0f || currentX > 1f ||
                currentY < 0f || currentY > 1f;
    }

    public boolean intersectedWith(PositionVector b, float intersectionThreshold) {
        float minx = Math.min(previousX, currentX);
        float maxx = Math.max(previousX, currentX);
        float x = b.getX();
        float y = b.getY();
        if (x - intersectionThreshold > maxx || x + intersectionThreshold < minx) {
            return false;
        }

        float miny = Math.min(previousY, currentY);
        float maxy = Math.max(previousY, currentY);
        if (y - intersectionThreshold > maxy || y + intersectionThreshold < miny) {
            return false;
        }

        float distance = TrigUtil
                .calculateShortestDistanceFromPointToLine(
                        previousX,
                        previousY,
                        currentX,
                        currentY,
                        x,
                        y);
        return distance <= intersectionThreshold;

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
