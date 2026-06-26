package com.example.scoring.engine

import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.RuleId
import com.example.scoring.model.ScoringRule

/**
 * The scoring interpreter. Total = sum of leaf rules (flat additive model, Design law §3.7).
 * Every branch is explicit; there is no default, so a new rule type breaks the build here.
 */
object Scorer {

    /** Points contributed by [rule] for [player], given the whole-table [ctx]. */
    fun score(rule: ScoringRule, player: PlayerInput, ctx: ScoringContext): Int {
        fun value(f: FieldId) = player.values[f] ?: 0
        return when (rule) {
            is ScoringRule.Lookup -> {
                val v = value(rule.field).coerceAtLeast(0)
                rule.table.getOrElse(v) { rule.table.lastOrNull() ?: 0 }
            }
            is ScoringRule.PerUnit -> value(rule.field) * rule.points
            is ScoringRule.Flat -> value(rule.field)
            is ScoringRule.PerN -> if (rule.divisor == 0) 0 else value(rule.field) / rule.divisor
            is ScoringRule.Ranking ->
                rankAward(value(rule.field), ctx.column(rule.field), rule.awards)
        }
    }

    /** Per-rule contributions for [player], keyed by RuleId, in template rule order. */
    fun breakdown(player: PlayerInput, ctx: ScoringContext): Map<RuleId, Int> =
        ctx.template.rules.associate { it.id to score(it, player, ctx) }

    /** [player]'s total = sum of all leaf rules. */
    fun total(player: PlayerInput, ctx: ScoringContext): Int =
        ctx.template.rules.sumOf { score(it, player, ctx) }
}
