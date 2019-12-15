package me.paulo.casaes.bbop.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MoveCommandDto {

    private final float moveX;
    private final float moveY;

    @JsonCreator
    public MoveCommandDto(
            @JsonProperty("moveX") float moveX,
            @JsonProperty("moveY") float moveY) {
        this.moveX = moveX;
        this.moveY = moveY;
    }

    public float getMoveX() {
        return moveX;
    }

    public float getMoveY() {
        return moveY;
    }
}
