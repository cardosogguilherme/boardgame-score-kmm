package com.example.scoring.model

/**
 * A [Template] flattened with a chosen scenario overlaid: base fields/rules plus the scenario's.
 * Carries a [labels] lookup so interpreters can render field labels by [FieldId] (never the
 * reverse — Design law §4). Grouping for the UI is provided by `ResolvedTemplate.groupedRules()`
 * in the engine package, which reads each rule's `group`.
 */
data class ResolvedTemplate(
    val fields: List<Field>,
    val rules: List<ScoringRule>,
) {
    val labels: Map<FieldId, String> = fields.associate { it.id to it.label }
}

/** Base + the chosen scenario, flattened. Unknown/`null` scenario yields the base template. */
fun Template.resolve(scenarioId: String? = null): ResolvedTemplate {
    val scenario = scenarioId?.let { sid -> scenarios.firstOrNull { it.id == sid } }
    return ResolvedTemplate(
        fields = fields + (scenario?.fields ?: emptyList()),
        rules = rules + (scenario?.rules ?: emptyList()),
    )
}
