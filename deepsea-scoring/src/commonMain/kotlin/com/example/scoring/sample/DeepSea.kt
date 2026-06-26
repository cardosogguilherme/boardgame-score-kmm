package com.example.scoring.sample

import com.example.scoring.model.Field
import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.scoring.model.RuleId
import com.example.scoring.model.Scenario
import com.example.scoring.model.ScoringRule
import com.example.scoring.model.Template

/**
 * Endeavor: Deep Sea as template data — the engine's first game and end-to-end fixture (brief §5).
 *
 * NOTE: [trackTable] is a **placeholder** ([0,1,3,6,10,14,19,25]); confirm the real thresholds
 * from the physical board / rulebook before treating scores as authoritative (brief §5 / §9).
 */
object DeepSea {

    // Field ids (referenced by id everywhere — never by label; Design law §4).
    val reputation = FieldId("reputation")
    val inspiration = FieldId("inspiration")
    val coordination = FieldId("coordination")
    val ingenuity = FieldId("ingenuity")
    val impact1 = FieldId("impact1")
    val impact2 = FieldId("impact2")
    val journal = FieldId("journal")
    val specialist = FieldId("specialist")
    val leftover = FieldId("leftover")
    val discsPlaced = FieldId("discsPlaced")

    /** Placeholder attribute-track point table — see the class note. */
    val trackTable = listOf(0, 1, 3, 6, 10, 14, 19, 25)

    const val SCENARIO_1 = "scenario-1"

    // Section group labels (UI grouping, §10).
    private const val ATTRIBUTES = "Attributes"
    private const val IMPACT = "Impact Board"
    private const val JOURNAL = "Journal & Specialist"
    private const val LEFTOVER = "Leftover Research"
    private const val GOAL = "Ranked Goal"

    val template = Template(
        id = "deep-sea",
        name = "Endeavor: Deep Sea",
        fields = listOf(
            Field(reputation, "Reputation", FieldKind.TRACK, max = trackTable.size - 1),
            Field(inspiration, "Inspiration", FieldKind.TRACK, max = trackTable.size - 1),
            Field(coordination, "Coordination", FieldKind.TRACK, max = trackTable.size - 1),
            Field(ingenuity, "Ingenuity", FieldKind.TRACK, max = trackTable.size - 1),
            Field(impact1, "Impact discs · 1-pt spaces", FieldKind.COUNT),
            Field(impact2, "Impact discs · 2-pt spaces", FieldKind.COUNT),
            Field(journal, "Journal points", FieldKind.COUNT),
            Field(specialist, "Specialist points", FieldKind.COUNT),
            Field(leftover, "Leftover research + staging discs", FieldKind.COUNT),
        ),
        rules = listOf(
            ScoringRule.Lookup(RuleId("rep"), reputation, trackTable, group = ATTRIBUTES),
            ScoringRule.Lookup(RuleId("ins"), inspiration, trackTable, group = ATTRIBUTES),
            ScoringRule.Lookup(RuleId("coo"), coordination, trackTable, group = ATTRIBUTES),
            ScoringRule.Lookup(RuleId("ing"), ingenuity, trackTable, group = ATTRIBUTES),
            ScoringRule.PerUnit(RuleId("imp1"), impact1, points = 1, group = IMPACT),
            ScoringRule.PerUnit(RuleId("imp2"), impact2, points = 2, group = IMPACT),
            ScoringRule.Flat(RuleId("jrn"), journal, group = JOURNAL),
            ScoringRule.Flat(RuleId("spc"), specialist, group = JOURNAL),
            ScoringRule.PerN(RuleId("res"), leftover, divisor = 3, group = LEFTOVER),
        ),
        scenarios = listOf(
            Scenario(
                id = SCENARIO_1,
                name = "Scenario 1",
                fields = listOf(
                    Field(discsPlaced, "Discs placed", FieldKind.RANKING),
                ),
                rules = listOf(
                    // TABLE-scoped ranked goal across all players...
                    ScoringRule.Ranking(RuleId("goal"), discsPlaced, awards = listOf(8, 4), group = GOAL),
                    // ...plus a per-player 1-pt-per-disc bonus.
                    ScoringRule.PerUnit(RuleId("goalBonus"), discsPlaced, points = 1, group = GOAL),
                ),
            ),
        ),
    )
}
