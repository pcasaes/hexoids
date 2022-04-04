package me.pcasaes.hexoids.core.domain.vector;

import me.pcasaes.hexoids.core.domain.utils.TrigUtil;

import java.util.Objects;

public class Vector2 {

    public static final Vector2 ZERO = new Vector2(0, 0, false, 0, 0, true) {

        @Override
        void setAngleMagnitude(float angle, float magnitude) {
        }

        @Override
        void setXY(float x, float y) {
        }

        @Override
        void addXY(float x, float y) {
        }

        @Override
        void set(Vector2 vector) {
        }
    };

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
        if (this != vector) {
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
        return add(b.getX(), b.getY());
    }

    public Vector2 add(float x, float y) {
        return fromXY(
                getX() + x,
                getY() + y
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

    public float magnitudeFrom(Vector2 b) {
        Vector2 diff = this.minus(b);
        return (float) Math.sqrt(Math.pow(diff.getX(), 2.0) + Math.pow(diff.getY(), 2.0));
    }

    public Vector2 projection(Vector2 b) {
        return b.scale((b.dot(this)) / (b.dot(b)));
    }

    public Vector2 rejection(Vector2 b) {
        return this.minus(projection(b));
    }

    public Vector2 invert() {
        if (getX() == 0f && getY() == 0f) {
            return this;
        }
        return fromXY(
                -getX(),
                -getY()
        );
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

    public Vector2 reflect(Vector2 normal) {
        return normal.scale(-2 * this.dot(normal)).add(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vector2)) return false;
        Vector2 vector2 = (Vector2) o;
        return Float.compare(vector2.getX(), getX()) == 0 &&
                Float.compare(vector2.getY(), getY()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX(), getY());
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

    /**
     * https://stackoverflow.com/a/1968345
     * @param a1
     * @param a2
     * @param b1
     * @param b2
     * @return
     */
    public static Vector2 intersectedWith(
            Vector2 a1, Vector2 a2,
            Vector2 b1, Vector2 b2) {

        float s1_x = a2.getX() - a1.getX();
        float s1_y = a2.getY() - a1.getY();
        float s2_x = b2.getX() - b1.getX();
        float s2_y = b2.getY() - b1.getY();

        float s = (-s1_y * (a1.getX() - b1.getX()) + s1_x * (a1.getY() - b1.getY())) / (-s2_x * s1_y + s1_x * s2_y);
        float t = (s2_x * (a1.getY() - b1.getY()) - s2_y * (a1.getX() - b1.getX())) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            return Vector2.fromXY(a1.getX() + (t * s1_x), a1.getY() + (t * s1_y));
        }

        return null;
    }

    public static Vector2 calculateMoveDelta(Vector2 velocity, float minMove, long elapsed) {
        float velocityDelta = velocity.getMagnitude() * elapsed / 1000f;

        float mx = TrigUtil.calculateXComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta);
        float my = TrigUtil.calculateYComponentFromAngleAndMagnitude(velocity.getAngle(), velocityDelta);

        boolean mxAboveMinMove = Math.abs(mx) > minMove;
        boolean myAboveMinMove = Math.abs(my) > minMove;

        if (mxAboveMinMove && myAboveMinMove) {
            return fromXY(mx, my);
        } else if (mxAboveMinMove) {
            return fromXY(mx, 0f);
        } else if (myAboveMinMove) {
            return fromXY(0, my);
        }
        return Vector2.ZERO;
    }

    @Override
    public String toString() {
        return "Vector2{" +
                "angle=" + angle +
                ", magnitude=" + magnitude +
                ", initializedAM=" + initializedAM +
                ", x=" + x +
                ", y=" + y +
                ", initializedXY=" + initializedXY +
                '}';
    }
}
