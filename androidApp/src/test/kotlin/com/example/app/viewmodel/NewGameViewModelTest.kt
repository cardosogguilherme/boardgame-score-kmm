package com.example.app.viewmodel

import com.example.app.testing.FakeGameRepository
import com.example.app.testing.FakeTemplateRepository
import com.example.scoring.interactors.CreateGame
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.model.GameId
import com.example.scoring.sample.DeepSea
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
class NewGameViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(
        fakeTemplates: FakeTemplateRepository = FakeTemplateRepository(listOf(DeepSea.template)),
        fakeGames: FakeGameRepository = FakeGameRepository(),
    ): Triple<NewGameViewModel, FakeTemplateRepository, FakeGameRepository> {
        val vm = NewGameViewModel(ObserveTemplates(fakeTemplates), CreateGame(fakeGames))
        return Triple(vm, fakeTemplates, fakeGames)
    }

    // ── (a) templates surface in uiState ──────────────────────────────────────────────────────

    @Test
    fun templates_surface_in_uiState() = runTest {
        val (vm) = buildVm()
        val state = vm.uiState.first { it.templates.isNotEmpty() }
        assertEquals(1, state.templates.size)
        assertEquals(DeepSea.template.id, state.templates.single().id)
        assertEquals(DeepSea.template.name, state.templates.single().name)
    }

    // ── (b) full happy path ───────────────────────────────────────────────────────────────────

    @Test
    fun full_flow_creates_game_with_correct_template_player_and_name() = runTest {
        val (vm, _, fakeGames) = buildVm()

        // Wait for templates to surface
        vm.uiState.first { it.templates.isNotEmpty() }

        vm.onSelectTemplate(DeepSea.template.id)
        vm.onAddPlayer()

        // Retrieve the tempId of the added player from current state
        val tempId = vm.uiState.first { it.players.isNotEmpty() }.players.single().tempId
        vm.onPlayerNameChange(tempId, "You")
        vm.onGameNameChange("Friday")

        // Assert canStart is true
        val stateBeforeStart = vm.uiState.first { it.canStart }
        assertTrue(stateBeforeStart.canStart, "canStart should be true when template, player, and game name are set")

        var capturedId: GameId? = null
        vm.start { capturedId = it }
        advanceUntilIdle()

        assertNotNull(capturedId, "onCreated callback must be invoked with a GameId")

        val game = fakeGames.getGame(capturedId!!)
        assertNotNull(game, "Game must be persisted in the fake repository")
        assertEquals(DeepSea.template.id, game.templateId, "Game templateId must match selected template")
        assertEquals("Friday", game.name, "Game name must match the entered name")
        assertEquals(1, game.players.size, "Game must have exactly 1 player")
        assertEquals("You", game.players.single().name, "Player name must match the entered name")
    }

    // ── (c) start() does nothing when !canStart ───────────────────────────────────────────────

    @Test
    fun start_is_noop_when_cannot_start() = runTest {
        val (vm, _, fakeGames) = buildVm()

        // No template, no players, no name → canStart == false
        assertFalse(vm.uiState.value.canStart, "canStart must be false with no setup")

        var called = false
        vm.start { called = true }
        advanceUntilIdle()

        assertFalse(called, "onCreated callback must not be invoked when canStart is false")
        val games = fakeGames.observeGames().first()
        assertTrue(games.isEmpty(), "No game should be persisted when canStart is false")
    }

    // ── (d) onSelectTemplate resets selectedScenarioId ───────────────────────────────────────

    @Test
    fun selecting_template_resets_scenario_selection() = runTest {
        val (vm) = buildVm()

        vm.uiState.first { it.templates.isNotEmpty() }

        // Select template then a scenario from it
        vm.onSelectTemplate(DeepSea.template.id)
        vm.onSelectScenario(DeepSea.SCENARIO_1)

        val stateWithScenario = vm.uiState.first { it.selectedScenarioId != null }
        assertEquals(DeepSea.SCENARIO_1, stateWithScenario.selectedScenarioId)

        // Re-selecting any template must reset scenarioId to null
        vm.onSelectTemplate(DeepSea.template.id)

        val stateAfterReselect = vm.uiState.first { it.selectedTemplateId == DeepSea.template.id && it.selectedScenarioId == null }
        assertNull(stateAfterReselect.selectedScenarioId, "Scenario selection must be reset when a new template is selected")
    }

    // ── (e) added player gets a default non-blank name so Start unlocks without naming ──────────

    @Test
    fun added_player_gets_default_name_and_unlocks_start() = runTest {
        val (vm) = buildVm()
        vm.uiState.first { it.templates.isNotEmpty() }

        vm.onSelectTemplate(DeepSea.template.id)
        vm.onGameNameChange("Friday")
        vm.onAddPlayer()

        val state = vm.uiState.first { it.players.isNotEmpty() }
        assertEquals("Player 1", state.players.single().name, "A new player should get a default non-blank name")
        assertTrue(state.canStart, "Start must unlock without the user editing the player name")
    }
}
