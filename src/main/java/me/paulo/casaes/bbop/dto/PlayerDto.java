package me.paulo.casaes.bbop.dto;

public class PlayerDto {

    private final String playerId;
    private final int ship;
    private final float x;
    private final float y;
    private final float angle;

    protected PlayerDto(String playerId, int ship, float x, float y, float angle) {
        this.playerId = playerId;
        this.ship = ship;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public static PlayerDto of(String playerId, int ship, float x, float y, float angle) {
        return new PlayerDto(playerId, ship, x, y, angle);
    }

    public String getPlayerId() {
        return playerId;
    }

    public int getShip() {
        return ship;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAngle() {
        return angle;
    }
}
