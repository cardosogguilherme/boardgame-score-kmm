package com.example.scoring.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RuleScope { PER_PLAYER, TABLE }

/**
 * A scoring rule is **dumb data** — an AST node. It carries no behaviour: every operation
 * (score, scope, validate, describe, group) is a top-level `when` in the `engine` package.
 * Only [id] is hoisted to the interface; `field` stays per-leaf so future combinator rules
 * (which contain other rules and have no single field) remain expressible. See Design law §3.
 *
 * [group] is optional section metadata used only for UI grouping (Deferred decision §10,
 * resolved: groups live on the model). It is data, not behaviour, and is also per-leaf.
 */
@Serializable
sealed interface ScoringRule {
    val id: RuleId

    @Serializable
    @SerialName("lookup")
    data class Lookup(
        override val id: RuleId,
        val field: FieldId,
        val table: List<Int>,
        val group: String? = null,
    ) : ScoringRule

    @Serializable
    @SerialName("perUnit")
    data class PerUnit(
        override val id: RuleId,
        val field: FieldId,
        val points: Int,
        val group: String? = null,
    ) : ScoringRule

    @Serializable
    @SerialName("flat")
    data class Flat(
        override val id: RuleId,
        val field: FieldId,
        val group: String? = null,
    ) : ScoringRule

    @Serializable
    @SerialName("perN")
    data class PerN(
        override val id: RuleId,
        val field: FieldId,
        val divisor: Int,
        val group: String? = null,
    ) : ScoringRule

    @Serializable
    @SerialName("ranking")
    data class Ranking(
        override val id: RuleId,
        val field: FieldId,
        val awards: List<Int>,
        val group: String? = null,
    ) : ScoringRule
}
