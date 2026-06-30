package com.example.data.mapper

import com.example.data.entity.GameEntity
import com.example.data.entity.PlayerEntity
import com.example.data.entity.PlayerValueEntity
import com.example.scoring.model.FieldId
import com.example.scoring.model.Game
import com.example.scoring.model.GameId
import com.example.scoring.model.GameStatus
import com.example.scoring.model.PlayerId
import com.example.scoring.model.PlayerInput

fun Game.toEntities(createdAt: Long): Triple<GameEntity, List<PlayerEntity>, List<PlayerValueEntity>> {
    val g = GameEntity(id.raw, templateId, scenarioId, name, status.name, createdAt)
    val playerEntities = players.mapIndexed { i, p -> PlayerEntity(p.id.raw, id.raw, p.name, i) }
    val valueEntities = players.flatMap { p ->
        p.values.map { (field, v) -> PlayerValueEntity(id.raw, p.id.raw, field.raw, v) }
    }
    return Triple(g, playerEntities, valueEntities)
}

fun gameFrom(g: GameEntity, players: List<PlayerEntity>, values: List<PlayerValueEntity>): Game =
    Game(
        id = GameId(g.id),
        templateId = g.templateId,
        scenarioId = g.scenarioId,
        name = g.name,
        status = GameStatus.valueOf(g.status),
        players = players.sortedBy { it.position }.map { p ->
            PlayerInput(
                id = PlayerId(p.id),
                name = p.name,
                values = values.filter { it.playerId == p.id }
                    .associate { FieldId(it.fieldId) to it.value },
            )
        },
    )
