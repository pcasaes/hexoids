package me.pcasaes.hexoids.core.domain.repostiory

import io.smallrye.mutiny.Uni
import me.pcasaes.hexoids.core.domain.model.EntityId
import pcasaes.hexoids.record.proto.PlayerScore

interface ScoreBoardRepository {

    fun fetchPlayerScore(
        playerId: EntityId,
    ): Uni<PlayerScore?>

    fun savePlayerScore(
        playerScore: PlayerScore,
    ): Uni<Unit>

    fun reset(
        playerId: EntityId,
    ): Uni<Unit>

    fun fetchAllScores(): Uni<List<PlayerScore>>
}

object ScoreBoardRepositoryFactory: () -> ScoreBoardRepository {

    private lateinit var repository: ScoreBoardRepository

    fun register(repository: ScoreBoardRepository) {
        this.repository = repository
    }

    override fun invoke(): ScoreBoardRepository {
        return repository
    }
}