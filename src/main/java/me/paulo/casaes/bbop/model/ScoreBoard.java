package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.ScoreBoardUpdatedEventDto;
import me.paulo.casaes.bbop.dto.PlayerScoreUpdateDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public interface ScoreBoard {

    void updateScore(String playerId, int deltaScore);

    void resetScore(String playerId);

    void fixedUpdate(long timestamp);

    void reset();

    class Factory {

        private Factory() {
        }

        public static ScoreBoard get() {
            return ScoreBoard.Implementation.INSTANCE;
        }
    }


    class Implementation implements ScoreBoard {

        private static final long FIXED_UPDATE_DELTA = 1_000L;

        static final int SCORE_BOARD_SIZE = 10;

        private static final ScoreBoard INSTANCE = new Implementation();

        private final Map<String, Integer> scores = new HashMap<>();
        private final Map<String, Integer> updatedScores = new HashMap<>();
        private final List<Entry> rankedScoreBoard = new ArrayList<>();

        private long lastFixedUpdateTimestamp = 0L;

        @Override
        public void updateScore(String playerId, int deltaScore) {
            int score = this.scores.getOrDefault(playerId, 0) + deltaScore;
            this.scores.put(playerId, score);
            this.updatedScores.put(playerId, score);
            GameEvents.getClientEvents().register(DirectedCommandDto.of(playerId, PlayerScoreUpdateDto.ofScore(score)));
        }

        @Override
        public void resetScore(String playerId) {
            this.scores.put(playerId, 0);
            this.updatedScores.put(playerId, 0);
            GameEvents.getClientEvents().register(DirectedCommandDto.of(playerId, PlayerScoreUpdateDto.ofScore(0)));
        }

        @Override
        public void fixedUpdate(long timestamp) {
            if (!updatedScores.isEmpty() && timestamp - lastFixedUpdateTimestamp >= FIXED_UPDATE_DELTA) {
                lastFixedUpdateTimestamp = timestamp;

                updatedScores
                        .entrySet()
                        .forEach(this::tryAddToScoreBoard);


                updatedScores.clear();
                if (!rankedScoreBoard.isEmpty()) {
                    rankedScoreBoard.sort(Entry::compare);

                    ScoreBoardUpdatedEventDto event = ScoreBoardUpdatedEventDto.newInstance();

                    while (rankedScoreBoard.size() > SCORE_BOARD_SIZE) {
                        rankedScoreBoard.remove(rankedScoreBoard.size() - 1);
                    }

                    rankedScoreBoard
                            .forEach(entry -> event.add(entry.getPlayerId(), entry.getScore()));


                    GameEvents.getClientEvents().register(event);
                }
            }
        }

        private void tryAddToScoreBoard(Map.Entry<String, Integer> mapEntry) {
            OptionalInt index = IntStream
                    .range(0, rankedScoreBoard.size())
                    .filter(i -> rankedScoreBoard.get(i).getPlayerId().equals(mapEntry.getKey()))
                    .findFirst();
            if (index.isPresent()) {
                rankedScoreBoard.get(index.getAsInt()).setScore(mapEntry.getValue());
            } else {
                rankedScoreBoard.add(new Entry(mapEntry.getKey()).setScore(mapEntry.getValue()));
            }
        }

        @Override
        public void reset() {
            scores.clear();
            rankedScoreBoard.clear();
            updatedScores.clear();
            this.lastFixedUpdateTimestamp = 0L;
        }

        private static class Entry implements Comparable<Entry> {

            private final String playerId;
            private long score;

            public Entry(String playerId) {
                this.playerId = playerId;
            }

            public String getPlayerId() {
                return playerId;
            }

            public long getScore() {
                return score;
            }

            public Entry setScore(long score) {
                this.score = score;
                return this;
            }

            @Override
            public int compareTo(Entry o) {
                return compare(this, o);
            }

            public static int compare(Entry o1, Entry o2) {
                return Long.compare(o2.score, o1.score);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Entry entry = (Entry) o;
                return Objects.equals(playerId, entry.playerId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(playerId);
            }
        }

    }

}
