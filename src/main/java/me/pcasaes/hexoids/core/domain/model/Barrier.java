package me.pcasaes.hexoids.core.domain.model;

import me.pcasaes.hexoids.core.domain.utils.TrigUtil;
import me.pcasaes.hexoids.core.domain.vector.Vector2;
import pcasaes.hexoids.proto.BarrierDto;

public class Barrier {
    public static final float WIDTH = 0.0006F;
    public static final float HALF_WIDTH = WIDTH / 2F;
    public static final float LENGTH = 0.0032F;
    public static final float HALF_LENGTH = LENGTH / 2F;
    public static final float HALF_HYPOTENUS = (float) Math.hypot(HALF_LENGTH, HALF_WIDTH);

    private final Vector2 topLeft;
    private final Vector2 bottomRight;
    private final Vector2 centerPosition;
    private final Vector2 vector;
    private final Vector2 normal;
    private final float rotationAngle;

    private final BarrierDto dto;

    private Barrier(Vector2 centerPosition, float rotationAngle) {
        this.centerPosition = centerPosition;
        this.rotationAngle = rotationAngle;

        Vector2 fromCenterToCorner = Vector2.fromAngleMagnitude(rotationAngle, HALF_HYPOTENUS);

        this.topLeft = Vector2
                .fromXY(centerPosition.getX(), centerPosition.getY())
                .minus(fromCenterToCorner);

        this.bottomRight = Vector2
                .fromXY(centerPosition.getX(), centerPosition.getY())
                .add(fromCenterToCorner);

        this.vector = Vector2.fromAngleMagnitude(
                rotationAngle,
                LENGTH
        );

        this.normal = Vector2.fromAngleMagnitude(rotationAngle + TrigUtil.QUARTER_CIRCLE_IN_RADIANS, 1f);

        this.dto = BarrierDto.newBuilder()
                .setX(centerPosition.getX())
                .setY(centerPosition.getY())
                .setAngle(rotationAngle)
                .build();
    }

    public static Barrier place(Vector2 centerPosition, float rotationAngle) {
        return new Barrier(centerPosition, rotationAngle);
    }

    public BarrierDto toDto() {
        return dto;
    }

    public Vector2 getCenterPosition() {
        return centerPosition;
    }

    public Vector2 getTopLeft() {
        return topLeft;
    }

    public Vector2 getBottomRight() {
        return bottomRight;
    }

    public Vector2 getNormal() {
        return normal;
    }
}
