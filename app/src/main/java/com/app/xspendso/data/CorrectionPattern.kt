package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "correction_patterns",
    indices = [Index(value = ["amountRounded", "timeBucket", "accountSource"], unique = true)]
)
data class CorrectionPattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountRounded: Double,
    val timeBucket: Long,        // timestamp / (10 * 60 * 1000)
    val accountSource: String,
    val correctedCounterparty: String,
    val correctedCategory: String? = null
)
