package com.example.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea

/**
 * Builds a Screen A state holder for Endeavor: Deep Sea (scenario 1), pre-loaded with the §5
 * fixture so the running total opens at 83 and the ranked goal at 8/8/0. Steppers edit from there.
 */
fun deepSeaSampleState(): ScoreEntryState {
    val resolved = DeepSea.template.resolve(DeepSea.SCENARIO_1)
    val you = PlayerInput(
        PlayerId("you"), "You",
        mapOf(
            DeepSea.reputation to 4,
            DeepSea.inspiration to 3,
            DeepSea.coordination to 5,
            DeepSea.ingenuity to 2,
            DeepSea.impact1 to 6,
            DeepSea.impact2 to 4,
            DeepSea.journal to 9,
            DeepSea.specialist to 5,
            DeepSea.leftover to 7,
            DeepSea.discsPlaced to 12,
        ),
    )
    val mira = PlayerInput(PlayerId("mira"), "Mira", mapOf(DeepSea.discsPlaced to 12))
    val kane = PlayerInput(PlayerId("kane"), "Kane", mapOf(DeepSea.discsPlaced to 8))
    return ScoreEntryState(resolved, listOf(you, mira, kane), activePlayerId = PlayerId("you"))
}

/** Themed, self-contained Screen A for Deep Sea — the single entry point platform apps call. */
@Composable
fun DeepSeaScreenA(modifier: Modifier = Modifier) {
    val state = remember { deepSeaSampleState() }
    MaterialTheme {
        Surface {
            ScoreEntryScreen(
                state = state.uiStateNow(),
                onSelectPlayer = { state.activePlayerId = it },
                onIncrement = { f, p -> state.increment(f, playerId = p) },
                onDecrement = { f, p -> state.decrement(f, playerId = p) },
                title = DeepSea.template.name,
                modifier = modifier,
            )
        }
    }
}
