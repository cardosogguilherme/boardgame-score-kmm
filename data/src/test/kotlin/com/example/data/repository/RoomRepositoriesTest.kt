package com.example.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    @Test fun saved_template_is_observable_and_equal() = runTest {
        val repo = RoomTemplateRepository(db.templateDao())
        repo.saveTemplate(DeepSea.template)
        assertEquals(DeepSea.template, repo.observeTemplates().first().single())
    }

    @Test fun created_game_then_updated_value_persists() = runTest {
        val repo = RoomGameRepository(db.gameDao()) { 1000L }
        val id = repo.createGame(
            Game(GameId("g1"), DeepSea.template.id, DeepSea.SCENARIO_1, "Test",
                players = listOf(PlayerInput(PlayerId("you"), "You", emptyMap()))),
        )
        repo.updatePlayerValue(id, PlayerId("you"), DeepSea.journal, 9)
        assertEquals(9, repo.getGame(id)!!.players.single().values[DeepSea.journal])
    }
}
