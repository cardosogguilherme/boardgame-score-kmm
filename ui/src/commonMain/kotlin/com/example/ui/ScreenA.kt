package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerId
import com.example.ui.model.PlayerChip
import com.example.ui.model.RankedColumn
import com.example.ui.model.RuleRow
import com.example.ui.model.ScoreEntryUiState
import com.example.ui.model.Section

/**
 * Screen A — Score entry. Stateless: driven entirely by a [ScoreEntryUiState] snapshot and a set of
 * intent callbacks. Each rule renders as a row: label · stepper · live points · rule-type tag.
 * Numeric input is steppers only — never a text field (brief §7). The ranked-goal section spans
 * all players and resolves rank live. Edits flow out via [onIncrement]/[onDecrement]; the caller
 * (holder or ViewModel) owns clamping + recomputation and feeds back a fresh state.
 */
@Composable
fun ScoreEntryScreen(
    state: ScoreEntryUiState,
    onSelectPlayer: (PlayerId) -> Unit,
    onIncrement: (FieldId, PlayerId) -> Unit,
    onDecrement: (FieldId, PlayerId) -> Unit,
    title: String = state.title,
    modifier: Modifier = Modifier,
) {
    // Per-player rows edit the active player's sheet; the active id comes from the chips.
    val activeId = state.players.firstOrNull { it.isActive }?.id ?: state.players.firstOrNull()?.id

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(12.dp))

        PlayerSelector(players = state.players, onSelect = onSelectPlayer)
        Spacer(Modifier.size(12.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.sections) { section ->
                when (section) {
                    is Section.PerPlayer ->
                        PerPlayerSection(section, activeId, onIncrement, onDecrement)
                    is Section.Ranked -> RankedSection(section, onIncrement, onDecrement)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ScoreFooter(state)
    }
}

@Composable
private fun PerPlayerSection(
    section: Section.PerPlayer,
    activeId: PlayerId?,
    onIncrement: (FieldId, PlayerId) -> Unit,
    onDecrement: (FieldId, PlayerId) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            SectionHeader(section.title)
            section.rows.forEach { row: RuleRow ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.label, style = MaterialTheme.typography.bodyLarge)
                    }
                    RuleTypeTag(row.tag)
                    Spacer(Modifier.size(8.dp))
                    Stepper(
                        value = row.value,
                        onDecrement = {
                            val f = row.field; val p = activeId
                            if (f != null && p != null) onDecrement(f, p)
                        },
                        onIncrement = {
                            val f = row.field; val p = activeId
                            if (f != null && p != null) onIncrement(f, p)
                        },
                    )
                    Spacer(Modifier.size(8.dp))
                    PointsLabel(row.points)
                }
            }
        }
    }
}

@Composable
private fun RankedSection(
    section: Section.Ranked,
    onIncrement: (FieldId, PlayerId) -> Unit,
    onDecrement: (FieldId, PlayerId) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(section.title, Modifier.weight(1f))
                section.columns.forEach { col: RankedColumn -> RuleTypeTag(col.tag) }
            }
            Text(
                "Table-scoped: rank resolves across all players (${section.fieldLabel}).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.size(4.dp))
            section.rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        row.player.name + if (row.isActive) "  (you)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (row.isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Stepper(
                        value = row.value,
                        onDecrement = { section.field?.let { onDecrement(it, row.player.id) } },
                        onIncrement = { section.field?.let { onIncrement(it, row.player.id) } },
                    )
                    Spacer(Modifier.size(8.dp))
                    // Points from each rule in this group (e.g. rank award + per-disc bonus).
                    section.columns.forEach { col ->
                        PointsLabel(row.points[col.ruleId] ?: 0, Modifier.widthIn(min = 44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreFooter(state: ScoreEntryUiState) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                state.total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(6.dp))
        state.categoryBreakdown.forEach { (title, subtotal) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    title ?: "Other",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                Text(subtotal.toString(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlayerSelector(
    players: List<PlayerChip>,
    onSelect: (PlayerId) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEach { chip ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (chip.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSelect(chip.id) },
            ) {
                Text(
                    chip.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String?, modifier: Modifier = Modifier) {
    Text(
        title ?: "Other",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun RuleTypeTag(tag: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text(
            tag,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PointsLabel(points: Int, modifier: Modifier = Modifier) {
    Text(
        "$points",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        modifier = modifier.widthIn(min = 36.dp),
    )
}

@Composable
private fun Stepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−", enabled = value > 0, onClick = onDecrement)
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 32.dp).padding(horizontal = 6.dp),
        )
        StepperButton("+", enabled = true, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        modifier = Modifier.size(36.dp),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
