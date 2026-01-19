package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts_ledger")
data class ContactLedger(
    @PrimaryKey(autoGenerate = true)
    val contactId: Long = 0,
    val name: String,
    val phone: String,
    val photoUri: String? = null,
    val upiId: String? = null,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBalance: Double = 0.0, // positive means user will receive, negative means user has to pay
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "loan_transactions")
data class LoanTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val amount: Double,
    val type: LoanType, // LENT or BORROWED
    val date: Long,
    val remark: String? = null,
    val isSettled: Boolean = false,
    val partialSettledAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class LoanType {
    LENT, BORROWED
}
