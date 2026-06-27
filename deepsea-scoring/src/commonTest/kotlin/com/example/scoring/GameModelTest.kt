package com.example.scoring

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GameModelTest {
    @Test
    fun game_round_trips_through_json() {
        val game = Game(
            id = GameId("g1"),
            templateId = "deepSea",
            scenarioId = "scenario1",
            name = "Friday night",
            status = GameStatus.IN_PROGRESS,
            players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap())),
        )
        val json = Json { prettyPrint = false }
        val decoded = json.decodeFromString(Game.serializer(), json.encodeToString(Game.serializer(), game))
        assertEquals(game, decoded)
    }
}
