package com.example.ui.model

import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewGameUiStateTest {

    // ── (a) Scenario mapping ──────────────────────────────────────────────────────────────────

    @Test fun selected_template_scenarios_are_mapped() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = DeepSea.template.id,
            selectedScenarioId = null,
            players = emptyList(),
            gameName = "",
        )
        assertEquals(1, state.templates.size)
        assertEquals(1, state.scenarios.size)
        val scenario = state.scenarios.single()
        assertEquals("scenario-1", scenario.id)
        assertEquals("Scenario 1", scenario.name)
    }

    // ── (b) canStart TRUE ────────────────────────────────────────────────────────────────────

    @Test fun canStart_true_with_template_named_player_and_game_name() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = DeepSea.template.id,
            selectedScenarioId = null,
            players = listOf(PlayerDraftUi("p1", "You")),
            gameName = "Friday",
        )
        assertTrue(state.canStart)
    }

    // ── (c) canStart FALSE cases ─────────────────────────────────────────────────────────────

    @Test fun canStart_false_when_players_empty() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = DeepSea.template.id,
            selectedScenarioId = null,
            players = emptyList(),
            gameName = "Friday",
        )
        assertFalse(state.canStart)
    }

    @Test fun canStart_false_when_a_player_name_is_blank() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = DeepSea.template.id,
            selectedScenarioId = null,
            players = listOf(PlayerDraftUi("p1", "")),
            gameName = "Friday",
        )
        assertFalse(state.canStart)
    }

    @Test fun canStart_false_when_game_name_blank() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = DeepSea.template.id,
            selectedScenarioId = null,
            players = listOf(PlayerDraftUi("p1", "You")),
            gameName = "",
        )
        assertFalse(state.canStart)
    }

    @Test fun canStart_false_when_no_template_selected() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = null,
            selectedScenarioId = null,
            players = listOf(PlayerDraftUi("p1", "You")),
            gameName = "Friday",
        )
        assertFalse(state.canStart)
    }

    // ── (d) No template selected → scenarios empty ────────────────────────────────────────────

    @Test fun no_template_selected_yields_empty_scenarios() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = null,
            selectedScenarioId = null,
            players = emptyList(),
            gameName = "",
        )
        assertTrue(state.scenarios.isEmpty())
    }

    @Test fun unknown_template_id_yields_empty_scenarios() {
        val state = newGameUiState(
            templates = listOf(DeepSea.template),
            selectedTemplateId = "does-not-exist",
            selectedScenarioId = null,
            players = emptyList(),
            gameName = "",
        )
        assertTrue(state.scenarios.isEmpty())
    }
}
