package com.example.app.di

import com.example.data.AppDatabase
import com.example.data.appDatabase
import com.example.data.repository.RoomGameRepository
import com.example.data.repository.RoomTemplateRepository
import com.example.scoring.interactors.CreateGame
import com.example.scoring.interactors.DeleteTemplate
import com.example.scoring.interactors.FinishGame
import com.example.scoring.interactors.GetGame
import com.example.scoring.interactors.GetTemplate
import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.interactors.SaveTemplate
import com.example.scoring.interactors.UpdatePlayerValue
import com.example.scoring.repository.GameRepository
import com.example.scoring.repository.TemplateRepository
import com.example.app.viewmodel.LibraryViewModel
import com.example.app.viewmodel.NewGameViewModel
import com.example.app.viewmodel.RuleBuilderViewModel
import com.example.app.viewmodel.ScoreEntryViewModel
import com.example.scoring.model.GameId
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
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
    // Database — one instance for the whole app lifetime. Built by the :data Android factory
    // (Room 2.7 KMP + bundled SQLite driver); the iOS module will bind appDatabase() instead.
    single { appDatabase(androidContext()) }

    // Repositories — bound as their domain interfaces so screens stay framework-free
    single<TemplateRepository> { RoomTemplateRepository(get<AppDatabase>().templateDao()) }
    single<GameRepository> { RoomGameRepository(get<AppDatabase>().gameDao(), now = System::currentTimeMillis) }

    // Template interactors
    factory { ObserveTemplates(get()) }
    factory { GetTemplate(get()) }
    factory { SaveTemplate(get()) }
    factory { DeleteTemplate(get()) }

    // Game interactors
    factory { ObserveGames(get()) }
    factory { GetGame(get()) }
    factory { CreateGame(get()) }
    factory { UpdatePlayerValue(get()) }
    factory { FinishGame(get()) }

    // ViewModels
    viewModel { LibraryViewModel(get(), get()) }
    viewModel { (templateId: String?) -> RuleBuilderViewModel(get(), get(), templateId) }
    viewModel { NewGameViewModel(get(), get()) }
    viewModel { (gameId: GameId) -> ScoreEntryViewModel(gameId, get(), get(), get(), get()) }
}
