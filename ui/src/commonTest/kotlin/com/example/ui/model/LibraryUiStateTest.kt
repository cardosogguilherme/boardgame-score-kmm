package com.example.ui.model

import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryUiStateTest {
    @Test fun maps_templates_and_games_to_rows() {
        val games = listOf(
            Game(
                id = GameId("g1"),
                templateId = DeepSea.template.id,
                scenarioId = DeepSea.SCENARIO_1,
                name = "Friday",
                status = GameStatus.IN_PROGRESS,
            ),
        )
        val state = libraryUiState(listOf(DeepSea.template), games)
        assertEquals(DeepSea.template.name, state.templates.single().name)
        assertEquals("Friday", state.games.single().name)
        assertEquals(DeepSea.template.name, state.games.single().templateName)
    }
}
