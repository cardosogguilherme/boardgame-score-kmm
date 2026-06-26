package com.example.scoring.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class FieldId(val raw: String)

@Serializable
@JvmInline
value class PlayerId(val raw: String)

@Serializable
@JvmInline
value class RuleId(val raw: String)
