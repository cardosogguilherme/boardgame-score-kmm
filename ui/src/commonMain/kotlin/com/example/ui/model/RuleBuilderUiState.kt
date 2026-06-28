package com.example.ui.model

import com.example.scoring.engine.describe
import com.example.scoring.engine.typeTag
import com.example.scoring.engine.validate
import com.example.scoring.model.Field
import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.scoring.model.RuleId
import com.example.scoring.model.ScoringRule
import com.example.scoring.model.Template
import com.example.scoring.model.resolve

// ── Palette ──────────────────────────────────────────────────────────────────────────────────

enum class RuleTypeOption(val label: String, val hint: String) {
    LOOKUP("Lookup", "a field value maps to points via a lookup table"),
    PER_UNIT("Per Unit", "use when each unit of a resource scores a fixed amount"),
    FLAT("Flat", "use when the field value scores directly"),
    PER_N("Per N", "use when you earn 1 point per N units"),
    RANKING("Ranking", "use when players are ranked against each other for awards"),
}

// ── Row types ─────────────────────────────────────────────────────────────────────────────────

data class FieldRowUi(
    val id: String,
    val label: String,
    val kind: FieldKind,
    val max: Int?,
)

data class RuleRowUi(
    val id: String,
    val describe: String,
    val tag: String,
)

// ── Screen-level state ────────────────────────────────────────────────────────────────────────

data class RuleBuilderUiState(
    val templateId: String,
    val name: String,
    val fields: List<FieldRowUi>,
    val rules: List<RuleRowUi>,
    val palette: List<RuleTypeOption>,
    val errors: List<String>,
    val canSave: Boolean,
)

// ── Mapper ────────────────────────────────────────────────────────────────────────────────────

fun ruleBuilderUiState(draft: Template): RuleBuilderUiState {
    val resolved = draft.resolve()
    val labels = resolved.labels
    val errors = resolved.validate()
    return RuleBuilderUiState(
        templateId = draft.id,
        name = draft.name,
        fields = draft.fields.map { FieldRowUi(it.id.raw, it.label, it.kind, it.max) },
        rules = draft.rules.map { RuleRowUi(it.id.raw, it.describe(labels), it.typeTag()) },
        palette = RuleTypeOption.entries.toList(),
        errors = errors,
        canSave = errors.isEmpty()
            && draft.name.isNotBlank()
            && draft.fields.isNotEmpty()
            && draft.rules.isNotEmpty(),
    )
}

// ── Rule construction params ──────────────────────────────────────────────────────────────────

data class RuleParams(
    val points: Int = 1,
    val divisor: Int = 1,
    val table: List<Int> = emptyList(),
    val awards: List<Int> = emptyList(),
    val group: String? = null,
)

// ── Collision-safe id generators ──────────────────────────────────────────────────────────────

/** Returns the smallest N≥1 such that "field-N" is not already in [existingIds]. */
private fun nextFieldId(existingIds: Set<String>): FieldId {
    var n = 1
    while ("field-$n" in existingIds) n++
    return FieldId("field-$n")
}

/** Returns the smallest N≥1 such that "rule-N" is not already in [existingIds]. */
private fun nextRuleId(existingIds: Set<String>): RuleId {
    var n = 1
    while ("rule-$n" in existingIds) n++
    return RuleId("rule-$n")
}

// ── Pure edit helpers (return new Template; never mutate) ─────────────────────────────────────

fun Template.renamed(name: String): Template = copy(name = name)

fun Template.withFieldAdded(label: String, kind: FieldKind, max: Int?): Template {
    val newId = nextFieldId(fields.map { it.id.raw }.toSet())
    return copy(fields = fields + Field(id = newId, label = label, kind = kind, max = max))
}

fun Template.withFieldRemoved(fieldId: String): Template =
    copy(fields = fields.filter { it.id.raw != fieldId })

fun Template.withRuleRemoved(ruleId: String): Template =
    copy(rules = rules.filter { it.id.raw != ruleId })

fun Template.withRuleAdded(
    type: RuleTypeOption,
    fieldId: FieldId,
    params: RuleParams,
): Template {
    val newId = nextRuleId(rules.map { it.id.raw }.toSet())
    val rule: ScoringRule = when (type) {
        RuleTypeOption.LOOKUP -> ScoringRule.Lookup(newId, fieldId, params.table, params.group)
        RuleTypeOption.PER_UNIT -> ScoringRule.PerUnit(newId, fieldId, params.points, params.group)
        RuleTypeOption.FLAT -> ScoringRule.Flat(newId, fieldId, params.group)
        RuleTypeOption.PER_N -> ScoringRule.PerN(newId, fieldId, params.divisor, params.group)
        RuleTypeOption.RANKING -> ScoringRule.Ranking(newId, fieldId, params.awards, params.group)
    }
    return copy(rules = rules + rule)
}

// ── Factory ───────────────────────────────────────────────────────────────────────────────────

fun emptyTemplateDraft(id: String): Template =
    Template(id = id, name = "", fields = emptyList(), rules = emptyList())
