package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["accountSource", "amount", "timestamp"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountSource: String,      // My bank / wallet (CBI, SBI, HDFC, GPay, Email)
    val counterparty: String,       // Who I paid or received from (Swiggy, Amazon, Person)
    val category: String,           // Expense category
    val amount: Double,
    val timestamp: Long,
    val method: String,             // UPI / Card / ATM / Bank / Email
    val type: String,               // DEBIT / CREDIT
    val remark: String? = null,
    val enrichedSource: String,     // SMS / EMAIL / USER
    val isRecurring: Boolean = false,
    val balanceAfter: Double? = null // New: Track balance snapshot if available in SMS
)
