package me.pcasaes.bbop.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PlayerScoreUpdateCommandDto implements CommandDto {

    private final long score;

    private PlayerScoreUpdateCommandDto(long score) {
        this.score = score;
    }

    public static PlayerScoreUpdateCommandDto ofScore(long score) {
        return new PlayerScoreUpdateCommandDto(score);
    }

    public long getScore() {
        return score;
    }

    @Override
    public CommandType getCommand() {
        return CommandType.PLAYER_SCORE_UPDATE;
    }
}
