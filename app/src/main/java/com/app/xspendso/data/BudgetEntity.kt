package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val monthYear: String, // format: "MM-YYYY"
    val totalLimit: Double,
    val categoryLimits: Map<String, Double> = emptyMap()
)
