package com.example.app.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app.viewmodel.LibraryViewModel
import com.example.app.viewmodel.NewGameViewModel
import com.example.app.viewmodel.RuleBuilderViewModel
import com.example.app.viewmodel.ScoreEntryViewModel
import com.example.scoring.model.GameId
import com.example.ui.ScoreEntryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NewGameScreen
import com.example.ui.screens.RuleBuilderScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

object Routes {
    const val HOME = "home"
    const val SCORE_ENTRY = "score_entry"
    const val RULE_BUILDER = "rule_builder"
    const val NEW_GAME = "new_game"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {

        // ── Home ─────────────────────────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            val vm: LibraryViewModel = koinViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Board Game Score") },
                        actions = {
                            TextButton(onClick = { navController.navigate(Routes.NEW_GAME) }) {
                                Text("New game")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { navController.navigate(Routes.RULE_BUILDER) }) {
                        Text("+")
                    }
                },
            ) { innerPadding ->
                LibraryScreen(
                    state = state,
                    onNewTemplate = { navController.navigate(Routes.RULE_BUILDER) },
                    onOpenTemplate = { id -> navController.navigate("rule_builder?templateId=$id") },
                    onResumeGame = { id -> navController.navigate("${Routes.SCORE_ENTRY}/$id") },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        // ── Rule builder ─────────────────────────────────────────────────────────────────────────
        // Navigating to bare "rule_builder" yields templateId = null (new template).
        // Navigating to "rule_builder?templateId=<id>" opens an existing template for editing.
        composable(
            route = "rule_builder?templateId={templateId}",
            arguments = listOf(
                navArgument("templateId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")
            val vm: RuleBuilderViewModel = koinViewModel { parametersOf(templateId) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(if (templateId == null) "New template" else "Edit template")
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Text("<")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                RuleBuilderScreen(
                    state = state,
                    onNameChange = vm::onNameChange,
                    onAddField = vm::onAddField,
                    onRemoveField = vm::onRemoveField,
                    onAddRule = vm::onAddRule,
                    onRemoveRule = vm::onRemoveRule,
                    onSave = { vm.save { navController.popBackStack() } },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        // ── New game ──────────────────────────────────────────────────────────────────────────────
        composable(Routes.NEW_GAME) {
            val vm: NewGameViewModel = koinViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("New game") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Text("<")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                NewGameScreen(
                    state = state,
                    onSelectTemplate = vm::onSelectTemplate,
                    onSelectScenario = vm::onSelectScenario,
                    onGameNameChange = vm::onGameNameChange,
                    onAddPlayer = vm::onAddPlayer,
                    onPlayerNameChange = vm::onPlayerNameChange,
                    onRemovePlayer = vm::onRemovePlayer,
                    onStart = {
                        vm.start { gameId ->
                            navController.navigate("${Routes.SCORE_ENTRY}/${gameId.raw}") {
                                popUpTo(Routes.HOME)
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        // ── Score entry ───────────────────────────────────────────────────────────────────────────
        // Parametrized by gameId so both "New Game → start" and "Library → Resume" land here.
        composable(
            route = "${Routes.SCORE_ENTRY}/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val rawGameId = backStackEntry.arguments?.getString("gameId")!!
            val vm: ScoreEntryViewModel = koinViewModel { parametersOf(GameId(rawGameId)) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Score entry") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Text("<")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                vm.finish()
                                navController.popBackStack()
                            }) {
                                Text("Finish")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                ScoreEntryScreen(
                    state = state,
                    onSelectPlayer = vm::onSelectPlayer,
                    onIncrement = vm::onIncrement,
                    onDecrement = vm::onDecrement,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}
