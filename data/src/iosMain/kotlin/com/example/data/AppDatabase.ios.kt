package com.example.data

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS entry point for the shared database: a file-backed instance under the app's Documents
 * directory (so it survives relaunches). The iOS Koin module calls this. Mac-only to compile/link.
 */
@OptIn(ExperimentalForeignApi::class)
fun appDatabase(): AppDatabase {
    val documents: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    ) ?: error("Could not resolve the iOS Documents directory")
    val path = requireNotNull(documents.URLByAppendingPathComponent(DB_FILE_NAME)?.path) {
        "Could not build the database file path"
    }
    return Room.databaseBuilder<AppDatabase>(name = path).buildAppDatabase(Dispatchers.Default)
}
