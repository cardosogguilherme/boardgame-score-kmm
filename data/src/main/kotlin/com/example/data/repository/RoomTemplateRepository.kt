package com.example.data.repository

import com.example.data.dao.TemplateDao
import com.example.data.mapper.templateFrom
import com.example.data.mapper.toEntities
import com.example.scoring.model.Template
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTemplateRepository(private val dao: TemplateDao) : TemplateRepository {
    override fun observeTemplates(): Flow<List<Template>> =
        dao.observeTemplates().map { rows ->
            rows.map { templateFrom(it, dao.fields(it.id), dao.rules(it.id)) }
        }

    override suspend fun getTemplate(id: String): Template? {
        val t = dao.getTemplate(id) ?: return null
        return templateFrom(t, dao.fields(id), dao.rules(id))
    }

    override suspend fun saveTemplate(template: Template) {
        val (t, fields, rules) = template.toEntities()
        dao.saveTemplate(t, fields, rules)
    }

    override suspend fun deleteTemplate(id: String) {
        dao.clearFields(id); dao.clearRules(id); dao.deleteTemplate(id)
    }
}
