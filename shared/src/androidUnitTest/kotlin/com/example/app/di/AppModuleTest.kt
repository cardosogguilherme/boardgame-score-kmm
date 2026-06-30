package com.example.app.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.scoring.model.GameId
import kotlin.test.Test
import org.koin.test.verify.verify

/**
 * Verifies the Koin graph is internally consistent at test time rather than crashing at launch.
 * koin-test's verify() is JVM-only, so this runs on the Android target.
 *
 * The graph is split: [platformModule] provides the AppDatabase + the `() -> Long` clock, and
 * [sharedModule] wires the repositories, interactors, and ViewModels on top. Each is checked with
 * the types the *other* side supplies declared as extras:
 *  - sharedModule: AppDatabase + the clock (Function0) come from the platform; GameId is a runtime
 *    parameter to ScoreEntryViewModel.
 *  - platformModule (Android): Context comes from androidContext() at startup.
 */
class AppModuleTest {
    @Test
    fun sharedGraphIsConsistent() {
        sharedModule.verify(
            extraTypes = listOf(AppDatabase::class, GameId::class, Function0::class),
        )
    }

    @Test
    fun androidPlatformGraphIsConsistent() {
        platformModule().verify(extraTypes = listOf(Context::class))
    }
}
