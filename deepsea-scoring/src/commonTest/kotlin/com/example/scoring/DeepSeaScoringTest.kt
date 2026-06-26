package com.example.scoring

import com.example.scoring.engine.Scorer
import com.example.scoring.engine.ScoringContext
import com.example.scoring.engine.scope
import com.example.scoring.engine.validate
import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.RuleId
import com.example.scoring.model.RuleScope
import com.example.scoring.model.ScoringRule
import com.example.scoring.model.Template
import com.example.scoring.model.resolve
import com.example.scoring.sample.DeepSea
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepSeaScoringTest {

    private val resolved = DeepSea.template.resolve(DeepSea.SCENARIO_1)

    // §5 fixture: You & Mira tie at the top of the ranked goal, Kane trails.
    private val you = PlayerInput(
        id = PlayerId("you"),
        name = "You",
        values = mapOf(
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
    private val mira = PlayerInput(PlayerId("mira"), "Mira", mapOf(DeepSea.discsPlaced to 12))
    private val kane = PlayerInput(PlayerId("kane"), "Kane", mapOf(DeepSea.discsPlaced to 8))

    private val ctx = ScoringContext(resolved, listOf(you, mira, kane))

    @Test
    fun you_total_is_83() {
        assertEquals(83, Scorer.total(you, ctx))
    }

    @Test
    fun per_rule_breakdown_matches_brief() {
        val b = Scorer.breakdown(you, ctx).mapKeys { it.key.raw }
        assertEquals(10, b["rep"])
        assertEquals(6, b["ins"])
        assertEquals(14, b["coo"])
        assertEquals(3, b["ing"])
        assertEquals(6, b["imp1"])
        assertEquals(8, b["imp2"])
        assertEquals(9, b["jrn"])
        assertEquals(5, b["spc"])
        assertEquals(2, b["res"])
        assertEquals(8, b["goal"])
        assertEquals(12, b["goalBonus"])
        assertEquals(83, b.values.sum())
    }

    @Test
    fun ranked_goal_resolves_8_8_0_across_players() {
        fun goal(p: PlayerInput) = Scorer.breakdown(p, ctx).getValue(RuleId("goal"))
        assertEquals(8, goal(you))
        assertEquals(8, goal(mira))
        assertEquals(0, goal(kane))
    }

    @Test
    fun ranking_rule_is_table_scoped() {
        val goalRule = resolved.rules.first { it.id == RuleId("goal") }
        assertTrue(goalRule is ScoringRule.Ranking)
        assertEquals(RuleScope.TABLE, goalRule.scope())
    }

    @Test
    fun resolved_template_is_valid() {
        assertEquals(emptyList(), resolved.validate())
    }

    @Test
    fun template_survives_json_round_trip() {
        val json = Json { prettyPrint = false }
        val encoded = json.encodeToString(Template.serializer(), DeepSea.template)
        val decoded = json.decodeFromString(Template.serializer(), encoded)
        assertEquals(DeepSea.template, decoded)
    }

    @Test
    fun missing_inputs_default_to_zero() {
        val empty = PlayerInput(PlayerId("empty"), "Empty", emptyMap<FieldId, Int>())
        val emptyCtx = ScoringContext(resolved, listOf(empty, mira, kane))
        // No discs placed -> bottom of the ranked goal -> 0 for both goal and bonus.
        assertEquals(0, Scorer.total(empty, emptyCtx))
    }
}
