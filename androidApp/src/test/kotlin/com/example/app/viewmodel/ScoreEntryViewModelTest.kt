package com.example.app.viewmodel

import com.example.app.testing.FakeGameRepository
import com.example.app.testing.FakeTemplateRepository
import com.example.scoring.interactors.FinishGame
import com.example.scoring.interactors.GetGame
import com.example.scoring.interactors.GetTemplate
import com.example.scoring.interactors.UpdatePlayerValue
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScoreEntryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val gameId = GameId("g1")
    private val you = PlayerId("you")

    private fun seededGame() = Game(
        id = gameId,
        templateId = DeepSea.template.id,
        scenarioId = DeepSea.SCENARIO_1,
        name = "Friday",
        players = listOf(
            PlayerInput(you, "You", mapOf(DeepSea.journal to 9)),
        ),
    )

    private fun buildVm(games: FakeGameRepository): ScoreEntryViewModel = ScoreEntryViewModel(
        gameId = gameId,
        getGame = GetGame(games),
        getTemplate = GetTemplate(FakeTemplateRepository(listOf(DeepSea.template))),
        updatePlayerValue = UpdatePlayerValue(games),
        finishGame = FinishGame(games),
    )

    @Test
    fun increment_autosaves_and_raises_total_by_one() = runTest {
        val games = FakeGameRepository(listOf(seededGame()))
        val vm = buildVm(games)

        // Wait for load.
        val loaded = vm.uiState.first { it.players.isNotEmpty() }
        val before = loaded.total

        // journal is a FLAT rule → +1 point per unit.
        vm.onIncrement(DeepSea.journal, you)
        advanceUntilIdle()

        // (a) persisted to the repository
        val persisted = games.getGame(gameId)!!.players.single().values[DeepSea.journal]
        assertEquals(10, persisted, "UpdatePlayerValue must persist the incremented value")

        // (b) uiState total increased by exactly 1
        val after = vm.uiState.first { it.total == before + 1 }
        assertEquals(before + 1, after.total)
    }

    @Test
    fun decrements_clamp_at_zero() = runTest {
        val games = FakeGameRepository(listOf(seededGame()))
        val vm = buildVm(games)
        vm.uiState.first { it.players.isNotEmpty() }

        repeat(50) { vm.onDecrement(DeepSea.journal, you) }
        advanceUntilIdle()

        val persisted = games.getGame(gameId)!!.players.single().values[DeepSea.journal]
        assertEquals(0, persisted, "Repeated decrements must floor the field at 0")
    }
}
