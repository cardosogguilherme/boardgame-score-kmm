package com.example.data.mapper

import com.example.scoring.model.Scenario
import com.example.scoring.model.ScoringRule
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** ScoringRule is a @Serializable sealed interface; persist the whole rule as JSON. */
internal val ruleJson = Json { ignoreUnknownKeys = true }

internal fun ScoringRule.toJson(): String = ruleJson.encodeToString(ScoringRule.serializer(), this)
internal fun ruleFromJson(json: String): ScoringRule = ruleJson.decodeFromString(ScoringRule.serializer(), json)

internal fun List<Scenario>.toJson(): String =
    ruleJson.encodeToString(ListSerializer(Scenario.serializer()), this)

internal fun scenariosFromJson(json: String): List<Scenario> =
    ruleJson.decodeFromString(ListSerializer(Scenario.serializer()), json)
