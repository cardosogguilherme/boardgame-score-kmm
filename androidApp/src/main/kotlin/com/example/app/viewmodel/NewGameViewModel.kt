package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoring.interactors.CreateGame
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.Template
import com.example.ui.model.NewGameUiState
import com.example.ui.model.PlayerDraftUi
import com.example.ui.model.newGameUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Holds mutable selection state for the New Game screen and produces a [NewGameUiState] via
 * [combine] over the templates stream and the local selection draft.
 *
 * - [onSelectTemplate] resets [selectedScenarioId] to null because scenarios belong to a specific
 *   template and the previous selection would be invalid after switching.
 * - Player tempIds use a monotonic counter so remove-then-add never reuses an id (B3 lesson).
 * - [start] is a no-op when [uiState].canStart is false, ensuring the callback is never invoked
 *   without a valid game.
 */
class NewGameViewModel(
    observeTemplates: ObserveTemplates,
    private val createGame: CreateGame,
) : ViewModel() {

    // ── Internal selection state ──────────────────────────────────────────────────────────────

    private data class SelectionState(
        val selectedTemplateId: String? = null,
        val selectedScenarioId: String? = null,
        val players: List<PlayerDraftUi> = emptyList(),
        val gameName: String = "",
    )

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    private val _selection = MutableStateFlow(SelectionState())

    /** Monotonic — collision-safe after remove+add (mirrors the B3 fix). */
    private var playerIdCounter = 0

    // ── Exposed state ─────────────────────────────────────────────────────────────────────────

    val uiState: StateFlow<NewGameUiState> =
        combine(_templates, _selection) { templates, sel ->
            newGameUiState(
                templates = templates,
                selectedTemplateId = sel.selectedTemplateId,
                selectedScenarioId = sel.selectedScenarioId,
                players = sel.players,
                gameName = sel.gameName,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = newGameUiState(emptyList(), null, null, emptyList(), ""),
        )

    init {
        viewModelScope.launch {
            observeTemplates().collect { _templates.value = it }
        }
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────────────────────

    /** Selects a template and resets any previously selected scenario (it belongs to the old template). */
    fun onSelectTemplate(id: String) {
        _selection.update { it.copy(selectedTemplateId = id, selectedScenarioId = null) }
    }

    fun onSelectScenario(id: String?) {
        _selection.update { it.copy(selectedScenarioId = id) }
    }

    fun onGameNameChange(name: String) {
        _selection.update { it.copy(gameName = name) }
    }

    fun onAddPlayer() {
        val n = ++playerIdCounter
        _selection.update { it.copy(players = it.players + PlayerDraftUi("p-$n", "")) }
    }

    fun onPlayerNameChange(tempId: String, name: String) {
        _selection.update { sel ->
            sel.copy(players = sel.players.map {
                if (it.tempId == tempId) it.copy(name = name) else it
            })
        }
    }

    fun onRemovePlayer(tempId: String) {
        _selection.update { sel ->
            sel.copy(players = sel.players.filterNot { it.tempId == tempId })
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────────────────────

    /**
     * Builds and persists a [Game] then invokes [onCreated] with the returned [GameId].
     * Silently exits when [uiState].canStart is false (guards against UI races).
     */
    fun start(onCreated: (GameId) -> Unit) {
        if (!uiState.value.canStart) return
        viewModelScope.launch {
            val sel = _selection.value
            val gameId = GameId("game-${Random.nextInt(Int.MAX_VALUE)}")
            val game = Game(
                id = gameId,
                templateId = sel.selectedTemplateId!!,
                scenarioId = sel.selectedScenarioId,
                name = sel.gameName,
                players = sel.players.map { draft ->
                    PlayerInput(PlayerId("player-${draft.tempId}"), draft.name)
                },
            )
            val createdId = createGame(game)
            onCreated(createdId)
        }
    }
}
