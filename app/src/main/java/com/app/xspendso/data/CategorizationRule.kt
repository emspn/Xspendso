package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorization_rules")
data class CategorizationRule(
    @PrimaryKey
    val merchantPattern: String, // e.g., "swiggy", "uber"
    val category: String
)
