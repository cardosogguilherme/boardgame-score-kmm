package com.example.ui.model

import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleBuilderUiStateTest {

    // ── (a) DeepSea base template maps to 9 rules, first rule is correct, canSave == true ──

    @Test fun deepSea_template_maps_nine_rules() {
        val state = ruleBuilderUiState(DeepSea.template)
        assertEquals(9, state.rules.size)
    }

    @Test fun deepSea_first_rule_has_correct_describe_and_tag() {
        val state = ruleBuilderUiState(DeepSea.template)
        val first = state.rules.first()
        assertEquals("Reputation: track value scores from a lookup table", first.describe)
        assertEquals("LOOKUP", first.tag)
    }

    @Test fun deepSea_template_canSave_is_true_and_errors_empty() {
        val state = ruleBuilderUiState(DeepSea.template)
        assertTrue(state.errors.isEmpty(), "Expected no validation errors but got: ${state.errors}")
        assertTrue(state.canSave)
    }

    @Test fun deepSea_palette_contains_all_five_types() {
        val state = ruleBuilderUiState(DeepSea.template)
        assertEquals(RuleTypeOption.entries.toList(), state.palette)
    }

    // ── (b) Ranking rule on a COUNT field → errors non-empty, canSave == false ──

    @Test fun ranking_rule_on_count_field_produces_errors_and_canSave_false() {
        val draft = emptyTemplateDraft("t1")
            .withFieldAdded("Discs", FieldKind.COUNT, null)
            .withRuleAdded(RuleTypeOption.RANKING, FieldId("field-1"), RuleParams(awards = listOf(8, 4)))
            .renamed("X")

        val state = ruleBuilderUiState(draft)
        assertTrue(state.errors.isNotEmpty(), "Expected validation errors for Ranking on COUNT field")
        assertFalse(state.canSave)
    }

    // ── (c) PerUnit rule on a COUNT field → tag == "×n", canSave == true ──

    @Test fun per_unit_rule_maps_to_times_n_tag_and_canSave_true() {
        val draft = emptyTemplateDraft("t2")
            .renamed("T")
            .withFieldAdded("Gold", FieldKind.COUNT, null)
            .withRuleAdded(RuleTypeOption.PER_UNIT, FieldId("field-1"), RuleParams(points = 2))

        val state = ruleBuilderUiState(draft)
        assertEquals(1, state.rules.size)
        assertEquals("×n", state.rules.single().tag)
        assertTrue(state.canSave)
    }

    // ── Additional edit-helper coverage ──

    @Test fun withFieldRemoved_removes_the_field() {
        val draft = emptyTemplateDraft("t3")
            .withFieldAdded("Gold", FieldKind.COUNT, null)
            .withFieldAdded("Silver", FieldKind.COUNT, null)
            .withFieldRemoved("field-1")

        assertEquals(1, draft.fields.size)
        assertEquals("field-2", draft.fields.single().id.raw)
    }

    @Test fun withRuleRemoved_removes_the_rule() {
        val draft = emptyTemplateDraft("t4")
            .withFieldAdded("Gold", FieldKind.COUNT, null)
            .withRuleAdded(RuleTypeOption.FLAT, FieldId("field-1"), RuleParams())
            .withRuleAdded(RuleTypeOption.PER_UNIT, FieldId("field-1"), RuleParams(points = 3))
            .withRuleRemoved("rule-1")

        assertEquals(1, draft.rules.size)
        assertEquals("rule-2", draft.rules.single().id.raw)
    }

    @Test fun emptyTemplateDraft_has_blank_name_and_no_fields_or_rules() {
        val draft = emptyTemplateDraft("blank")
        assertEquals("blank", draft.id)
        assertEquals("", draft.name)
        assertTrue(draft.fields.isEmpty())
        assertTrue(draft.rules.isEmpty())
    }

    @Test fun canSave_false_when_name_blank() {
        val draft = emptyTemplateDraft("t5")
            .withFieldAdded("Gold", FieldKind.COUNT, null)
            .withRuleAdded(RuleTypeOption.PER_UNIT, FieldId("field-1"), RuleParams(points = 1))
        // name stays blank
        assertFalse(ruleBuilderUiState(draft).canSave)
    }

    @Test fun canSave_false_when_no_rules() {
        val draft = emptyTemplateDraft("t6")
            .renamed("HasFields")
            .withFieldAdded("Gold", FieldKind.COUNT, null)
        // no rules added
        assertFalse(ruleBuilderUiState(draft).canSave)
    }

    // ── B1 empty-fields gap: name only, no fields → canSave == false ──

    @Test fun canSave_false_when_no_fields() {
        val draft = emptyTemplateDraft("x").renamed("T")
        // no fields, no rules
        assertFalse(ruleBuilderUiState(draft).canSave)
    }
}
