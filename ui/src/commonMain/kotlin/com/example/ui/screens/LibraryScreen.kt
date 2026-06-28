package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.model.GameRow
import com.example.ui.model.LibraryUiState
import com.example.ui.model.TemplateRow

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    // Host-driven: the library route wires this to the Home "+" FAB, so the list body itself never
    // invokes it. Kept in the signature so the screen is the single contract for the route.
    onNewTemplate: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onResumeGame: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text("Templates") }
        if (state.templates.isEmpty()) {
            item { Text("No templates yet — tap + to create one.") }
        } else {
            items(state.templates, key = { it.id }) { row -> TemplateCard(row, onOpenTemplate) }
        }
        item { Text("Games") }
        if (state.games.isEmpty()) {
            item { Text("No games yet.") }
        } else {
            items(state.games, key = { it.id }) { row -> GameCard(row, onResumeGame) }
        }
    }
}

@Composable
private fun TemplateCard(row: TemplateRow, onOpen: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(row.id) }) {
        Text(row.name, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun GameCard(row: GameRow, onResume: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onResume(row.id) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(row.name)
            Text("${row.templateName} · ${row.status}")
        }
    }
}
