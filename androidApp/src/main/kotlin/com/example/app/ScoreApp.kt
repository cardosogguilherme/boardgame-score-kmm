package com.example.app

import android.app.Application
import com.example.app.di.initKoin
import org.koin.android.ext.koin.androidContext

/**
 * Application entry point that starts the shared Koin graph. The graph (platform + shared modules)
 * lives in :shared so it can be reused by the iOS entry point; here we only supply the Android
 * Context the platform module needs.
 */
class ScoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ScoreApp)
        }
    }
}
