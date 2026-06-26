package com.example.scoring.engine

import com.example.scoring.model.FieldId
import com.example.scoring.model.ScoringRule

/**
 * Plain-language, one-line rendering for the UI — the readability test that justifies typed
 * rules over free-form formulas (Design law §3.3). Fields are shown by label, resolved from
 * [labels] by id; scoring never references a label (§4).
 */
fun ScoringRule.describe(labels: Map<FieldId, String>): String {
    fun label(f: FieldId) = labels[f] ?: f.raw
    return when (this) {
        is ScoringRule.Lookup -> "${label(field)}: track value scores from a lookup table"
        is ScoringRule.PerUnit -> "${label(field)}: $points point${if (points == 1) "" else "s"} per unit"
        is ScoringRule.Flat -> "${label(field)}: scores its face value"
        is ScoringRule.PerN -> "${label(field)}: 1 point per $divisor"
        is ScoringRule.Ranking -> "${label(field)}: ranked goal awarding ${awards.joinToString(" / ")} by rank"
    }
}
