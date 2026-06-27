package com.example.scoring.interactors

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.Template
import com.example.scoring.repository.GameRepository
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTemplateRepository(initial: List<Template> = emptyList()) : TemplateRepository {
    private val state = MutableStateFlow(initial.associateBy { it.id })
    override fun observeTemplates(): Flow<List<Template>> = state.map { it.values.toList() }
    override suspend fun getTemplate(id: String): Template? = state.value[id]
    override suspend fun saveTemplate(template: Template) {
        state.value = state.value + (template.id to template)
    }
    override suspend fun deleteTemplate(id: String) { state.value = state.value - id }
}

class FakeGameRepository(initial: List<Game> = emptyList()) : GameRepository {
    private val state = MutableStateFlow(initial.associateBy { it.id })
    override fun observeGames(): Flow<List<Game>> = state.map { it.values.toList() }
    override suspend fun getGame(id: GameId): Game? = state.value[id]
    override suspend fun createGame(game: Game): GameId {
        state.value = state.value + (game.id to game); return game.id
    }
    override suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int) {
        val game = state.value[id] ?: return
        val players = game.players.map {
            if (it.id == player) it.copy(values = it.values + (field to value)) else it
        }
        state.value = state.value + (id to game.copy(players = players))
    }
    override suspend fun addPlayer(id: GameId, player: PlayerInput) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(players = game.players + player))
    }
    override suspend fun removePlayer(id: GameId, player: PlayerId) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(players = game.players.filterNot { it.id == player }))
    }
    override suspend fun setStatus(id: GameId, status: GameStatus) {
        val game = state.value[id] ?: return
        state.value = state.value + (id to game.copy(status = status))
    }
    override suspend fun deleteGame(id: GameId) { state.value = state.value - id }
}
