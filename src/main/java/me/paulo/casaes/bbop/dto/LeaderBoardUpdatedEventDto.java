package me.paulo.casaes.bbop.dto;

import java.util.ArrayList;
import java.util.List;

public class LeaderBoardUpdatedEventDto implements EventDto {

    private final List<Entry> scores = new ArrayList<>();

    private LeaderBoardUpdatedEventDto() {
    }

    public static LeaderBoardUpdatedEventDto newInstance() {
        return new LeaderBoardUpdatedEventDto();
    }

    public List<Entry> getScores() {
        return scores;
    }

    public void add(String playerId, long score) {
        scores.add(new Entry(playerId, score));
    }

    @Override
    public EventType getEvent() {
        return EventType.LEADERBOARD_UPDATED;
    }

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
