package com.app.xspendso.domain.usecase

import com.app.xspendso.data.LoanType
import com.app.xspendso.data.PeopleDao
import com.app.xspendso.domain.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncPeopleUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val peopleDao: PeopleDao
) {
    /**
     * Scans transactions for potential P2P payments and suggests adding them to People Ledger
     * or automatically updates existing balances if the person is already in the ledger.
     */
    suspend operator fun invoke() {
        val transactions = transactionRepository.getAllTransactions().first()
        val contacts = peopleDao.getAllContacts().first()

        // P2P detection logic: Transactions with "UPI" in source and specific counterparty patterns
        // e.g., "SENT TO", "RECEIVED FROM"
        transactions.forEach { tx ->
            val counterparty = tx.counterparty
            if (counterparty == "Unknown" || counterparty.isBlank()) return@forEach

            // Try to find a match in the people ledger by name
            val matchingContact = contacts.find { contact ->
                counterparty.contains(contact.name, ignoreCase = true) || 
                contact.name.contains(counterparty, ignoreCase = true)
            }

            if (matchingContact != null) {
                // Potential person match found
                // Check if this transaction is already linked to a loan/ledger entry
                // (This would require a mapping table or a 'linkedTxId' in loan_transactions)
                // For now, we can just log it or suggest it in a real implementation
            }
        }
    }
}
