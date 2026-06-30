package com.example.app.di

import com.example.data.AppDatabase
import com.example.data.appDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android platform bindings: the Room database (needs a Context) and the system wall clock. */
actual fun platformModule(): Module = module {
    single<AppDatabase> { appDatabase(androidContext()) }
    single<() -> Long> { { System.currentTimeMillis() } }
}
