package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.scoring.engine.Scorer
import com.example.scoring.engine.ScoringContext
import com.example.scoring.engine.groupedRules
import com.example.scoring.engine.scope
import com.example.scoring.engine.typeTag
import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.ResolvedTemplate
import com.example.scoring.model.RuleId
import com.example.scoring.model.RuleScope
import com.example.scoring.model.ScoringRule

/**
 * State holder ("ViewModel") for Screen A. Wraps a [ScoringContext] — the *whole table* — so the
 * TABLE-scoped ranked goal can resolve across every player while per-player rules read one sheet
 * (Design law §3.6). The domain module stays untouched: this class only orchestrates it.
 *
 * All inputs are mutated through [increment]/[decrement]/[setValue] (steppers, never text), and
 * every output ([total], [breakdown], [sections]) is derived live from the domain interpreters.
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

    /** Live total for the active player. */
    val total: Int get() = Scorer.total(activePlayer, ctx)

    /** Live per-rule breakdown for the active player, keyed by RuleId, in rule order. */
    val breakdown: Map<RuleId, Int> get() = Scorer.breakdown(activePlayer, ctx)

    fun pointsFor(rule: ScoringRule, player: PlayerInput = activePlayer): Int =
        Scorer.score(rule, player, ctx)

    /** Active player's points summed per section group, in group order (footer breakdown). */
    fun categoryBreakdown(): List<Pair<String?, Int>> {
        val b = breakdown
        return template.groupedRules().map { (title, rules) ->
            title to rules.sumOf { b[it.id] ?: 0 }
        }
    }

    /**
     * The screen as ordered sections (from the model's `group` metadata, §10). A group that
     * contains any TABLE-scoped rule becomes a [Section.Ranked] rendered across all players;
     * everything else is a [Section.PerPlayer] bound to the active player's sheet.
     */
    fun sections(): List<Section> = template.groupedRules().map { (title, rules) ->
        if (rules.any { it.scope() == RuleScope.TABLE }) {
            val field = rules.first().referencedFieldOrNull()
            Section.Ranked(
                title = title,
                field = field,
                fieldLabel = field?.let { template.labels[it] } ?: title.orEmpty(),
                columns = rules.map { RankedColumn(it.id, it.typeTag(), it) },
                rows = players.map { p ->
                    RankedRow(
                        player = p,
                        value = field?.let { valueOf(p.id, it) } ?: 0,
                        points = rules.associate { it.id to pointsFor(it, p) },
                        isActive = p.id == activePlayerId,
                    )
                },
            )
        } else {
            Section.PerPlayer(
                title = title,
                rows = rules.map { rule ->
                    val field = rule.referencedFieldOrNull()
                    RuleRow(
                        rule = rule,
                        label = field?.let { template.labels[it] } ?: rule.id.raw,
                        field = field,
                        tag = rule.typeTag(),
                        value = field?.let { valueOf(activePlayerId, it) } ?: 0,
                        points = pointsFor(rule),
                    )
                },
            )
        }
    }
}

private fun ScoringRule.referencedFieldOrNull(): FieldId? = when (this) {
    is ScoringRule.Lookup -> field
    is ScoringRule.PerUnit -> field
    is ScoringRule.Flat -> field
    is ScoringRule.PerN -> field
    is ScoringRule.Ranking -> field
}

/** One rendered section of Screen A. */
sealed interface Section {
    val title: String?

    /** A list of single-input rows bound to the active player's sheet. */
    data class PerPlayer(override val title: String?, val rows: List<RuleRow>) : Section

    /** A cross-player matrix: one input column (the ranking field) + a points column per rule. */
    data class Ranked(
        override val title: String?,
        val field: FieldId?,
        val fieldLabel: String,
        val columns: List<RankedColumn>,
        val rows: List<RankedRow>,
    ) : Section
}

data class RuleRow(
    val rule: ScoringRule,
    val label: String,
    val field: FieldId?,
    val tag: String,
    val value: Int,
    val points: Int,
)

data class RankedColumn(val ruleId: RuleId, val tag: String, val rule: ScoringRule)

data class RankedRow(
    val player: PlayerInput,
    val value: Int,
    val points: Map<RuleId, Int>,
    val isActive: Boolean,
)
