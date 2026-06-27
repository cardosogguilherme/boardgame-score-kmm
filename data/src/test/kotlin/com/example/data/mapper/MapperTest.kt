package com.example.data.mapper

import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {
    @Test fun template_round_trips_through_entities() {
        val (t, fields, rules) = DeepSea.template.toEntities()
        val restored = templateFrom(t, fields, rules)
        assertEquals(DeepSea.template, restored)
    }

    @Test fun game_round_trips_through_entities() {
        val originalGame = Game(
            id = GameId("game-1"),
            templateId = "deep-sea",
            scenarioId = "scenario1",
            name = "Friday Night",
            status = GameStatus.IN_PROGRESS,
            players = listOf(
                PlayerInput(
                    id = PlayerId("player-a"),
                    name = "Alice",
                    values = mapOf(
                        FieldId("rep") to 3,
                        FieldId("ins") to 7,
                    ),
                ),
                PlayerInput(
                    id = PlayerId("player-b"),
                    name = "Bob",
                    values = mapOf(
                        FieldId("rep") to 11,
                        FieldId("coo") to 2,
                    ),
                ),
            ),
        )

        val (g, players, values) = originalGame.toEntities(createdAt = 1_700_000_000_000L)
        val restored = gameFrom(g, players, values)

        assertEquals(originalGame, restored)
    }
}
