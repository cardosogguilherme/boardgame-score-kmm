package com.example.scoring.interactors

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.repository.GameRepository
import kotlinx.coroutines.flow.Flow

class ObserveGames(private val repo: GameRepository) {
    operator fun invoke(): Flow<List<Game>> = repo.observeGames()
}

class GetGame(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId): Game? = repo.getGame(id)
}

class CreateGame(private val repo: GameRepository) {
    suspend operator fun invoke(game: Game): GameId = repo.createGame(game)
}

class UpdatePlayerValue(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId, player: PlayerId, field: FieldId, value: Int) =
        repo.updatePlayerValue(id, player, field, value)
}

class FinishGame(private val repo: GameRepository) {
    suspend operator fun invoke(id: GameId) = repo.setStatus(id, GameStatus.FINISHED)
}
