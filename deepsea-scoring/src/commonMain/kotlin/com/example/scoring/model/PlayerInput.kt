package com.example.scoring.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerInput(
    val id: PlayerId,
    val name: String,
    val values: Map<FieldId, Int> = emptyMap(),
)
