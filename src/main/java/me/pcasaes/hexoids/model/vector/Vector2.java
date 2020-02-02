package me.pcasaes.hexoids.model.vector;

import me.pcasaes.hexoids.util.TrigUtil;

public class Vector2 {

    private float angle;
    private float magnitude;
    private boolean initializedAM;
    private float x;
    private float y;
    private boolean initializedXY;

    public Vector2(float angle, float magnitude, boolean initializedAM, float x, float y, boolean initializedXY) {
        this.angle = angle;
        this.magnitude = magnitude;
        this.initializedAM = initializedAM;
        this.x = x;
        this.y = y;
        this.initializedXY = initializedXY;
    }

    public static Vector2 fromAngleMagnitude(float angle, float magnitude) {
        return new Vector2(angle, magnitude, true, 0, 0, false);
    }

    public static Vector2 fromXY(float x, float y) {
        return new Vector2(0, 0, false, x, y, true);
    }

    void setAngleMagnitude(float angle, float magnitude) {
        this.angle = angle;
        this.magnitude = magnitude;
        this.initializedAM = true;
        this.initializedXY = false;
    }

    void setXY(float x, float y) {
        this.x = x;
        this.y = y;
        this.initializedXY = true;
        this.initializedAM = false;
    }

    void addXY(float x, float y) {
        setXY(getX() + x, getY() + y);
    }

    void set(Vector2 vector) {
        if (vector.initializedAM && vector.initializedXY) {
            this.x = vector.x;
            this.y = vector.y;
            this.angle = vector.angle;
            this.magnitude = vector.magnitude;
            this.initializedXY = true;
            this.initializedAM = true;
        } else if (vector.initializedXY) {
            setXY(vector.x, vector.y);
        } else {
            setAngleMagnitude(vector.angle, vector.magnitude);
        }
    }

    private void lazyInitAM() {
        if (!this.initializedAM) {
            this.angle = TrigUtil.calculateAngleFromComponents(this.x, this.y);
            this.magnitude = TrigUtil.calculateMagnitudeFromComponents(this.x, this.y);
            this.initializedAM = true;
        }
    }

    private void lazyInitXY() {
        if (!this.initializedXY) {
            this.x = TrigUtil.calculateXComponentFromAngleAndMagnitude(this.angle, this.magnitude);
            this.y = TrigUtil.calculateYComponentFromAngleAndMagnitude(this.angle, this.magnitude);
            this.initializedXY = true;
        }
    }

    public float getAngle() {
        lazyInitAM();
        return angle;
    }

    public float getMagnitude() {
        lazyInitAM();
        return magnitude;
    }

    public float getX() {
        lazyInitXY();
        return x;
    }

    public float getY() {
        lazyInitXY();
        return y;
    }

    public Vector2 scale(float scaler) {
        return fromAngleMagnitude(this.getAngle(), this.getMagnitude() * scaler);
    }

    /**
     * If magnitude is less then the absolute value of this vector's magnitude will
     * return a new vector with the same angle and the supplied magnitude respecting
     * this magnitude's sign.
     *
     * @param magnitude (must be positive)
     * @return
     */
    public Vector2 absMax(float magnitude) {
        if (this.getMagnitude() < 0) {
            float negMagnitude = -magnitude;
            if (negMagnitude < this.getMagnitude()) {
                return this;
            } else {
                return fromAngleMagnitude(this.getAngle(), negMagnitude);
            }
        } else {
            if (magnitude > this.getMagnitude()) {
                return this;
            } else {
                return fromAngleMagnitude(this.getAngle(), magnitude);
            }
        }
    }

    public Vector2 add(Vector2 b) {
        return fromXY(
                getX() + b.getX(),
                getY() + b.getY()
        );
    }

    public Vector2 minus(Vector2 b) {
        return fromXY(
                getX() - b.getX(),
                getY() - b.getY()
        );
    }

    public float dot(Vector2 b) {
        return b.getX() * getX() + b.getY() * getY();
    }

    public Vector2 projection(Vector2 b) {
        return b.scale((b.dot(this)) / (b.dot(b)));
    }

    public Vector2 rejection(Vector2 b) {
        return this.minus(projection(b));
    }

    public Vector2 invertX() {
        if (getX() == 0f) {
            return this;
        }
        return fromXY(-getX(), getY());
    }

    public Vector2 invertY() {
        if (getY() == 0f) {
            return this;
        }
        return fromXY(getX(), -getY());
    }

    /**
     * Return true if both vectors have the same equivalent angle.
     * This does take magnitude sign into account.
     *
     * @param b
     * @return
     */
    public boolean sameDirection(Vector2 b) {
        boolean sameAngle = b.getAngle() == getAngle();
        boolean sameSign = Math.signum(b.getMagnitude()) == Math.signum(getMagnitude());
        return sameAngle == sameSign;
    }
}
