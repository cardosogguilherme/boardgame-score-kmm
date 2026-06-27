package com.example.app.di

import org.koin.dsl.module

/**
 * The app's Koin dependency graph.
 *
 * Kept as a plain Koin module (no Android-only annotations), so once an iOS target exists the
 * shared bindings can move into a commonMain module and only the platform-specific providers
 * (e.g. the Room-backed repositories, which are Android-only today) stay here.
 *
 * Bindings are added as the pieces they wire land: the Room-backed repositories (Task 7), the
 * domain interactors, and the screen ViewModels (Task 10).
 */
val appModule = module {
}
