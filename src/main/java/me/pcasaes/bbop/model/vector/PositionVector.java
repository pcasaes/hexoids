package me.pcasaes.bbop.model.vector;

import me.pcasaes.bbop.model.Config;
import me.pcasaes.bbop.util.TrigUtil;

public class PositionVector {

    private final Vector2 vector;

    private float currentX;
    private float currentY;
    private long currentTimestamp;

    private float previousX;
    private float previousY;

    private PositionVector(Vector2 vector,
                           float startX,
                           float startY,
                           long startTime) {
        this.vector = vector;
        this.currentX = this.previousX = startX;
        this.currentY = this.previousY = startY;
        this.currentTimestamp = startTime;
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
                startTime
        );
    }


    public void initialized(float x, float y, long timestamp) {
        this.vector.setAngleMagnitude(0, 0);
        this.previousX = this.currentX = x;
        this.previousY = this.currentY = y;
        this.currentTimestamp = timestamp;
    }

    /**
     * Move the position and updates is velocity accordingly.
     *
     * @param x         the x to move to
     * @param y         the y to move to
     * @param timestamp the time to base elapsed time against. Should be "now",
     *                  or at least greater than the value used in the previous call.
     */
    public void move(float x, float y, long timestamp) {
        if (timestamp <= this.currentTimestamp) {
            return;
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

    public boolean intersectedWith(float x, float y, float intersectionThreshold) {
        float minx = Math.min(previousX, currentX);
        float maxx = Math.max(previousX, currentX);
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

}
