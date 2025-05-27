package me.pcasaes.hexoids.core.domain.model

import io.smallrye.mutiny.Uni
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueue
import me.pcasaes.hexoids.core.domain.eventqueue.GameQueueFactory
import me.pcasaes.hexoids.core.domain.model.DomainEvent.Companion.create
import me.pcasaes.hexoids.core.domain.model.DomainEvent.Companion.delete
import me.pcasaes.hexoids.core.domain.model.EntityId.Companion.of
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getClientEvents
import me.pcasaes.hexoids.core.domain.model.GameEvents.Companion.getDomainEvents
import me.pcasaes.hexoids.core.domain.repostiory.ScoreBoardRepository
import me.pcasaes.hexoids.core.domain.repostiory.ScoreBoardRepositoryFactory
import pcasaes.hexoids.proto.DirectedCommand
import pcasaes.hexoids.proto.Dto
import pcasaes.hexoids.proto.Event
import pcasaes.hexoids.proto.PlayerScoreIncreasedEventDto
import pcasaes.hexoids.proto.PlayerScoreUpdateCommandDto
import pcasaes.hexoids.proto.PlayerScoreUpdatedEventDto
import pcasaes.hexoids.proto.ScoreBoardUpdatedEventDto
import pcasaes.hexoids.proto.ScoreEntry
import pcasaes.hexoids.record.proto.PlayerScore
import java.util.Objects
import kotlin.collections.MutableMap.MutableEntry

interface ScoreBoard {

    fun updateScore(playerId: EntityId, deltaScore: Int)

    fun resetScore(playerId: EntityId)

    fun fixedUpdate(timestamp: Long)

    fun consumeFromScoreBoardControlTopic(domainEvent: DomainEvent)

    fun consumeFromScoreBoardUpdateTopic(domainEvent: DomainEvent)

    class Implementation(
        private val clock: Clock,
        private val repositoryFactory: () -> ScoreBoardRepository,
        private val gameQueueFactory: () -> GameQueue,
    ) : ScoreBoard {
        private var lastFixedUpdateTimestamp = 0L
        private val updatedScores = HashMap<EntityId, Int>()
        private val rankedScoreBoard = ArrayList<Entry>()

        override fun updateScore(playerId: EntityId, deltaScore: Int) {
            getDomainEvents().dispatch(
                create(
                    GameTopic.SCORE_BOARD_CONTROL_TOPIC.name,
                    playerId.getId(),
                    Event.newBuilder()
                        .setPlayerScoreIncreased(
                            PlayerScoreIncreasedEventDto.newBuilder()
                                .setPlayerId(playerId.getGuid())
                                .setGained(deltaScore)
                                .setTimestamp(clock.getTime())
                        )
                        .build()
                )
            )
        }

        private fun scoreIncreased(event: PlayerScoreIncreasedEventDto) {
            val playerId = of(event.playerId)
            repositoryFactory()
                .fetchPlayerScore(playerId)
                .onItem()
                .transformToUni { playerScore ->
                    val currentTimestamp = playerScore?.timestamp
                    val newScore = (playerScore?.score ?: 0) + event.gained
                    (if (currentTimestamp == null || currentTimestamp <= event.timestamp) {
                        val newScore = PlayerScore.newBuilder()
                            .setPlayerId(event.playerId)
                            .setTimestamp(event.timestamp)
                            .setScore(newScore)
                            .build()

                        repositoryFactory().savePlayerScore(newScore)
                    } else {
                        Uni.createFrom().nullItem()
                    }).onItem().transform { newScore }
                }
                .onItem()
                .invoke { newScore: Int ->
                    gameQueueFactory().enqueue {
                        getDomainEvents().dispatch(
                            create(
                                GameTopic.SCORE_BOARD_UPDATE_TOPIC.name,
                                playerId.getId(),
                                Event.newBuilder()
                                    .setPlayerScoreUpdated(
                                        PlayerScoreUpdatedEventDto.newBuilder()
                                            .setPlayerId(playerId.getGuid())
                                            .setScore(newScore)
                                    )
                                    .build()
                            )
                        )
                    }
                }
                .subscribe().with { }

        }


        private fun scoreReset(playerId: EntityId) {
            repositoryFactory()
                .reset(playerId)
                .onItem()
                .invoke { _ ->
                    gameQueueFactory().enqueue {
                        getDomainEvents().dispatch(
                            delete(
                                GameTopic.SCORE_BOARD_UPDATE_TOPIC.name,
                                playerId.getId()
                            )
                        )
                    }
                }
                .subscribe().with { }
        }

        private fun scoreUpdated(playerId: EntityId, score: Int) {
            this.updatedScores.put(playerId, score)
            getClientEvents()
                .dispatch(
                    Dto.newBuilder()
                        .setDirectedCommand(
                            DirectedCommand.newBuilder()
                                .setPlayerId(playerId.getGuid())
                                .setPlayerScoreUpdate(PlayerScoreUpdateCommandDto.newBuilder().setScore(score.toLong()))
                        )
                        .build()
                )
        }

        override fun resetScore(playerId: EntityId) {
            getDomainEvents().dispatch(
                delete(
                    GameTopic.SCORE_BOARD_CONTROL_TOPIC.name,
                    playerId.getId()
                )
            )
        }

        override fun consumeFromScoreBoardControlTopic(domainEvent: DomainEvent) {
            val event = domainEvent.event
            if (event == null) {
                scoreReset(of(domainEvent.key))
            } else if (event.hasPlayerScoreIncreased()) {
                scoreIncreased(event.getPlayerScoreIncreased())
            }
        }

        override fun consumeFromScoreBoardUpdateTopic(domainEvent: DomainEvent) {
            val event = domainEvent.event
            if (event == null) {
                scoreUpdated(of(domainEvent.key), 0)
            } else if (event.hasPlayerScoreUpdated()) {
                val playerId = of(event.getPlayerScoreUpdated().playerId)
                val score = event.getPlayerScoreUpdated().score
                scoreUpdated(playerId, score)
            }
        }

        override fun fixedUpdate(timestamp: Long) {
            if (updatedScores.isNotEmpty() && timestamp - lastFixedUpdateTimestamp >= FIXED_UPDATE_DELTA) {
                lastFixedUpdateTimestamp = timestamp

                updatedScores
                    .entries
                    .forEach { mapEntry -> this.tryAddToScoreBoard(mapEntry) }


                updatedScores.clear()
                if (rankedScoreBoard.isNotEmpty()) {
                    rankedScoreBoard.sortWith { o1, o2 -> Entry.Companion.compare(o1, o2) }

                    val scoreBuilderEvent = ScoreBoardUpdatedEventDto.newBuilder()

                    while (rankedScoreBoard.size > SCORE_BOARD_SIZE) {
                        rankedScoreBoard.removeAt(rankedScoreBoard.size - 1)
                    }

                    rankedScoreBoard
                        .map { obj -> obj.toDto() }
                        .forEach { value -> scoreBuilderEvent.addScores(value) }

                    getClientEvents().dispatch(
                        Dto
                            .newBuilder()
                            .setEvent(
                                Event.newBuilder()
                                    .setScoreBoardUpdated(scoreBuilderEvent)
                            )
                            .build()
                    )
                }
            }
        }

        private fun tryAddToScoreBoard(mapEntry: MutableEntry<EntityId, Int>) {
            val index = (0 until rankedScoreBoard.size)
                .firstOrNull { i -> rankedScoreBoard[i].getPlayerId() == mapEntry.key }
            if (index != null) {
                rankedScoreBoard[index].setScore(mapEntry.value)
            } else {
                rankedScoreBoard.add(Entry(mapEntry.key).setScore(mapEntry.value))
            }
        }

        private class Entry(private val playerId: EntityId) : Comparable<Entry> {
            private var score = 0

            fun getPlayerId(): EntityId {
                return playerId
            }

            fun getScore(): Int {
                return score
            }

            fun setScore(score: Int): Entry {
                this.score = score
                return this
            }

            fun toDto(): ScoreEntry {
                return ScoreEntry.newBuilder()
                    .setPlayerId(playerId.getGuid())
                    .setScore(score)
                    .build()
            }

            override fun compareTo(other: Entry): Int {
                return compare(this, other)
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || javaClass != other.javaClass) return false
                val entry = other as Entry
                return playerId == entry.playerId
            }

            override fun hashCode(): Int {
                return Objects.hash(playerId)
            }

            companion object {
                fun compare(o1: Entry, o2: Entry): Int {
                    return o2.score.toLong().compareTo(o1.score.toLong())
                }
            }
        }

        companion object {
            private const val FIXED_UPDATE_DELTA = 1000L

            const val SCORE_BOARD_SIZE: Int = 10
        }
    }

    companion object {

        fun create(
            clock: Clock,
            repositoryFactory: () -> ScoreBoardRepository = ScoreBoardRepositoryFactory,
            gameQueueFactory: () -> GameQueue = GameQueueFactory
        ): ScoreBoard {
            return Implementation(clock, repositoryFactory, gameQueueFactory)
        }
    }
}
