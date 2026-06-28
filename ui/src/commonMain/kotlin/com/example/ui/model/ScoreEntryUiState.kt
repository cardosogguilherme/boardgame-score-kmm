package com.example.ui.model

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

// ── UI-state types (pure data; no Compose) ──────────────────────────────────────────────────────

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

/** A selectable player chip in the score-entry header. */
data class PlayerChip(val id: PlayerId, val name: String, val isActive: Boolean)

/**
 * Immutable snapshot driving the stateless `ScoreEntryScreen`: who the players are, the ordered
 * sections to render, and the active player's footer totals. Built purely by [scoreEntryUiState].
 */
data class ScoreEntryUiState(
    val title: String,
    val players: List<PlayerChip>,
    val sections: List<Section>,
    val total: Int,
    val categoryBreakdown: List<Pair<String?, Int>>,
)

// ── Pure reducer ────────────────────────────────────────────────────────────────────────────────

private fun ScoringRule.referencedFieldOrNull(): FieldId? = when (this) {
    is ScoringRule.Lookup -> field
    is ScoringRule.PerUnit -> field
    is ScoringRule.Flat -> field
    is ScoringRule.PerN -> field
    is ScoringRule.Ranking -> field
}

/**
 * Pure derivation of the whole score-entry screen from the resolved template + the player table +
 * the active player. Reproduces, exactly, the section/total/breakdown logic that used to live in
 * `ScoreEntryState`: a group containing any TABLE-scoped rule becomes a [Section.Ranked] spanning
 * all players; everything else is a [Section.PerPlayer] bound to the active sheet. No Compose.
 */
fun scoreEntryUiState(
    template: ResolvedTemplate,
    players: List<PlayerInput>,
    activeId: PlayerId,
    title: String = "",
): ScoreEntryUiState {
    require(players.isNotEmpty()) { "Score entry needs at least one player." }
    val ctx = ScoringContext(template, players)
    val activePlayer = players.first { it.id == activeId }

    fun value(field: FieldId, player: PlayerInput): Int = player.values[field] ?: 0
    fun pointsFor(rule: ScoringRule, player: PlayerInput): Int = Scorer.score(rule, player, ctx)

    val breakdown: Map<RuleId, Int> = Scorer.breakdown(activePlayer, ctx)
    val grouped = template.groupedRules()

    val sections = grouped.map { (sectionTitle, rules) ->
        if (rules.any { it.scope() == RuleScope.TABLE }) {
            val field = rules.first().referencedFieldOrNull()
            Section.Ranked(
                title = sectionTitle,
                field = field,
                fieldLabel = field?.let { template.labels[it] } ?: sectionTitle.orEmpty(),
                columns = rules.map { RankedColumn(it.id, it.typeTag(), it) },
                rows = players.map { p ->
                    RankedRow(
                        player = p,
                        value = field?.let { value(it, p) } ?: 0,
                        points = rules.associate { it.id to pointsFor(it, p) },
                        isActive = p.id == activeId,
                    )
                },
            )
        } else {
            Section.PerPlayer(
                title = sectionTitle,
                rows = rules.map { rule ->
                    val field = rule.referencedFieldOrNull()
                    RuleRow(
                        rule = rule,
                        label = field?.let { template.labels[it] } ?: rule.id.raw,
                        field = field,
                        tag = rule.typeTag(),
                        value = field?.let { value(it, activePlayer) } ?: 0,
                        points = pointsFor(rule, activePlayer),
                    )
                },
            )
        }
    }

    val categoryBreakdown = grouped.map { (sectionTitle, rules) ->
        sectionTitle to rules.sumOf { breakdown[it.id] ?: 0 }
    }

    return ScoreEntryUiState(
        title = title,
        players = players.map { PlayerChip(it.id, it.name, it.id == activeId) },
        sections = sections,
        total = Scorer.total(activePlayer, ctx),
        categoryBreakdown = categoryBreakdown,
    )
}
