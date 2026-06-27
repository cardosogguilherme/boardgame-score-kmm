package com.example.app.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.viewmodel.LibraryViewModel
import com.example.ui.DeepSeaScreenA
import com.example.ui.screens.LibraryScreen
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val HOME = "home"
    const val SCORE_ENTRY = "score_entry"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: LibraryViewModel = koinViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            LibraryScreen(
                state = state,
                onNewTemplate = { /* RuleBuilder route — Plan B */ },
                onOpenTemplate = { /* RuleBuilder route — Plan B */ },
                onResumeGame = { navController.navigate(Routes.SCORE_ENTRY) },
            )
        }
        composable(Routes.SCORE_ENTRY) {
            // Plan C binds this to a persisted game; for now the existing demo screen.
            DeepSeaScreenA()
        }
    }
}
