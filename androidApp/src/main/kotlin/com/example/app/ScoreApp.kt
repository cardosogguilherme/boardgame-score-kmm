package com.example.app

import android.app.Application
import com.example.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application entry point that starts Koin's dependency graph for the whole app.
 *
 * Koin replaces Hilt here because it carries no Android-only annotations or compiler plugin: the
 * module DSL is plain Kotlin and can move to commonMain for a future iOS target. The KMM modules
 * (:deepsea-scoring, :ui commonMain) stay framework-free; their implementations are wired in
 * [appModule].
 */
class ScoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ScoreApp)
            modules(appModule)
        }
    }
}
