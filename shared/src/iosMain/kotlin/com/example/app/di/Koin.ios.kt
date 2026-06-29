package com.example.app.di

import com.example.data.AppDatabase
import com.example.data.appDatabase
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDate

/** iOS platform bindings: the Room database (file-backed) and the system wall clock. Mac-only. */
actual fun platformModule(): Module = module {
    single<AppDatabase> { appDatabase() }
    single<() -> Long> { { (NSDate().timeIntervalSince1970 * 1000.0).toLong() } }
}
