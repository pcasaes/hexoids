package me.paulo.casaes.bbop.model;

import me.paulo.casaes.bbop.dto.DirectedCommandDto;
import me.paulo.casaes.bbop.dto.EventType;
import me.paulo.casaes.bbop.dto.PlayerScoreIncreasedEventDto;
import me.paulo.casaes.bbop.dto.PlayerScoreUpdateCommandDto;
import me.paulo.casaes.bbop.dto.PlayerScoreUpdatedEventDto;
import me.paulo.casaes.bbop.dto.ScoreBoardUpdatedEventDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

public interface ScoreBoard {

    void updateScore(UUID playerId, int deltaScore);

    void scoreUpdated(PlayerScoreUpdatedEventDto event);

    void scoreIncreased(PlayerScoreIncreasedEventDto event);

    void resetScore(UUID playerId);

    void scoreReset(UUID playerId);

    void fixedUpdate(long timestamp);

    void reset();

    void consumeFromScoreBoardControlTopic(DomainEvent domainEvent);

    void consumeFromScoreBoardUpdateTopic(DomainEvent domainEvent);

    static ScoreBoard get() {
        return Implementation.INSTANCE;
    }

    class Implementation implements ScoreBoard {

        private static final long FIXED_UPDATE_DELTA = 1_000L;

        private static final ScoreBoard INSTANCE = new Implementation();


        static final int SCORE_BOARD_SIZE = 10;

        private final Map<UUID, Integer> scores = new HashMap<>();
        private final Map<UUID, Integer> updatedScores = new HashMap<>();
        private final List<Entry> rankedScoreBoard = new ArrayList<>();

        private long lastFixedUpdateTimestamp = 0L;

        @Override
        public void updateScore(UUID playerId, int deltaScore) {
            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.ScoreBoardControlTopic.name(),
                            playerId,
                            PlayerScoreIncreasedEventDto.increased(playerId, deltaScore)
                    )
            );
        }

        @Override
        public void scoreIncreased(PlayerScoreIncreasedEventDto event) {
            int score = this.scores.getOrDefault(event.getPlayerId(), 0) + event.getGained();
            this.scores.put(event.getPlayerId(), score);

            GameEvents.getDomainEvents().register(
                    DomainEvent.create(
                            Topics.ScoreBoardUpdateTopic.name(),
                            event.getPlayerId(),
                            PlayerScoreUpdatedEventDto.updated(event.getPlayerId(), score)
                    )
            );
        }

        @Override
        public void scoreReset(UUID playerId) {
            this.scores.remove(playerId);

            GameEvents.getDomainEvents().register(
                    DomainEvent.delete(
                            Topics.ScoreBoardUpdateTopic.name(),
                            playerId
                    )
            );
        }

        @Override
        public void scoreUpdated(PlayerScoreUpdatedEventDto event) {
            if (event.getScore() <= 0) {
                this.scores.remove(event.getPlayerId());
            } else {
                this.scores.compute(event.getPlayerId(), (k, v) ->
                        Math.max(v == null ? 0 : v, event.getScore())
                );
            }
            this.updatedScores.put(event.getPlayerId(), event.getScore());
            GameEvents.getClientEvents().register(DirectedCommandDto.of(event.getPlayerId().toString(), PlayerScoreUpdateCommandDto.ofScore(event.getScore())));
        }

        @Override
        public void resetScore(UUID playerId) {
            GameEvents.getDomainEvents().register(
                    DomainEvent.delete(
                            Topics.ScoreBoardControlTopic.name(),
                            playerId
                    )
            );
        }

        @Override
        public void consumeFromScoreBoardControlTopic(DomainEvent domainEvent) {
            if (domainEvent.getEvent() == null) {
                scoreReset(domainEvent.getKey());
            } else if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_SCORE_INCREASED)) {
                scoreIncreased((PlayerScoreIncreasedEventDto) domainEvent.getEvent());
            }
        }

        @Override
        public void consumeFromScoreBoardUpdateTopic(DomainEvent domainEvent) {
            if (domainEvent.getEvent() == null) {
                scoreUpdated(PlayerScoreUpdatedEventDto.updated(domainEvent.getKey(), 0));
            } else if (domainEvent.getEvent() != null && domainEvent.getEvent().isEvent(EventType.PLAYER_SCORE_UPDATED)) {
                scoreUpdated((PlayerScoreUpdatedEventDto) domainEvent.getEvent());
            }
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
                            .forEach(entry -> event.add(entry.getPlayerId().toString(), entry.getScore()));


                    GameEvents.getClientEvents().register(event);
                }
            }
        }

        private void tryAddToScoreBoard(Map.Entry<UUID, Integer> mapEntry) {
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

            private final UUID playerId;
            private long score;

            public Entry(UUID playerId) {
                this.playerId = playerId;
            }

            public UUID getPlayerId() {
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
