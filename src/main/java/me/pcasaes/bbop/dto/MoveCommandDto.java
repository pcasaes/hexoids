package me.pcasaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class MoveCommandDto {

    private final float moveX;
    private final float moveY;
    private final Float angle;
    private final Float thrustAngle;

    @JsonCreator
    public MoveCommandDto(
            @JsonProperty("moveX") float moveX,
            @JsonProperty("moveY") float moveY,
            @JsonProperty("angle") Float angle,
            @JsonProperty("thrustAngle") Float thrustAngle) {
        this.moveX = moveX;
        this.moveY = moveY;
        this.angle = angle;
        this.thrustAngle = thrustAngle;
    }

    public float getMoveX() {
        return moveX;
    }

    public float getMoveY() {
        return moveY;
    }

    public Float getAngle() {
        return angle;
    }

    public Float getThrustAngle() {
        return thrustAngle;
    }
}
