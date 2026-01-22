package com.app.xspendso.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {
    // Contact Ledger
    @Query("SELECT * FROM contacts_ledger ORDER BY lastUpdated DESC")
    fun getAllContacts(): Flow<List<ContactLedger>>

    @Query("SELECT * FROM contacts_ledger")
    suspend fun getAllContactsList(): List<ContactLedger>

    @Query("SELECT * FROM contacts_ledger WHERE contactId = :contactId")
    suspend fun getContactById(contactId: Long): ContactLedger?

    @Query("SELECT * FROM contacts_ledger WHERE uuid = :uuid")
    suspend fun getContactByUuid(uuid: String): ContactLedger?

    @Query("SELECT * FROM contacts_ledger WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactLedger?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactLedger): Long

    @Update
    suspend fun updateContact(contact: ContactLedger)

    @Delete
    suspend fun deleteContact(contact: ContactLedger)

    // Loan Transactions
    @Query("SELECT * FROM loan_transactions WHERE contactId = :contactId ORDER BY date DESC")
    fun getTransactionsByContactId(contactId: Long): Flow<List<LoanTransaction>>

    @Query("SELECT * FROM loan_transactions")
    suspend fun getAllLoanTransactionsList(): List<LoanTransaction>

    @Query("SELECT * FROM loan_transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): LoanTransaction?

    @Query("SELECT * FROM loan_transactions WHERE uuid = :uuid")
    suspend fun getTransactionByUuid(uuid: String): LoanTransaction?

    @Query("SELECT * FROM loan_transactions WHERE sourceTransactionId = :sourceId")
    suspend fun getTransactionsBySourceId(sourceId: Long): List<LoanTransaction>

    @Query("DELETE FROM loan_transactions WHERE sourceTransactionId = :sourceId")
    suspend fun deleteTransactionsBySourceId(sourceId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanTransaction(transaction: LoanTransaction): Long

    @Update
    suspend fun updateLoanTransaction(transaction: LoanTransaction)

    @Delete
    suspend fun deleteLoanTransaction(transaction: LoanTransaction)

    @Query("DELETE FROM loan_transactions WHERE id IN (:transactionIds)")
    suspend fun deleteMultipleTransactionsInternal(transactionIds: List<Long>)

    @Transaction
    suspend fun addLoanTransactionAndUpdateBalance(transaction: LoanTransaction) {
        val contact = getContactById(transaction.contactId)
        val contactUuid = contact?.uuid ?: ""
        insertLoanTransaction(transaction.copy(contactUuid = contactUuid, lastUpdated = System.currentTimeMillis()))
        recalculateContactBalance(transaction.contactId)
    }

    @Transaction
    suspend fun deleteSourceSplitAndUpdateBalances(sourceId: Long) {
        val affectedContacts = getTransactionsBySourceId(sourceId).map { it.contactId }.distinct()
        deleteTransactionsBySourceId(sourceId)
        affectedContacts.forEach { recalculateContactBalance(it) }
    }

    @Transaction
    suspend fun updateLoanTransactionAndUpdateBalance(transaction: LoanTransaction) {
        updateLoanTransaction(transaction.copy(lastUpdated = System.currentTimeMillis()))
        recalculateContactBalance(transaction.contactId)
    }

    @Transaction
    suspend fun deleteLoanTransactionAndUpdateBalance(transactionId: Long) {
        val transaction = getTransactionById(transactionId) ?: return
        deleteLoanTransaction(transaction)
        recalculateContactBalance(transaction.contactId)
    }

    @Transaction
    suspend fun deleteMultipleTransactionsAndUpdateBalance(contactId: Long, transactionIds: List<Long>) {
        deleteMultipleTransactionsInternal(transactionIds)
        recalculateContactBalance(contactId)
    }

    @Transaction
    suspend fun toggleTransactionSettlement(transactionId: Long) {
        val transaction = getTransactionById(transactionId) ?: return
        val updatedTx = transaction.copy(isSettled = !transaction.isSettled, lastUpdated = System.currentTimeMillis())
        updateLoanTransaction(updatedTx)
        recalculateContactBalance(transaction.contactId)
    }

    @Transaction
    suspend fun recalculateContactBalance(contactId: Long) {
        val contact = getContactById(contactId) ?: return
        val txs = getAllTransactionsForContactInternal(contactId)
        
        var totalLent = 0.0
        var totalBorrowed = 0.0
        
        txs.forEach { tx ->
            val remainingAmount = tx.amount - tx.partialSettledAmount
            if (!tx.isSettled && remainingAmount > 0) {
                if (tx.type == LoanType.LENT) {
                    totalLent += remainingAmount
                } else {
                    totalBorrowed += remainingAmount
                }
            }
        }
        
        val newNetBalance = totalLent - totalBorrowed
        
        updateContact(contact.copy(
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            netBalance = newNetBalance,
            lastUpdated = System.currentTimeMillis()
        ))
    }

    @Query("SELECT * FROM loan_transactions WHERE contactId = :contactId ORDER BY date ASC")
    suspend fun getAllTransactionsForContactInternal(contactId: Long): List<LoanTransaction>

    @Transaction
    suspend fun settleLoanTransaction(contactId: Long, amountToSettle: Double, typeToSettle: LoanType) {
        val contact = getContactById(contactId) ?: return
        val settlementTx = LoanTransaction(
            contactId = contactId,
            contactUuid = contact.uuid,
            amount = amountToSettle,
            type = if (typeToSettle == LoanType.LENT) LoanType.BORROWED else LoanType.LENT,
            date = System.currentTimeMillis(),
            remark = "Settlement Payment",
            isSettled = true,
            lastUpdated = System.currentTimeMillis()
        )
        insertLoanTransaction(settlementTx)

        val txs = getAllTransactionsForContactInternal(contactId)
            .filter { it.type == typeToSettle && !it.isSettled }
        
        var remainingSettleAmount = amountToSettle
        
        for (tx in txs) {
            if (remainingSettleAmount <= 0) break
            
            val txRemaining = tx.amount - tx.partialSettledAmount
            if (txRemaining <= remainingSettleAmount) {
                updateLoanTransaction(tx.copy(
                    partialSettledAmount = tx.amount,
                    isSettled = true,
                    lastUpdated = System.currentTimeMillis()
                ))
                remainingSettleAmount -= txRemaining
            } else {
                updateLoanTransaction(tx.copy(
                    partialSettledAmount = tx.partialSettledAmount + remainingSettleAmount,
                    lastUpdated = System.currentTimeMillis()
                ))
                remainingSettleAmount = 0.0
            }
        }
        
        recalculateContactBalance(contactId)
    }
}
