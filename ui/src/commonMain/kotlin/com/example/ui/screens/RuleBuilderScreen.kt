package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scoring.model.FieldKind
import com.example.ui.model.FieldRowUi
import com.example.ui.model.RuleBuilderUiState
import com.example.ui.model.RuleParams
import com.example.ui.model.RuleRowUi
import com.example.ui.model.RuleTypeOption

/**
 * Stateless Rule Builder screen. All business state lives upstream (ViewModel / parent);
 * this composable only holds transient input buffers for the in-progress add-field /
 * add-rule forms (cleared on emit).
 */
@Composable
fun RuleBuilderScreen(
    state: RuleBuilderUiState,
    onNameChange: (String) -> Unit,
    onAddField: (label: String, kind: FieldKind, max: Int?) -> Unit,
    onRemoveField: (String) -> Unit,
    onAddRule: (RuleTypeOption, fieldId: String, RuleParams) -> Unit,
    onRemoveRule: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ── Transient input buffers — in-progress "add field" form ────────────────
    var fieldLabel by remember { mutableStateOf("") }
    var fieldKind by remember { mutableStateOf(FieldKind.COUNT) }
    var fieldMaxText by remember { mutableStateOf("") }

    // ── Transient input buffers — in-progress "add rule" form ─────────────────
    var selectedPalette by remember { mutableStateOf<RuleTypeOption?>(null) }
    var selectedFieldId by remember { mutableStateOf<String?>(null) }
    var rulePoints by remember { mutableStateOf("1") }
    var ruleDivisor by remember { mutableStateOf("1") }
    var ruleTable by remember { mutableStateOf("") }
    var ruleAwards by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Template name ──────────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text("Template name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        // ── Fields section ─────────────────────────────────────────────────────
        item { SectionHeader("Fields") }

        items(state.fields, key = { it.id }) { row ->
            FieldRowItem(row = row, onRemove = { onRemoveField(row.id) })
        }

        item {
            AddFieldRow(
                label = fieldLabel,
                kind = fieldKind,
                maxText = fieldMaxText,
                onLabelChange = { fieldLabel = it },
                onKindChange = { fieldKind = it },
                onMaxChange = { fieldMaxText = it },
                onAdd = {
                    if (fieldLabel.isNotBlank()) {
                        onAddField(fieldLabel, fieldKind, fieldMaxText.toIntOrNull())
                        fieldLabel = ""
                        fieldKind = FieldKind.COUNT
                        fieldMaxText = ""
                    }
                },
            )
        }

        // ── Rules section ──────────────────────────────────────────────────────
        item { SectionHeader("Rules") }

        item {
            AddRulePanel(
                palette = state.palette,
                fields = state.fields,
                selectedPalette = selectedPalette,
                selectedFieldId = selectedFieldId,
                rulePoints = rulePoints,
                ruleDivisor = ruleDivisor,
                ruleTable = ruleTable,
                ruleAwards = ruleAwards,
                onSelectPalette = { opt ->
                    selectedPalette = if (selectedPalette == opt) null else opt
                    if (selectedPalette != null) {
                        selectedFieldId = state.fields.firstOrNull()?.id
                    }
                },
                onSelectField = { selectedFieldId = it },
                onPointsChange = { rulePoints = it },
                onDivisorChange = { ruleDivisor = it },
                onTableChange = { ruleTable = it },
                onAwardsChange = { ruleAwards = it },
                onAdd = {
                    val opt = selectedPalette
                    val fieldId = selectedFieldId
                    if (opt != null && fieldId != null) {
                        val params = buildRuleParams(opt, rulePoints, ruleDivisor, ruleTable, ruleAwards)
                        if (params != null) {
                            onAddRule(opt, fieldId, params)
                            selectedPalette = null
                            selectedFieldId = null
                            rulePoints = "1"
                            ruleDivisor = "1"
                            ruleTable = ""
                            ruleAwards = ""
                        }
                    }
                },
            )
        }

        items(state.rules, key = { it.id }) { row ->
            RuleRowItem(row = row, onRemove = { onRemoveRule(row.id) })
        }

        // ── Errors panel ───────────────────────────────────────────────────────
        if (state.errors.isNotEmpty()) {
            item { ErrorsPanel(errors = state.errors) }
        }

        // ── Save ───────────────────────────────────────────────────────────────
        item {
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Save")
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

// ── Field row ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun FieldRowItem(row: FieldRowUi, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(row.kind.name)
                        if (row.max != null) append(" · max ${row.max}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            TextButton(onClick = onRemove) { Text("✕") }
        }
    }
}

// ── Add field row ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddFieldRow(
    label: String,
    kind: FieldKind,
    maxText: String,
    onLabelChange: (String) -> Unit,
    onKindChange: (FieldKind) -> Unit,
    onMaxChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Add field", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                label = { Text("Field label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FieldKindSelector(selected = kind, onSelect = onKindChange)
            OutlinedTextField(
                value = maxText,
                onValueChange = onMaxChange,
                label = { Text("Max value (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = onAdd,
                enabled = label.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add field")
            }
        }
    }
}

// ── FieldKind selector ─────────────────────────────────────────────────────────────────────────

@Composable
private fun FieldKindSelector(selected: FieldKind, onSelect: (FieldKind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldKind.entries.forEach { kind ->
            SelectableChip(
                label = kind.name,
                selected = kind == selected,
                onClick = { onSelect(kind) },
            )
        }
    }
}

// ── Add rule panel ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddRulePanel(
    palette: List<RuleTypeOption>,
    fields: List<FieldRowUi>,
    selectedPalette: RuleTypeOption?,
    selectedFieldId: String?,
    rulePoints: String,
    ruleDivisor: String,
    ruleTable: String,
    ruleAwards: String,
    onSelectPalette: (RuleTypeOption) -> Unit,
    onSelectField: (String) -> Unit,
    onPointsChange: (String) -> Unit,
    onDivisorChange: (String) -> Unit,
    onTableChange: (String) -> Unit,
    onAwardsChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Add rule", style = MaterialTheme.typography.labelLarge)
            palette.forEach { opt ->
                PaletteOptionRow(
                    option = opt,
                    selected = opt == selectedPalette,
                    onClick = { onSelectPalette(opt) },
                )
            }
            if (selectedPalette != null) {
                HorizontalDivider()
                if (fields.isNotEmpty()) {
                    Text("Target field", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fields.forEach { field ->
                            SelectableChip(
                                label = field.label,
                                selected = field.id == selectedFieldId,
                                onClick = { onSelectField(field.id) },
                            )
                        }
                    }
                }
                RuleParamsForm(
                    type = selectedPalette,
                    points = rulePoints,
                    divisor = ruleDivisor,
                    table = ruleTable,
                    awards = ruleAwards,
                    onPointsChange = onPointsChange,
                    onDivisorChange = onDivisorChange,
                    onTableChange = onTableChange,
                    onAwardsChange = onAwardsChange,
                )
                val canAdd = selectedFieldId != null
                    && canAddRule(selectedPalette, rulePoints, ruleDivisor, ruleTable, ruleAwards)
                Button(
                    onClick = onAdd,
                    enabled = canAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add rule")
                }
            }
        }
    }
}

@Composable
private fun PaletteOptionRow(option: RuleTypeOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                option.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(72.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                option.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Rule params form ───────────────────────────────────────────────────────────────────────────

@Composable
private fun RuleParamsForm(
    type: RuleTypeOption,
    points: String,
    divisor: String,
    table: String,
    awards: String,
    onPointsChange: (String) -> Unit,
    onDivisorChange: (String) -> Unit,
    onTableChange: (String) -> Unit,
    onAwardsChange: (String) -> Unit,
) {
    when (type) {
        RuleTypeOption.PER_UNIT -> OutlinedTextField(
            value = points,
            onValueChange = onPointsChange,
            label = { Text("Points per unit") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        RuleTypeOption.PER_N -> OutlinedTextField(
            value = divisor,
            onValueChange = onDivisorChange,
            label = { Text("Divisor N (non-zero)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        RuleTypeOption.LOOKUP -> OutlinedTextField(
            value = table,
            onValueChange = onTableChange,
            label = { Text("Point table (comma-separated ints)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        RuleTypeOption.RANKING -> OutlinedTextField(
            value = awards,
            onValueChange = onAwardsChange,
            label = { Text("Awards by rank (comma-separated ints)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        RuleTypeOption.FLAT -> Text(
            "No additional parameters — field value scores directly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

// ── Rule row ───────────────────────────────────────────────────────────────────────────────────

@Composable
private fun RuleRowItem(row: RuleRowUi, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.describe,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(8.dp))
            RuleTypeTag(tag = row.tag)
            TextButton(onClick = onRemove) { Text("✕") }
        }
    }
}

// ── Errors panel ───────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorsPanel(errors: List<String>) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Validation errors",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
            errors.forEach { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

// ── Selectable chip ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

// ── Rule type tag (visual style matches ScreenA.kt's RuleTypeTag) ──────────────────────────────

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

// ── Local validation & param construction ──────────────────────────────────────────────────────

private fun canAddRule(
    type: RuleTypeOption,
    points: String,
    divisor: String,
    table: String,
    awards: String,
): Boolean = when (type) {
    RuleTypeOption.PER_UNIT -> points.toIntOrNull() != null
    RuleTypeOption.PER_N -> (divisor.toIntOrNull() ?: 0) != 0
    RuleTypeOption.LOOKUP -> parseIntList(table)?.isNotEmpty() == true
    RuleTypeOption.RANKING -> parseIntList(awards)?.isNotEmpty() == true
    RuleTypeOption.FLAT -> true
}

private fun buildRuleParams(
    type: RuleTypeOption,
    points: String,
    divisor: String,
    table: String,
    awards: String,
): RuleParams? = when (type) {
    RuleTypeOption.PER_UNIT -> points.toIntOrNull()?.let { RuleParams(points = it) }
    RuleTypeOption.PER_N -> divisor.toIntOrNull()?.takeIf { it != 0 }?.let { RuleParams(divisor = it) }
    RuleTypeOption.LOOKUP -> parseIntList(table)?.takeIf { it.isNotEmpty() }?.let { RuleParams(table = it) }
    RuleTypeOption.RANKING -> parseIntList(awards)?.takeIf { it.isNotEmpty() }?.let { RuleParams(awards = it) }
    RuleTypeOption.FLAT -> RuleParams()
}

private fun parseIntList(text: String): List<Int>? {
    if (text.isBlank()) return null
    val parts = text.split(",").map { it.trim() }
    return if (parts.all { it.toIntOrNull() != null }) parts.map { it.toInt() } else null
}
