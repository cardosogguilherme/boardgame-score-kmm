package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game ORDER BY createdAt DESC")
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game WHERE id = :id")
    suspend fun getGame(id: String): GameEntity?

    @Query("SELECT * FROM player WHERE gameId = :id ORDER BY position")
    suspend fun players(id: String): List<PlayerEntity>

    @Query("SELECT * FROM player_value WHERE gameId = :id")
    suspend fun values(id: String): List<PlayerValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayer(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertValue(value: PlayerValueEntity)

    @Query("UPDATE game SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("DELETE FROM player WHERE gameId = :gameId AND id = :playerId")
    suspend fun deletePlayer(gameId: String, playerId: String)

    @Query("DELETE FROM game WHERE id = :id")
    suspend fun deleteGame(id: String)
}
