package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.entity.FieldEntity
import com.example.data.entity.RuleEntity
import com.example.data.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM template ORDER BY name")
    fun observeTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun getTemplate(id: String): TemplateEntity?

    @Query("SELECT * FROM field WHERE templateId = :id ORDER BY position")
    suspend fun fields(id: String): List<FieldEntity>

    @Query("SELECT * FROM rule WHERE templateId = :id ORDER BY position")
    suspend fun rules(id: String): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFields(fields: List<FieldEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<RuleEntity>)

    @Query("DELETE FROM field WHERE templateId = :id")
    suspend fun clearFields(id: String)

    @Query("DELETE FROM rule WHERE templateId = :id")
    suspend fun clearRules(id: String)

    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Transaction
    suspend fun saveTemplate(template: TemplateEntity, fields: List<FieldEntity>, rules: List<RuleEntity>) {
        upsertTemplate(template)
        clearFields(template.id); upsertFields(fields)
        clearRules(template.id); upsertRules(rules)
    }
}
