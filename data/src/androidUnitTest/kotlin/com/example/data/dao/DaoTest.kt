package com.example.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private lateinit var games: GameDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        games = db.gameDao()
    }

    @After fun tearDown() = db.close()

    @Test fun game_with_players_and_values_round_trips() = runTest {
        games.upsertGame(GameEntity("g1", "deepSea", "scenario1", "Test", "IN_PROGRESS", 1000L))
        games.upsertPlayers(listOf(PlayerEntity("you", "g1", "You", 0)))
        games.upsertValue(PlayerValueEntity("g1", "you", "journal", 9))

        assertEquals("Test", games.observeGames().first().single().name)
        assertEquals(1, games.players("g1").size)
        assertEquals(9, games.values("g1").single().value)
    }
}
