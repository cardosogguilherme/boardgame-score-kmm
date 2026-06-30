package com.example.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/** File name of the on-device database, shared by every platform's builder. */
const val DB_FILE_NAME: String = "boardgame-score.db"

/**
 * Shared Room configuration applied on every platform: the bundled native SQLite driver (so the
 * exact same engine ships on Android and iOS) plus the coroutine context queries run on. The
 * context is passed in because `Dispatchers.IO` is not available in commonMain — each platform
 * supplies it (both Android and Kotlin/Native have it).
 */
internal fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(
    queryContext: CoroutineContext,
): AppDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(queryContext)
        .build()
