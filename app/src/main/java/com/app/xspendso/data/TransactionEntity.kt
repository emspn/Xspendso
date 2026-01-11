package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val category: String,
    val amount: Double,
    val timestamp: Long,
    val method: String,
    val type: String, // DEBIT/CREDIT
    val source: String // SMS/MANUAL
)