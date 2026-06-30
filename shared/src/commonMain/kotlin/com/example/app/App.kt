package com.example.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.app.nav.AppNavHost
import org.koin.compose.KoinContext

/**
 * The whole app UI, shared across Android and iOS. [KoinContext] binds the started Koin instance to
 * the composition so `koinViewModel()` inside the nav graph resolves. Hosts call this after
 * `initKoin()` (Android: MainActivity.setContent; iOS: the ComposeUIViewController entry point).
 */
@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            AppNavHost()
        }
    }
}
