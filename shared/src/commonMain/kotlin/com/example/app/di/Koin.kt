package com.example.app.di

import com.example.app.viewmodel.LibraryViewModel
import com.example.app.viewmodel.NewGameViewModel
import com.example.app.viewmodel.RuleBuilderViewModel
import com.example.app.viewmodel.ScoreEntryViewModel
import com.example.data.AppDatabase
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
import com.example.scoring.model.GameId
import com.example.scoring.repository.GameRepository
import com.example.scoring.repository.TemplateRepository
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Platform-specific half of the graph. Each platform supplies the [AppDatabase] (Android needs a
 * Context, iOS a Documents-directory path) and the wall clock `() -> Long` (commonMain has no
 * `System.currentTimeMillis`). The same [sharedModule] runs on both, fed by this.
 */
expect fun platformModule(): Module

/**
 * Platform-agnostic half of the graph: repositories bound as their domain interfaces, the
 * interactors, and the screen ViewModels. The Room DAOs are taken from the platform [AppDatabase].
 */
val sharedModule: Module = module {
    // Repositories — bound as domain interfaces so screens stay framework-free.
    single<TemplateRepository> { RoomTemplateRepository(get<AppDatabase>().templateDao()) }
    single<GameRepository> { RoomGameRepository(get<AppDatabase>().gameDao(), now = get()) }

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

/**
 * Starts Koin with the platform + shared modules. Hosts call this once at launch:
 * Android passes `androidContext(...)` via [appDeclaration]; iOS calls it with no declaration.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule(), sharedModule)
}
