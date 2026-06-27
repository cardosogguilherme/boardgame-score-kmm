package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.dao.GameDao
import com.example.data.dao.TemplateDao
import com.example.data.entity.FieldEntity
import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity

@Database(
    entities = [
        TemplateEntity::class, FieldEntity::class, RuleEntity::class,
        GameEntity::class, PlayerEntity::class, PlayerValueEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun gameDao(): GameDao
}
