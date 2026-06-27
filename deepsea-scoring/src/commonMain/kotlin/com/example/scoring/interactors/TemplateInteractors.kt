package com.example.scoring.interactors

import com.example.scoring.engine.validate
import com.example.scoring.model.Template
import com.example.scoring.model.resolve
import com.example.scoring.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplates(private val repo: TemplateRepository) {
    operator fun invoke(): Flow<List<Template>> = repo.observeTemplates()
}

class GetTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(id: String): Template? = repo.getTemplate(id)
}

/** Validates the template; persists only when there are no errors. Returns the error list. */
class SaveTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(template: Template): List<String> {
        val errors = template.resolve().validate()
        if (errors.isEmpty()) repo.saveTemplate(template)
        return errors
    }
}

class DeleteTemplate(private val repo: TemplateRepository) {
    suspend operator fun invoke(id: String) = repo.deleteTemplate(id)
}
