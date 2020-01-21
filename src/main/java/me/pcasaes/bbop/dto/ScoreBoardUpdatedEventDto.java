package me.pcasaes.bbop.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.ArrayList;
import java.util.List;

@RegisterForReflection
public class ScoreBoardUpdatedEventDto implements EventDto {

    private final List<Entry> scores = new ArrayList<>();

    private ScoreBoardUpdatedEventDto() {
    }

    public static ScoreBoardUpdatedEventDto newInstance() {
        return new ScoreBoardUpdatedEventDto();
    }

    public List<Entry> getScores() {
        return scores;
    }

    public void add(String playerId, long score) {
        scores.add(new Entry(playerId, score));
    }

    @Override
    public EventType getEvent() {
        return EventType.SCOREBOARD_UPDATED;
    }

    @RegisterForReflection
    public static class Entry {
        private final String playerId;
        private final long score;

        private Entry(String playerId, long score) {
            this.playerId = playerId;
            this.score = score;
        }

        public String getPlayerId() {
            return playerId;
        }

        public long getScore() {
            return score;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "playerId='" + playerId + '\'' +
                    ", score=" + score +
                    '}';
        }
    }
}
