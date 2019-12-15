package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveCommandDto {

    private final float moveX;
    private final float moveY;
    private final Float angle;

    @JsonCreator
    public MoveCommandDto(
            @JsonProperty("moveX") float moveX,
            @JsonProperty("moveY") float moveY,
            @JsonProperty("angle") Float angle) {
        this.moveX = moveX;
        this.moveY = moveY;
        this.angle = angle;
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
}
