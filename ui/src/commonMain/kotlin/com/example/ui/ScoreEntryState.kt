package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scoring.engine.Scorer
import com.example.scoring.engine.ScoringContext
import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.ResolvedTemplate
import com.example.scoring.model.RuleId
import com.example.scoring.model.ScoringRule
import com.example.ui.model.ScoreEntryUiState
import com.example.ui.model.Section
import com.example.ui.model.scoreEntryUiState

/**
 * State holder ("ViewModel") for Screen A. Wraps a [ScoringContext] — the *whole table* — so the
 * TABLE-scoped ranked goal can resolve across every player while per-player rules read one sheet
 * (Design law §3.6). The domain module stays untouched: this class only orchestrates it.
 *
 * Mutation (steppers, never text) lives here; all *derivation* (sections/total/breakdown) delegates
 * to the pure [scoreEntryUiState] reducer in `com.example.ui.model` via [uiStateNow], so the screen
 * and this holder compute scores from one shared, tested source of truth.
 */
class ScoreEntryState(
    val template: ResolvedTemplate,
    initialPlayers: List<PlayerInput>,
    activePlayerId: PlayerId = initialPlayers.first().id,
) {
    init {
        require(initialPlayers.isNotEmpty()) { "Score entry needs at least one player." }
    }

    var players by mutableStateOf(initialPlayers)
        private set

    var activePlayerId by mutableStateOf(activePlayerId)

    private val ctx: ScoringContext get() = ScoringContext(template, players)
    private val maxByField: Map<FieldId, Int?> = template.fields.associate { it.id to it.max }

    val activePlayer: PlayerInput get() = players.first { it.id == activePlayerId }

    fun valueOf(playerId: PlayerId, field: FieldId): Int =
        players.first { it.id == playerId }.values[field] ?: 0

    fun setValue(field: FieldId, raw: Int, playerId: PlayerId = activePlayerId) {
        val max = maxByField[field]
        val clamped = raw.coerceAtLeast(0).let { if (max != null) it.coerceAtMost(max) else it }
        players = players.map { p ->
            if (p.id == playerId) p.copy(values = p.values + (field to clamped)) else p
        }
    }

    fun increment(field: FieldId, by: Int = 1, playerId: PlayerId = activePlayerId) =
        setValue(field, valueOf(playerId, field) + by, playerId)

    fun decrement(field: FieldId, by: Int = 1, playerId: PlayerId = activePlayerId) =
        setValue(field, valueOf(playerId, field) - by, playerId)

    /** The full derived UI snapshot — the single source of truth for every derived output below. */
    fun uiStateNow(): ScoreEntryUiState = scoreEntryUiState(template, players, activePlayerId)

    /** Live total for the active player. */
    val total: Int get() = uiStateNow().total

    /** Live per-rule breakdown for the active player, keyed by RuleId, in rule order. */
    val breakdown: Map<RuleId, Int> get() = Scorer.breakdown(activePlayer, ctx)

    fun pointsFor(rule: ScoringRule, player: PlayerInput = activePlayer): Int =
        Scorer.score(rule, player, ctx)

    /** Active player's points summed per section group, in group order (footer breakdown). */
    fun categoryBreakdown(): List<Pair<String?, Int>> = uiStateNow().categoryBreakdown

    /**
     * The screen as ordered sections (from the model's `group` metadata, §10). A group that
     * contains any TABLE-scoped rule becomes a [Section.Ranked] rendered across all players;
     * everything else is a [Section.PerPlayer] bound to the active player's sheet.
     */
    fun sections(): List<Section> = uiStateNow().sections
}
