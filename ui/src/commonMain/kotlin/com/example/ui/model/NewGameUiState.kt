package com.example.ui.model

import com.example.scoring.model.Template

// ── Option types ──────────────────────────────────────────────────────────────────────────────

data class TemplateOptionUi(val id: String, val name: String)

data class ScenarioOptionUi(val id: String, val name: String)

data class PlayerDraftUi(val tempId: String, val name: String)

// ── Screen-level state ────────────────────────────────────────────────────────────────────────

data class NewGameUiState(
    val templates: List<TemplateOptionUi>,
    val selectedTemplateId: String?,
    val scenarios: List<ScenarioOptionUi>,
    val selectedScenarioId: String?,
    val players: List<PlayerDraftUi>,
    val gameName: String,
    val canStart: Boolean,
)

// ── Mapper / reducer ──────────────────────────────────────────────────────────────────────────

fun newGameUiState(
    templates: List<Template>,
    selectedTemplateId: String?,
    selectedScenarioId: String?,
    players: List<PlayerDraftUi>,
    gameName: String,
): NewGameUiState {
    val selectedTemplate = if (selectedTemplateId != null) {
        templates.find { it.id == selectedTemplateId }
    } else {
        null
    }
    val scenarios = selectedTemplate
        ?.scenarios
        ?.map { ScenarioOptionUi(it.id, it.name) }
        ?: emptyList()
    val canStart = selectedTemplateId != null
        && players.isNotEmpty()
        && players.all { it.name.isNotBlank() }
        && gameName.isNotBlank()
    return NewGameUiState(
        templates = templates.map { TemplateOptionUi(it.id, it.name) },
        selectedTemplateId = selectedTemplateId,
        scenarios = scenarios,
        selectedScenarioId = selectedScenarioId,
        players = players,
        gameName = gameName,
        canStart = canStart,
    )
}
