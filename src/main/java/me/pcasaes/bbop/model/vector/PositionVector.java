package me.pcasaes.bbop.model.vector;

import me.pcasaes.bbop.model.Config;
import me.pcasaes.bbop.util.TrigUtil;

import java.util.OptionalDouble;

public class PositionVector {

    private final Configuration configuration;
    private final Float maxMagnitude;
    private final Float negMaxMagnitude;

    private final Vector2 vector;

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
            this.negMaxMagnitude = -this.maxMagnitude;
        } else {
            this.maxMagnitude = null;
            this.negMaxMagnitude = null;
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

        float minMove = Config.get().getMinMove();
        if (Math.abs(moveX) <= minMove &&
                Math.abs(moveY) <= minMove
        ) {
            return false;
        }

        if (maxMagnitude != null) {
            moveX = Math.max(negMaxMagnitude, Math.min(moveX, maxMagnitude));
            moveY = Math.max(negMaxMagnitude, Math.min(moveY, maxMagnitude));
        }

        float x = this.currentX + moveX;
        float y = this.currentY + moveY;

        if (configuration.isBounded()) {
            x = Math.max(0f, Math.min(1f, x));
            y = Math.max(0f, Math.min(1f, y));
        }

        if (this.currentX == x && this.currentY == y) {
            return false;
        }

        float angle = TrigUtil.calculateAngleBetweenTwoPoints(this.currentX, this.currentY, x, y);

        float diffX = x - this.currentX;
        float diffY = y - this.currentY;

        float elapsed = (timestamp - this.currentTimestamp) / 1000f; // in seconds
        float magnitude = (float) Math.sqrt(diffX * diffX + diffY * diffY) / elapsed;
        this.vector.setAngleMagnitude(angle, magnitude);
        this.previousX = this.currentX;
        this.previousY = this.currentY;
        this.currentX = x;
        this.currentY = y;
        this.currentTimestamp = timestamp;

        return true;
    }

    /**
     * Updates this vector's position (x,y) based on it's velocity and elapsed time.
     *
     * @param timestamp the time to base elapsed time against. Should be "now",
     *                  or at least greater than the value used in the previous call.
     * @return
     */
    public PositionVector update(long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return this;
        }
        long elapsed = timestamp - this.currentTimestamp;
        float r = vector.getMagnitude() * elapsed / 1000f;


        float mx = (float) Math.cos(vector.getAngle()) * r;
        float my = (float) Math.sin(vector.getAngle()) * r;

        float minMove = Config.get().getMinMove();
        if (Math.abs(mx) > minMove) {
            this.previousX = this.currentX;
            this.currentX += mx;
        }
        if (Math.abs(my) > minMove) {
            this.previousY = this.currentY;
            this.currentY += my;
        }

        this.currentTimestamp = timestamp;

        return this;
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
        default boolean isBounded() {
            return false;
        }

        default OptionalDouble maxMagnitude() {
            return OptionalDouble.empty();
        }
    }

    public static final Configuration DEFAULT_CONFIGURATION = new Configuration() {
    };

}
