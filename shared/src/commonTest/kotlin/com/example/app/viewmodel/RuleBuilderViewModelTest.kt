package com.example.app.viewmodel

import com.example.app.testing.FakeTemplateRepository
import com.example.scoring.interactors.GetTemplate
import com.example.scoring.interactors.SaveTemplate
import com.example.scoring.model.FieldKind
import com.example.ui.model.RuleParams
import com.example.ui.model.RuleTypeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RuleBuilderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── (a) Valid new template: name + field + rule → canSave; save persists + invokes callback ──

    @Test
    fun new_template_with_valid_content_persists_and_invokes_callback() = runTest {
        val fakeRepo = FakeTemplateRepository()
        val vm = RuleBuilderViewModel(
            getTemplate = GetTemplate(fakeRepo),
            saveTemplate = SaveTemplate(fakeRepo),
            templateId = null,
        )

        vm.onNameChange("My Game")
        vm.onAddField("Gold", FieldKind.COUNT, null)
        vm.onAddRule(RuleTypeOption.PER_UNIT, "field-1", RuleParams(points = 2))

        // Verify canSave is true before saving
        val state = vm.uiState.first { it.canSave }
        assertTrue(state.canSave)

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertTrue(saved, "onSaved callback should have been invoked for a valid template")
        assertNotNull(
            fakeRepo.getTemplate(vm.uiState.value.templateId),
            "Template should be persisted to the repository",
        )
    }

    // ── (b) Invalid draft (Ranking on COUNT field): save must NOT persist and NOT invoke callback ──

    @Test
    fun invalid_draft_does_not_persist_and_does_not_invoke_callback() = runTest {
        val fakeRepo = FakeTemplateRepository()
        val vm = RuleBuilderViewModel(
            getTemplate = GetTemplate(fakeRepo),
            saveTemplate = SaveTemplate(fakeRepo),
            templateId = null,
        )

        // Ranking rule on COUNT field → fails validation
        vm.onNameChange("Bad Game")
        vm.onAddField("Discs", FieldKind.COUNT, null)
        vm.onAddRule(RuleTypeOption.RANKING, "field-1", RuleParams(awards = listOf(8, 4)))

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertFalse(saved, "onSaved callback should NOT be invoked when validation fails")
        assertNull(
            fakeRepo.getTemplate(vm.uiState.value.templateId),
            "Invalid template should NOT be persisted to the repository",
        )
    }

    // ── (c) Collision-safe id regression: add A → add B → remove A → add C → ids stay distinct ──

    @Test
    fun field_ids_stay_distinct_after_remove_then_add() = runTest {
        val fakeRepo = FakeTemplateRepository()
        val vm = RuleBuilderViewModel(
            getTemplate = GetTemplate(fakeRepo),
            saveTemplate = SaveTemplate(fakeRepo),
            templateId = null,
        )

        vm.onAddField("A", FieldKind.COUNT, null)  // → field-1
        vm.onAddField("B", FieldKind.COUNT, null)  // → field-2
        vm.onRemoveField("field-1")                 // remove A; only field-2 remains
        vm.onAddField("C", FieldKind.COUNT, null)   // must NOT reuse field-2

        // Subscribing yields the up-to-date draft (2 fields)
        val state = vm.uiState.first { it.fields.size == 2 }

        assertEquals(2, state.fields.size)
        val ids = state.fields.map { it.id }
        assertEquals(2, ids.toSet().size, "Field ids must be unique after remove-then-add; got: $ids")
    }
}
