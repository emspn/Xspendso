package com.app.xspendso.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorizationRuleDao {
    @Query("SELECT * FROM categorization_rules")
    fun getAllRules(): Flow<List<CategorizationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategorizationRule)

    @Delete
    suspend fun deleteRule(rule: CategorizationRule)

    @Query("DELETE FROM categorization_rules")
    suspend fun deleteAllRules()
}
