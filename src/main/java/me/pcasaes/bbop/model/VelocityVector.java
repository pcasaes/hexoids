package me.pcasaes.bbop.model;

import me.pcasaes.bbop.util.TrigUtil;

public class VelocityVector {

    private final float startX;
    private final float startY;
    private final float angle;
    private final float speed;
    private final long startTimestamp;

    private float previousX;
    private float previousY;
    private float currentX;
    private float currentY;

    private VelocityVector(float startX,
                           float startY,
                           float angle,
                           float speed,
                           long startTimestamp) {
        this.startX = this.currentX = this.previousX = startX;
        this.startY = this.currentY = this.previousY = startY;
        this.angle = angle;
        this.speed = speed;
        this.startTimestamp = startTimestamp;
    }

    public static VelocityVector of(float x,
                                    float y,
                                    float angle,
                                    float speed,
                                    long startTimestamp) {
        return new VelocityVector(x, y, angle, speed, startTimestamp);
    }

    VelocityVector update(long timestamp) {
        long elapsed = timestamp - this.startTimestamp;
        float r = speed * elapsed / 1000f;


        float mx = (float) Math.cos(angle) * r;
        float my = (float) Math.sin(angle) * r;

        float minMove = Config.get().getMinMove();
        if (Math.abs(mx) > minMove) {
            this.previousX = this.currentX;
            this.currentX = startX + mx;
        }
        if (Math.abs(my) > minMove) {
            this.previousY = this.currentY;
            this.currentY = startY + my;
        }


        return this;
    }

    public float getX() {
        return this.currentX;
    }

    public float getY() {
        return this.currentY;
    }

    public float getAngle() {
        return angle;
    }

    boolean isOutOfBounds() {
        return currentX < 0f || currentX > 1f ||
                currentY < 0f || currentY > 1f;
    }

    boolean intersectedWith(float x, float y, float intersectionThreshold) {
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
