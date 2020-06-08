package me.pcasaes.hexoids.domain.model;

import me.pcasaes.hexoids.domain.utils.DtoUtils;
import pcasaes.hexoids.proto.PlayerScoreIncreasedEventDto;
import pcasaes.hexoids.proto.ScoreBoardUpdatedEventDto;
import pcasaes.hexoids.proto.ScoreEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static me.pcasaes.hexoids.domain.utils.DtoUtils.PLAYER_SCORE_INCREASED_BUILDER;
import static me.pcasaes.hexoids.domain.utils.DtoUtils.PLAYER_SCORE_UPDATED_BUILDER;
import static me.pcasaes.hexoids.domain.utils.DtoUtils.PLAYER_SCORE_UPDATE_COMMAND_BUILDER;
import static me.pcasaes.hexoids.domain.utils.DtoUtils.SCORE_BOARD_ENTRY_BUILDER;
import static me.pcasaes.hexoids.domain.utils.DtoUtils.SCORE_BOARD_UPDATE_BUILDER;

public interface ScoreBoard {

    void updateScore(EntityId playerId, int deltaScore);

    void resetScore(EntityId playerId);

    void fixedUpdate(long timestamp);

    void consumeFromScoreBoardControlTopic(DomainEvent domainEvent);

    void consumeFromScoreBoardUpdateTopic(DomainEvent domainEvent);

    static ScoreBoard create(Clock clock) {
        return new Implementation(clock);
    }

    class Implementation implements ScoreBoard {

        private static final long FIXED_UPDATE_DELTA = 1_000L;


        static final int SCORE_BOARD_SIZE = 10;

        private final Map<EntityId, Integer> scores = new HashMap<>();
        private final Map<EntityId, Long> scoresTimestamps = new HashMap<>();
        private final Map<EntityId, Integer> updatedScores = new HashMap<>();
        private final List<Entry> rankedScoreBoard = new ArrayList<>();

        private long lastFixedUpdateTimestamp = 0L;

        private final Clock clock;

        private Implementation(Clock clock) {
            this.clock = clock;
        }

        @Override
        public void updateScore(EntityId playerId, int deltaScore) {
            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.create(
                            GameTopic.SCORE_BOARD_CONTROL_TOPIC.name(),
                            playerId.getId(),
                            DtoUtils
                                    .newEvent()
                                    .setPlayerScoreIncreased(
                                            PLAYER_SCORE_INCREASED_BUILDER
                                                    .clear()
                                                    .setPlayerId(playerId.getGuid())
                                                    .setGained(deltaScore)
                                                    .setTimestamp(clock.getTime())
                                    )
                                    .build()
                    )
            );
        }

        private void scoreIncreased(PlayerScoreIncreasedEventDto event) {
            EntityId playerId = EntityId.of(event.getPlayerId());
            Long currentTimestamp = this.scoresTimestamps.get(playerId);
            if (currentTimestamp == null || currentTimestamp <= event.getTimestamp()) {
                this.scoresTimestamps.put(playerId, event.getTimestamp());
                int score = this.scores.getOrDefault(playerId, 0) + event.getGained();
                this.scores.put(playerId, score);

                GameEvents.getDomainEvents().dispatch(
                        DomainEvent.create(
                                GameTopic.SCORE_BOARD_UPDATE_TOPIC.name(),
                                playerId.getId(),
                                DtoUtils
                                        .newEvent()
                                        .setPlayerScoreUpdated(
                                                PLAYER_SCORE_UPDATED_BUILDER
                                                        .clear()
                                                        .setPlayerId(playerId.getGuid())
                                                        .setScore(score)
                                        )
                                        .build()
                        )
                );
            }
        }

        private void scoreReset(EntityId playerId) {
            this.scores.remove(playerId);
            this.scoresTimestamps.remove(playerId);

            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.delete(
                            GameTopic.SCORE_BOARD_UPDATE_TOPIC.name(),
                            playerId.getId()
                    )
            );
        }

        private void scoreUpdated(EntityId playerId, int score) {
            if (score <= 0) {
                this.scores.remove(playerId);
                this.scoresTimestamps.remove(playerId);
            } else {
                this.scores.compute(playerId, (k, v) ->
                        Math.max(v == null ? 0 : v, score)
                );
            }
            this.updatedScores.put(playerId, score);
            GameEvents
                    .getClientEvents()
                    .dispatch(
                            DtoUtils.newDtoDirectedCommand(playerId.getGuid(), cmd ->
                                    cmd.setPlayerScoreUpdate(
                                            PLAYER_SCORE_UPDATE_COMMAND_BUILDER
                                                    .clear()
                                                    .setScore(score)
                                    )
                            )
                    );
        }

        @Override
        public void resetScore(EntityId playerId) {
            GameEvents.getDomainEvents().dispatch(
                    DomainEvent.delete(
                            GameTopic.SCORE_BOARD_CONTROL_TOPIC.name(),
                            playerId.getId()
                    )
            );
        }

        @Override
        public void consumeFromScoreBoardControlTopic(DomainEvent domainEvent) {
            if (domainEvent.getEvent() == null) {
                scoreReset(EntityId.of(domainEvent.getKey()));
            } else if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerScoreIncreased()) {
                scoreIncreased(domainEvent.getEvent().getPlayerScoreIncreased());
            }
        }

        @Override
        public void consumeFromScoreBoardUpdateTopic(DomainEvent domainEvent) {
            if (domainEvent.getEvent() == null) {
                scoreUpdated(EntityId.of(domainEvent.getKey()), 0);
            } else if (domainEvent.getEvent() != null && domainEvent.getEvent().hasPlayerScoreUpdated()) {
                EntityId playerId = EntityId.of(domainEvent.getEvent().getPlayerScoreUpdated().getPlayerId());
                int score = domainEvent.getEvent().getPlayerScoreUpdated().getScore();
                scoreUpdated(playerId, score);
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
                    GameEvents.getClientEvents().dispatch(
                            DtoUtils
                                    .newDtoEvent(evt -> {
                                        ScoreBoardUpdatedEventDto.Builder scoreBuilder = SCORE_BOARD_UPDATE_BUILDER
                                                .clear();

                                        while (rankedScoreBoard.size() > SCORE_BOARD_SIZE) {
                                            rankedScoreBoard.remove(rankedScoreBoard.size() - 1);
                                        }

                                        rankedScoreBoard
                                                .stream()
                                                .map(Entry::toDto)
                                                .forEach(scoreBuilder::addScores);
                                        evt.setScoreBoardUpdated(scoreBuilder);
                                    })
                    );
                }
            }
        }

        private void tryAddToScoreBoard(Map.Entry<EntityId, Integer> mapEntry) {
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

        private static class Entry implements Comparable<Entry> {

            private final EntityId playerId;
            private int score;

            public Entry(EntityId playerId) {
                this.playerId = playerId;
            }

            public EntityId getPlayerId() {
                return playerId;
            }

            public int getScore() {
                return score;
            }

            public Entry setScore(int score) {
                this.score = score;
                return this;
            }

            ScoreEntry toDto() {
                return SCORE_BOARD_ENTRY_BUILDER
                        .clear()
                        .setPlayerId(playerId.getGuid())
                        .setScore(score)
                        .build();
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
