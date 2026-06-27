package com.example.scoring.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class GameId(val raw: String)

@Serializable
enum class GameStatus { IN_PROGRESS, FINISHED }

/**
 * A scoring session: a chosen template/scenario plus the players and their entered values.
 * `templateId`/`scenarioId` reference a persisted Template; `players` carries the live inputs.
 */
@Serializable
data class Game(
    val id: GameId,
    val templateId: String,
    val scenarioId: String? = null,
    val name: String,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val players: List<PlayerInput> = emptyList(),
)
