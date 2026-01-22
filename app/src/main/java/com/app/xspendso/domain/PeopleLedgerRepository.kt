package com.app.xspendso.domain

import com.app.xspendso.data.ContactLedger
import com.app.xspendso.data.LoanTransaction
import com.app.xspendso.data.LoanType
import kotlinx.coroutines.flow.Flow

interface PeopleLedgerRepository {
    fun getAllContacts(): Flow<List<ContactLedger>>
    fun getTransactionsForContact(contactId: Long): Flow<List<LoanTransaction>>
    
    suspend fun addContact(name: String, phone: String, photoUri: String?): Resource<Unit>
    suspend fun updateContactUpi(contactId: Long, upiId: String): Resource<Unit>
    suspend fun updateContactName(contactId: Long, newName: String): Resource<Unit>
    suspend fun deleteContact(contact: ContactLedger): Resource<Unit>
    
    suspend fun addLoanTransaction(contactId: Long, amount: Double, type: LoanType, remark: String?, date: Long): Resource<Unit>
    suspend fun updateLoanTransaction(transaction: LoanTransaction): Resource<Unit>
    suspend fun deleteLoanTransaction(transactionId: Long): Resource<Unit>
    suspend fun deleteMultipleLoanTransactions(contactId: Long, transactionIds: List<Long>): Resource<Unit>
    suspend fun toggleSettlement(transactionId: Long): Resource<Unit>
    suspend fun settlePartialAmount(contactId: Long, amount: Double, type: LoanType): Resource<Unit>
    
    suspend fun syncProfile(): Resource<Unit>
    suspend fun syncWithCloud(): Resource<Unit>
    fun getSyncStatus(): Flow<SyncStatus>
}

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, ERROR
}
