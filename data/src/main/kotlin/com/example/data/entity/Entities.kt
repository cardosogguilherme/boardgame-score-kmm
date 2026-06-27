package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val scenariosJson: String = "[]",
)

@Entity(tableName = "field")
data class FieldEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val label: String,
    val kind: String,
    val max: Int?,
    val position: Int,
)

@Entity(tableName = "rule")
data class RuleEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val type: String,
    val payloadJson: String,
    val position: Int,
)

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val scenarioId: String?,
    val name: String,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val name: String,
    val position: Int,
)

@Entity(tableName = "player_value", primaryKeys = ["gameId", "playerId", "fieldId"])
data class PlayerValueEntity(
    val gameId: String,
    val playerId: String,
    val fieldId: String,
    val value: Int,
)
