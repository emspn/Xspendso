package com.app.xspendso.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CorrectionDao {
    @Query("SELECT * FROM correction_patterns")
    fun getAllPatterns(): Flow<List<CorrectionPattern>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: CorrectionPattern)

    @Query("SELECT * FROM correction_patterns WHERE amountRounded = :amount AND timeBucket = :timeBucket AND accountSource = :source")
    suspend fun findMatchingPattern(amount: Double, timeBucket: Long, source: String): CorrectionPattern?

    @Query("DELETE FROM correction_patterns")
    suspend fun deleteAllPatterns()
}
