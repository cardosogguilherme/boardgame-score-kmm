package com.example.ui

import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.RuleId
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * UI-layer acceptance test: drives the §5 fixture through the Screen A state holder (not the
 * domain test) and proves the running total reads 83 and the ranked goal resolves 8/8/0.
 */
class ScoreEntryStateTest {

    private fun fixtureState(): ScoreEntryState {
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

    @Test
    fun running_total_reads_83() {
        assertEquals(83, fixtureState().total)
    }

    @Test
    fun ranked_goal_section_resolves_8_8_0() {
        val ranked = fixtureState().sections().filterIsInstance<Section.Ranked>().single()
        val byName = ranked.rows.associateBy { it.player.name }
        assertEquals(8, byName.getValue("You").points.getValue(RuleId("goal")))
        assertEquals(8, byName.getValue("Mira").points.getValue(RuleId("goal")))
        assertEquals(0, byName.getValue("Kane").points.getValue(RuleId("goal")))
    }

    @Test
    fun stepping_a_field_updates_the_live_total() {
        val state = fixtureState()
        val before = state.total
        state.increment(DeepSea.journal) // journal is a FLAT rule: +1 point
        assertEquals(before + 1, state.total)
        state.decrement(DeepSea.journal)
        assertEquals(before, state.total)
    }

    @Test
    fun steppers_clamp_at_zero() {
        val state = fixtureState()
        repeat(50) { state.decrement(DeepSea.ingenuity) }
        assertEquals(0, state.valueOf(PlayerId("you"), DeepSea.ingenuity))
    }

    @Test
    fun sections_follow_group_metadata_in_order() {
        val titles = fixtureState().sections().map { it.title }
        assertEquals(
            listOf("Attributes", "Impact Board", "Journal & Specialist", "Leftover Research", "Ranked Goal"),
            titles,
        )
        // The ranked-goal group is the cross-player section.
        assertTrue(fixtureState().sections().last() is Section.Ranked)
    }
}
