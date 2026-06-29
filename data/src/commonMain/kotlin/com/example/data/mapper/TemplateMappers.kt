package com.example.data.mapper

import com.example.data.entity.FieldEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity
import com.example.scoring.model.Field
import com.example.scoring.model.FieldId
import com.example.scoring.model.FieldKind
import com.example.scoring.model.Template

fun Template.toEntities(): Triple<TemplateEntity, List<FieldEntity>, List<RuleEntity>> {
    val t = TemplateEntity(id = id, name = name, scenariosJson = scenarios.toJson())
    val fieldEntities = fields.mapIndexed { i, f ->
        FieldEntity(f.id.raw, id, f.label, f.kind.name, f.max, i)
    }
    val ruleEntities = rules.mapIndexed { i, r ->
        RuleEntity(r.id.raw, id, r::class.simpleName ?: "rule", r.toJson(), i)
    }
    return Triple(t, fieldEntities, ruleEntities)
}

fun templateFrom(t: TemplateEntity, fields: List<FieldEntity>, rules: List<RuleEntity>): Template =
    Template(
        id = t.id,
        name = t.name,
        fields = fields.sortedBy { it.position }.map {
            Field(FieldId(it.id), it.label, FieldKind.valueOf(it.kind), it.max)
        },
        rules = rules.sortedBy { it.position }.map { ruleFromJson(it.payloadJson) },
        scenarios = scenariosFromJson(t.scenariosJson),
    )
