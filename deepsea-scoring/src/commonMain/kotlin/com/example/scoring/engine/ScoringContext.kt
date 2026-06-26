package com.example.scoring.engine

import com.example.scoring.model.FieldId
import com.example.scoring.model.PlayerInput
import com.example.scoring.model.ResolvedTemplate

/**
 * Holds the whole table — every player's column — because scoring forks on scope: PER_PLAYER
 * rules read one sheet, TABLE rules ([com.example.scoring.model.ScoringRule.Ranking]) need
 * everyone (Design law §3.6). Per-player rules simply ignore the rest of the table.
 */
data class ScoringContext(
    val template: ResolvedTemplate,
    val players: List<PlayerInput>,
) {
    /** The values every player entered for [field], in player order (missing == 0). */
    fun column(field: FieldId): List<Int> = players.map { it.values[field] ?: 0 }
}
