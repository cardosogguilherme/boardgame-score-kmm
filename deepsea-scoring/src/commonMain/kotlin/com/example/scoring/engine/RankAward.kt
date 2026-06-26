package com.example.scoring.engine

/**
 * Deep Sea ranked-goal tie rule. A player's award is decided by competition rank — the count
 * of strictly-higher values in the column. Tied top players each take the top award, and the
 * award slot(s) consumed by the tie are skipped, so 12/12/8 with awards [8,4] resolves to
 * 8/8/0 (second place is not scored). Brief §4: `rankAward(value, column, awards)`.
 */
fun rankAward(value: Int, column: List<Int>, awards: List<Int>): Int {
    val rank = column.count { it > value }
    return awards.getOrElse(rank) { 0 }
}
