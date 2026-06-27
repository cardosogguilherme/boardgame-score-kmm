package com.example.ui.model

import com.example.scoring.model.Game
import com.example.scoring.model.GameStatus
import com.example.scoring.model.Template

data class TemplateRow(val id: String, val name: String)

data class GameRow(
    val id: String,
    val name: String,
    val templateName: String,
    val status: GameStatus,
)

data class LibraryUiState(
    val templates: List<TemplateRow> = emptyList(),
    val games: List<GameRow> = emptyList(),
)

fun libraryUiState(templates: List<Template>, games: List<Game>): LibraryUiState {
    val nameById = templates.associate { it.id to it.name }
    return LibraryUiState(
        templates = templates.map { TemplateRow(it.id, it.name) },
        games = games.map {
            GameRow(it.id.raw, it.name, nameById[it.templateId] ?: it.templateId, it.status)
        },
    )
}
