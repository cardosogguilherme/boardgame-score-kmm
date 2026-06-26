package com.example.scoring.model

import kotlinx.serialization.Serializable

@Serializable
enum class FieldKind { COUNT, TRACK, RANKING }

@Serializable
data class Field(
    val id: FieldId,
    val label: String,
    val kind: FieldKind,
    val max: Int? = null,
)
