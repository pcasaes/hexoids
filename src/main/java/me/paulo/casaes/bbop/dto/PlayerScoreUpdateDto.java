package me.paulo.casaes.bbop.dto;

public class PlayerScoreUpdateDto implements CommandDto {

    private final long score;

    private PlayerScoreUpdateDto(long score) {
        this.score = score;
    }

    public static PlayerScoreUpdateDto ofScore(long score) {
        return new PlayerScoreUpdateDto(score);
    }

    public long getScore() {
        return score;
    }

    @Override
    public CommandType getCommand() {
        return CommandType.PLAYER_SCORE_UPDATE;
    }
}
