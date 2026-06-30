package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers

/**
 * Android entry point for the shared database: a file-backed instance under the app's standard
 * database directory. The Android Koin module calls this with `androidContext()`.
 */
fun appDatabase(context: Context): AppDatabase {
    val app = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(app, DB_FILE_NAME)
        .buildAppDatabase(Dispatchers.IO)
}
