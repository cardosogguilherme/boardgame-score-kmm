package com.example.ui.model

import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.RuleId
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression anchor for the PURE reducer: the Deep Sea §5 fixture must total 83 with the ranked
 * goal awarding 8/8/0, and the sections must follow the group metadata order. Mirrors the fixture
 * used by [com.example.ui.ScoreEntryStateTest] so both the holder and the reducer prove the anchor.
 */
class ScoreEntryUiStateTest {

    private fun fixture(): ScoreEntryUiState {
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
        return scoreEntryUiState(resolved, listOf(you, mira, kane), PlayerId("you"))
    }

    @Test
    fun reducer_total_reads_83() {
        assertEquals(83, fixture().total)
    }

    @Test
    fun ranked_goal_awards_8_8_0() {
        val ranked = fixture().sections.filterIsInstance<Section.Ranked>().single()
        val byName = ranked.rows.associateBy { it.player.name }
        assertEquals(8, byName.getValue("You").points.getValue(RuleId("goal")))
        assertEquals(8, byName.getValue("Mira").points.getValue(RuleId("goal")))
        assertEquals(0, byName.getValue("Kane").points.getValue(RuleId("goal")))
    }

    @Test
    fun sections_follow_group_metadata_in_order() {
        assertEquals(
            listOf("Attributes", "Impact Board", "Journal & Specialist", "Leftover Research", "Ranked Goal"),
            fixture().sections.map { it.title },
        )
    }
}
