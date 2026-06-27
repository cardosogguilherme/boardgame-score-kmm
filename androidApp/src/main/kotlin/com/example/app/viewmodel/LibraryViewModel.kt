package com.example.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.ui.model.LibraryUiState
import com.example.ui.model.libraryUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    observeTemplates: ObserveTemplates,
    observeGames: ObserveGames,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        combine(observeTemplates(), observeGames()) { templates, games ->
            libraryUiState(templates, games)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
