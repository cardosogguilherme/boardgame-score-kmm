package com.example.app.di

import android.content.Context
import kotlin.test.Test
import org.koin.test.verify.verify

/**
 * Verifies that the Koin graph is consistent at test time rather than crashing at app launch.
 *
 * Context is declared as an extra type because it is supplied by Koin's Android integration
 * (androidContext()) rather than being wired as a provider inside appModule itself.
 */
class AppModuleTest {
    @Test
    fun appModuleIsConsistent() {
        appModule.verify(extraTypes = listOf(Context::class))
    }
}
