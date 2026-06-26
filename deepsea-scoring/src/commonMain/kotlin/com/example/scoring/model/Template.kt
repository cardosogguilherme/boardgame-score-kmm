package com.example.scoring.model

import kotlinx.serialization.Serializable

@Serializable
data class Template(
    val id: String,
    val name: String,
    val fields: List<Field>,
    val rules: List<ScoringRule>,
    val scenarios: List<Scenario> = emptyList(),
)

@Serializable
data class Scenario(
    val id: String,
    val name: String,
    val fields: List<Field> = emptyList(),
    val rules: List<ScoringRule> = emptyList(),
)
