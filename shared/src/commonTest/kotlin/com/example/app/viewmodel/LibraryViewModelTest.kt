package com.example.app.viewmodel

import com.example.app.testing.FakeGameRepository
import com.example.app.testing.FakeTemplateRepository
import com.example.scoring.interactors.ObserveGames
import com.example.scoring.interactors.ObserveTemplates
import com.example.scoring.sample.DeepSea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposes_templates_from_interactor() = runTest {
        val templates = FakeTemplateRepository(listOf(DeepSea.template))
        val vm = LibraryViewModel(ObserveTemplates(templates), ObserveGames(FakeGameRepository()))
        val state = vm.uiState.first { it.templates.isNotEmpty() }
        assertEquals(DeepSea.template.name, state.templates.single().name)
    }
}
