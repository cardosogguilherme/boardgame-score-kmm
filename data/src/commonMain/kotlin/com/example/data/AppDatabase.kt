package com.example.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
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
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun gameDao(): GameDao
}

// Room KSP generates the platform `actual` for this object on each target, replacing the reflective
// instantiation the Android-only build relied on. Declaring it `expect` is what lets the database be
// constructed from commonMain across both Android and iOS.
@Suppress("KotlinNoActualForExpectDeclaration", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
