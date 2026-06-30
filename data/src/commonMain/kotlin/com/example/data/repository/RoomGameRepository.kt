package com.example.data.repository

import com.example.data.dao.GameDao
import com.example.data.entity.PlayerValueEntity
import com.example.data.mapper.gameFrom
import com.example.data.mapper.toEntities
import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// `now` is injected (no default) because the wall clock is platform-specific — commonMain has no
// `System.currentTimeMillis`. The platform Koin module supplies it (Android: System.currentTimeMillis).
class RoomGameRepository(
    private val dao: GameDao,
    private val now: () -> Long,
) : GameRepository {
    override fun observeGames(): Flow<List<Game>> =
        combine(
            dao.observeGames(),
            dao.observeAllPlayers(),
            dao.observeAllValues(),
        ) { games, players, values ->
            games.map { g ->
                gameFrom(
                    g,
                    players.filter { it.gameId == g.id },
                    values.filter { it.gameId == g.id },
                )
            }
        }

    override suspend fun getGame(id: GameId): Game? {
        val g = dao.getGame(id.raw) ?: return null
        return gameFrom(g, dao.players(id.raw), dao.values(id.raw))
    }

    override suspend fun createGame(game: Game): GameId {
        val (g, players, values) = game.toEntities(now())
        dao.insertGame(g, players, values)
        return game.id
    }

    override suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int) {
        dao.upsertValue(PlayerValueEntity(id.raw, player.raw, field.raw, value))
    }

    override suspend fun addPlayer(id: GameId, player: PlayerInput) {
        dao.addPlayerAtEnd(id.raw, player.id.raw, player.name)
    }

    override suspend fun removePlayer(id: GameId, player: PlayerId) {
        dao.deletePlayer(id.raw, player.raw)
    }

    override suspend fun setStatus(id: GameId, status: GameStatus) {
        dao.setStatus(id.raw, status.name)
    }

    override suspend fun deleteGame(id: GameId) {
        dao.deleteGame(id.raw)
    }
}
