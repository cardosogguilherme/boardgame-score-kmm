package com.example.scoring.engine

import com.example.scoring.model.FieldKind
import com.example.scoring.model.ResolvedTemplate
import com.example.scoring.model.RuleScope
import com.example.scoring.model.ScoringRule

/**
 * Structural validation of a resolved template. Returns human-readable problems; an empty list
 * means valid. Notably enforces that a TABLE-scoped rule sits on a RANKING field (Design law §3.6).
 */
fun ResolvedTemplate.validate(): List<String> {
    val problems = mutableListOf<String>()
    val fieldsById = fields.associateBy { it.id }
    for (rule in rules) {
        for (f in rule.referencedFields()) {
            val field = fieldsById[f]
            if (field == null) {
                problems += "Rule '${rule.id.raw}' references unknown field '${f.raw}'."
                continue
            }
            if (rule.scope() == RuleScope.TABLE && field.kind != FieldKind.RANKING) {
                problems += "Rule '${rule.id.raw}' is table-scoped but field '${f.raw}' is " +
                    "${field.kind}, not RANKING."
            }
        }
        when (rule) {
            is ScoringRule.Lookup ->
                if (rule.table.isEmpty()) problems += "Lookup rule '${rule.id.raw}' has an empty table."
            is ScoringRule.PerN ->
                if (rule.divisor == 0) problems += "PerN rule '${rule.id.raw}' has divisor 0."
            is ScoringRule.Ranking ->
                if (rule.awards.isEmpty()) problems += "Ranking rule '${rule.id.raw}' has no awards."
            is ScoringRule.PerUnit, is ScoringRule.Flat -> Unit
        }
    }
    return problems
}
