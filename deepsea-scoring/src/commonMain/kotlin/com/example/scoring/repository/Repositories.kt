package com.example.scoring.repository

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<Template>>
    suspend fun getTemplate(id: String): Template?
    suspend fun saveTemplate(template: Template)
    suspend fun deleteTemplate(id: String)
}

interface GameRepository {
    fun observeGames(): Flow<List<Game>>
    suspend fun getGame(id: GameId): Game?
    suspend fun createGame(game: Game): GameId
    suspend fun updatePlayerValue(id: GameId, player: PlayerId, field: FieldId, value: Int)
    suspend fun addPlayer(id: GameId, player: PlayerInput)
    suspend fun removePlayer(id: GameId, player: PlayerId)
    suspend fun setStatus(id: GameId, status: GameStatus)
    suspend fun deleteGame(id: GameId)
}
