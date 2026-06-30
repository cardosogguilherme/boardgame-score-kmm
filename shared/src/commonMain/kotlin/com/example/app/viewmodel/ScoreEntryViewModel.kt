package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoring.interactors.FinishGame
import com.example.scoring.interactors.GetGame
import com.example.scoring.interactors.GetTemplate
import com.example.scoring.interactors.UpdatePlayerValue
import com.example.scoring.model.FieldId
import com.example.scoring.model.GameId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.ResolvedTemplate
import com.example.scoring.model.resolve
import com.example.ui.model.ScoreEntryUiState
import com.example.ui.model.scoreEntryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the stateless `ScoreEntryScreen` for a real persisted [Game].
 *
 * - On construction it loads the game + its template, resolves the chosen scenario, and seeds the
 *   player table + active player into local [MutableStateFlow]s.
 * - [uiState] is the pure [scoreEntryUiState] reducer over `(players, activeId)`, so the same
 *   tested derivation that powers the demo powers production.
 * - Every edit clamps to `0..Field.max` (the exact rule the old holder used), updates local state
 *   so the total recomputes immediately, AND autosaves via [updatePlayerValue] — no explicit save.
 */
class ScoreEntryViewModel(
    private val gameId: GameId,
    private val getGame: GetGame,
    private val getTemplate: GetTemplate,
    private val updatePlayerValue: UpdatePlayerValue,
    private val finishGame: FinishGame,
) : ViewModel() {

    private val players = MutableStateFlow<List<PlayerInput>>(emptyList())
    private val activeId = MutableStateFlow<PlayerId?>(null)

    // Populated once the game + template load; null until then (pre-load renders an empty screen).
    private var resolved: ResolvedTemplate? = null
    private var title: String = ""
    private var maxByField: Map<FieldId, Int?> = emptyMap()

    val uiState: StateFlow<ScoreEntryUiState> =
        combine(players, activeId) { ps, active ->
            val r = resolved
            if (r == null || ps.isEmpty() || active == null) EMPTY
            else scoreEntryUiState(r, ps, active, title)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EMPTY)

    init {
        viewModelScope.launch {
            val game = getGame(gameId) ?: return@launch
            val template = getTemplate(game.templateId) ?: return@launch
            val r = template.resolve(game.scenarioId)
            resolved = r
            title = game.name
            maxByField = r.fields.associate { it.id to it.max }
            players.value = game.players
            activeId.value = game.players.firstOrNull()?.id
        }
    }

    fun onSelectPlayer(id: PlayerId) {
        activeId.value = id
    }

    fun onIncrement(field: FieldId, player: PlayerId) = step(field, player, +1)

    fun onDecrement(field: FieldId, player: PlayerId) = step(field, player, -1)

    fun finish() {
        viewModelScope.launch { finishGame(gameId) }
    }

    private fun step(field: FieldId, player: PlayerId, delta: Int) {
        val current = players.value.firstOrNull { it.id == player }?.values?.get(field) ?: 0
        val max = maxByField[field]
        val newValue = (current + delta).coerceAtLeast(0).let {
            if (max != null) it.coerceAtMost(max) else it
        }
        if (newValue == current) return
        players.update { list ->
            list.map { p ->
                if (p.id == player) p.copy(values = p.values + (field to newValue)) else p
            }
        }
        viewModelScope.launch { updatePlayerValue(gameId, player, field, newValue) }
    }

    private companion object {
        val EMPTY = ScoreEntryUiState(
            title = "",
            players = emptyList(),
            sections = emptyList(),
            total = 0,
            categoryBreakdown = emptyList(),
        )
    }
}
