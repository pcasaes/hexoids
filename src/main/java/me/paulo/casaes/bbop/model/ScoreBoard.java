package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.LeaderBoardUpdatedEventDto;
import me.paulo.casaes.bbop.dto.PlayerScoreUpdateDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public interface ScoreBoard {

    void updateScore(String playerId, long deltaScore);

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

        static final int LEADER_BOARD_SIZE = 10;

        private static final ScoreBoard INSTANCE = new Implementation();

        private final Map<String, Long> scores = new HashMap<>();
        private final Map<String, Long> updatedScores = new HashMap<>();
        private final List<Entry> rankedLeaderBoard = new ArrayList<>();

        private long lastFixedUpdateTimestamp = 0L;

        @Override
        public void updateScore(String playerId, long deltaScore) {
            long score = this.scores.getOrDefault(playerId, Long.valueOf(0)) + deltaScore;
            this.scores.put(playerId, score);
            this.updatedScores.put(playerId, score);
            GameEvents.get().register(DirectedCommandDto.of(playerId, PlayerScoreUpdateDto.ofScore(score)));
        }

        @Override
        public void fixedUpdate(long timestamp) {
            if (!updatedScores.isEmpty() && timestamp - lastFixedUpdateTimestamp >= FIXED_UPDATE_DELTA) {
                lastFixedUpdateTimestamp = timestamp;

                updatedScores
                        .entrySet()
                        .forEach(this::tryAddLeaderBoard);


                updatedScores.clear();
                if (!rankedLeaderBoard.isEmpty()) {
                    rankedLeaderBoard.sort(Entry::compare);

                    LeaderBoardUpdatedEventDto event = LeaderBoardUpdatedEventDto.newInstance();

                    while (rankedLeaderBoard.size() > LEADER_BOARD_SIZE) {
                        rankedLeaderBoard.remove(rankedLeaderBoard.size() - 1);
                    }

                    rankedLeaderBoard
                            .forEach(entry -> event.add(entry.getPlayerId(), entry.getScore()));


                    GameEvents.get().register(event);
                }
            }
        }

        private void tryAddLeaderBoard(Map.Entry<String, Long> mapEntry) {
            OptionalInt index = IntStream
                    .range(0, rankedLeaderBoard.size())
                    .filter(i -> rankedLeaderBoard.get(i).getPlayerId().equals(mapEntry.getKey()))
                    .findFirst();
            if (index.isPresent()) {
                rankedLeaderBoard.get(index.getAsInt()).setScore(mapEntry.getValue());
            } else {
                rankedLeaderBoard.add(new Entry(mapEntry.getKey()).setScore(mapEntry.getValue()));
            }
        }

        @Override
        public void reset() {
            scores.clear();
            rankedLeaderBoard.clear();
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
