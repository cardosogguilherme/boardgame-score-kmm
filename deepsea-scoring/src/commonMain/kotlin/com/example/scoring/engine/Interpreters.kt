package com.example.scoring.engine

import com.example.scoring.model.FieldId
import com.example.scoring.model.ResolvedTemplate
import com.example.scoring.model.RuleScope
import com.example.scoring.model.ScoringRule

// Each interpreter is an exhaustive `when` over the sealed AST. Adding a rule type must force a
// compile error here, never a silent default branch (Design law §3.1).

/** TABLE rules need the whole table; everything else is per-player. */
fun ScoringRule.scope(): RuleScope = when (this) {
    is ScoringRule.Ranking -> RuleScope.TABLE
    is ScoringRule.Lookup,
    is ScoringRule.PerUnit,
    is ScoringRule.Flat,
    is ScoringRule.PerN,
    -> RuleScope.PER_PLAYER
}

/** Fields a rule reads. A future combinator would union its children's fields here. */
fun ScoringRule.referencedFields(): Set<FieldId> = when (this) {
    is ScoringRule.Lookup -> setOf(field)
    is ScoringRule.PerUnit -> setOf(field)
    is ScoringRule.Flat -> setOf(field)
    is ScoringRule.PerN -> setOf(field)
    is ScoringRule.Ranking -> setOf(field)
}

/** Section tag used only for UI grouping (§10). Per-leaf data, surfaced via an interpreter. */
fun ScoringRule.group(): String? = when (this) {
    is ScoringRule.Lookup -> group
    is ScoringRule.PerUnit -> group
    is ScoringRule.Flat -> group
    is ScoringRule.PerN -> group
    is ScoringRule.Ranking -> group
}

/** Compact rule-type tag for the UI: LOOKUP / ×n / FLAT / ÷n / RANK. */
fun ScoringRule.typeTag(): String = when (this) {
    is ScoringRule.Lookup -> "LOOKUP"
    is ScoringRule.PerUnit -> "×n"
    is ScoringRule.Flat -> "FLAT"
    is ScoringRule.PerN -> "÷n"
    is ScoringRule.Ranking -> "RANK"
}

/** Rules bucketed by [group], preserving first-appearance order. Null group = ungrouped. */
fun ResolvedTemplate.groupedRules(): List<Pair<String?, List<ScoringRule>>> {
    val buckets = LinkedHashMap<String?, MutableList<ScoringRule>>()
    for (rule in rules) buckets.getOrPut(rule.group()) { mutableListOf() }.add(rule)
    return buckets.map { (g, rs) -> g to rs.toList() }
}
