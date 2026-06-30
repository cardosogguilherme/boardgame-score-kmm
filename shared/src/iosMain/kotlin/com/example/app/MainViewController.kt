package com.example.app

import androidx.compose.ui.window.ComposeUIViewController
import com.example.app.di.initKoin
import platform.UIKit.UIViewController

/**
 * iOS entry point. The Swift `iosApp` wraps this `UIViewController` in a
 * `UIViewControllerRepresentable`. Koin is started once via the controller's `configure` block
 * (guarded so re-creation is safe), then the same shared [App] composable that runs on Android
 * renders here. Mac-only to compile/link.
 */
fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { ensureKoin() },
) {
    App()
}

private var koinStarted = false

private fun ensureKoin() {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
}
