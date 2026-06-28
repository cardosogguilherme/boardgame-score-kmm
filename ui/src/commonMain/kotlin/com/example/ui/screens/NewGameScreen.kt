package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.model.NewGameUiState
import com.example.ui.model.PlayerDraftUi

/**
 * Stateless New Game screen. All business state lives upstream (ViewModel / parent);
 * this composable holds no local business state — it renders entirely from [state] and
 * emits typed callbacks.
 */
@Composable
fun NewGameScreen(
    state: NewGameUiState,
    onSelectTemplate: (String) -> Unit,
    onSelectScenario: (String?) -> Unit,
    onGameNameChange: (String) -> Unit,
    onAddPlayer: () -> Unit,
    onPlayerNameChange: (tempId: String, name: String) -> Unit,
    onRemovePlayer: (tempId: String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Template picker ────────────────────────────────────────────────────
        item { SectionHeader("Template") }

        if (state.templates.isEmpty()) {
            item {
                Text(
                    text = "No templates yet — create one with +",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            items(state.templates, key = { it.id }) { template ->
                SelectableRow(
                    label = template.name,
                    selected = template.id == state.selectedTemplateId,
                    onClick = { onSelectTemplate(template.id) },
                )
            }
        }

        // ── Scenario picker (only when the selected template has scenarios) ────
        if (state.scenarios.isNotEmpty()) {
            item { SectionHeader("Scenario") }
            item {
                SelectableRow(
                    label = "None",
                    selected = state.selectedScenarioId == null,
                    onClick = { onSelectScenario(null) },
                )
            }
            items(state.scenarios, key = { it.id }) { scenario ->
                SelectableRow(
                    label = scenario.name,
                    selected = scenario.id == state.selectedScenarioId,
                    onClick = { onSelectScenario(scenario.id) },
                )
            }
        }

        // ── Game name ──────────────────────────────────────────────────────────
        item { SectionHeader("Game name") }
        item {
            OutlinedTextField(
                value = state.gameName,
                onValueChange = onGameNameChange,
                label = { Text("Game name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // ── Players ────────────────────────────────────────────────────────────
        item { SectionHeader("Players") }
        items(state.players, key = { it.tempId }) { player ->
            PlayerRow(
                player = player,
                onNameChange = { name -> onPlayerNameChange(player.tempId, name) },
                onRemove = { onRemovePlayer(player.tempId) },
            )
        }
        item {
            TextButton(
                onClick = onAddPlayer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("+ Add player")
            }
        }

        // ── Start game ─────────────────────────────────────────────────────────
        item {
            Button(
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Start game")
            }
        }
    }
}

// ── Section header ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

// ── Selectable row ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

// ── Player row ─────────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayerRow(
    player: PlayerDraftUi,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = player.name,
                onValueChange = onNameChange,
                label = { Text("Player name") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            TextButton(onClick = onRemove) { Text("✕") }
        }
    }
}
