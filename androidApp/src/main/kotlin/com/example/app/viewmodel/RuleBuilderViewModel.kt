package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoring.interactors.GetTemplate
import com.example.scoring.interactors.SaveTemplate
import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.ui.model.RuleBuilderUiState
import com.example.ui.model.RuleParams
import com.example.ui.model.RuleTypeOption
import com.example.ui.model.emptyTemplateDraft
import com.example.ui.model.renamed
import com.example.ui.model.ruleBuilderUiState
import com.example.ui.model.withFieldAdded
import com.example.ui.model.withFieldRemoved
import com.example.ui.model.withRuleAdded
import com.example.ui.model.withRuleRemoved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Holds and exposes the mutable template draft for the rule-builder screen.
 *
 * - When [templateId] is null a fresh draft is initialised with a random id; when non-null the
 *   existing template is loaded from [getTemplate] in an init coroutine.
 * - Edit callbacks apply the pure helpers from `:ui` to the draft and re-emit via [uiState].
 * - [save] validates + persists via [saveTemplate]; invokes [onSaved] only when there are no
 *   errors. Validation errors are surfaced through [saveErrors] (the mapper already reflects them
 *   in [uiState].errors and [uiState].canSave, so the screen can gate the Save button without
 *   checking [saveErrors] directly).
 */
class RuleBuilderViewModel(
    private val getTemplate: GetTemplate,
    private val saveTemplate: SaveTemplate,
    templateId: String?,
) : ViewModel() {

    private val _draft = MutableStateFlow(
        emptyTemplateDraft("tpl-${Random.nextInt(Int.MAX_VALUE)}"),
    )

    private val _saveErrors = MutableStateFlow<List<String>>(emptyList())
    val saveErrors: StateFlow<List<String>> = _saveErrors.asStateFlow()

    val uiState: StateFlow<RuleBuilderUiState> = _draft
        .map { ruleBuilderUiState(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ruleBuilderUiState(_draft.value),
        )

    init {
        if (templateId != null) {
            viewModelScope.launch {
                getTemplate(templateId)?.let { _draft.value = it }
            }
        }
    }

    // ── Edit callbacks ────────────────────────────────────────────────────────────────────────

    fun onNameChange(name: String) {
        _draft.update { it.renamed(name) }
    }

    fun onAddField(label: String, kind: FieldKind, max: Int?) {
        _draft.update { it.withFieldAdded(label, kind, max) }
    }

    fun onRemoveField(id: String) {
        _draft.update { it.withFieldRemoved(id) }
    }

    fun onAddRule(type: RuleTypeOption, fieldId: String, params: RuleParams) {
        _draft.update { it.withRuleAdded(type, FieldId(fieldId), params) }
    }

    fun onRemoveRule(id: String) {
        _draft.update { it.withRuleRemoved(id) }
    }

    // ── Persistence ───────────────────────────────────────────────────────────────────────────

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val errors = saveTemplate(_draft.value)
            if (errors.isEmpty()) onSaved() else _saveErrors.value = errors
        }
    }
}
