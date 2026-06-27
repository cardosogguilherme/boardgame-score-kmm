package com.example.scoring.interactors

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteractorsTest {
    @Test
    fun save_template_rejects_invalid_and_persists_valid() = runTest {
        val repo = FakeTemplateRepository()
        val save = SaveTemplate(repo)
        // DeepSea base template is valid (validate() == emptyList()).
        val errors = save(DeepSea.template)
        assertEquals(emptyList(), errors)
        assertEquals(DeepSea.template, repo.getTemplate(DeepSea.template.id))
    }

    @Test
    fun create_game_then_update_value_is_observable() = runTest {
        val repo = FakeGameRepository()
        val create = CreateGame(repo)
        val update = UpdatePlayerValue(repo)
        val id = create(
            Game(
                id = GameId("g1"),
                templateId = DeepSea.template.id,
                scenarioId = DeepSea.SCENARIO_1,
                name = "Test",
                players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap())),
            ),
        )
        update(id, PlayerId("you"), DeepSea.journal, 9)
        val game = ObserveGames(repo)().first().single()
        assertEquals(9, game.players.single().values[DeepSea.journal])
    }

    @Test
    fun finish_game_sets_status() = runTest {
        val repo = FakeGameRepository(
            listOf(Game(GameId("g1"), DeepSea.template.id, null, "T", GameStatus.IN_PROGRESS)),
        )
        FinishGame(repo)(GameId("g1"))
        assertTrue(repo.getGame(GameId("g1"))!!.status == GameStatus.FINISHED)
    }
}
