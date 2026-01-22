package com.app.xspendso.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "contacts_ledger",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class ContactLedger(
    @PrimaryKey(autoGenerate = true)
    val contactId: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val photoUri: String? = null,
    val upiId: String? = null,
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val netBalance: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "loan_transactions",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class LoanTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val contactId: Long,
    val contactUuid: String = "",
    val amount: Double,
    val type: LoanType,
    val date: Long,
    val remark: String? = null,
    val isSettled: Boolean = false,
    val partialSettledAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val sourceTransactionId: Long? = null
)

enum class LoanType {
    LENT, BORROWED
}
