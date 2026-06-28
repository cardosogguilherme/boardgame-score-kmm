package com.example.app.di

import android.content.Context
import com.example.scoring.model.GameId
import kotlin.test.Test
import org.koin.test.verify.verify

/**
 * Verifies that the Koin graph is consistent at test time rather than crashing at app launch.
 *
 * Context is declared as an extra type because it is supplied by Koin's Android integration
 * (androidContext()) rather than being wired as a provider inside appModule itself. GameId is
 * declared for the same reason: it is a runtime parameter to ScoreEntryViewModel (injected via
 * parametersOf at the call site), not a graph-resolved dependency.
 */
class AppModuleTest {
    @Test
    fun appModuleIsConsistent() {
        appModule.verify(extraTypes = listOf(Context::class, GameId::class))
    }
}
