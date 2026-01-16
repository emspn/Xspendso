package com.app.xspendso.domain.usecase

import com.app.xspendso.data.PrefsManager
import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.sms.SmsReader
import com.app.xspendso.sms.TransactionMergeEngine
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.round

data class SyncResult(
    val newTransactionsCount: Int,
    val totalSpent: Double
)

class SyncLedgerUseCase(
    private val smsReader: SmsReader,
    private val repository: TransactionRepository,
    private val detectRecurringTransactionsUseCase: DetectRecurringTransactionsUseCase,
    private val prefsManager: PrefsManager
) {
    suspend operator fun invoke(): SyncResult {
        // 1. Ingest raw data from SMS (Delta Sync)
        val lastSmsTime = prefsManager.lastSmsSyncTimestamp
        val smsTransactions = smsReader.readTransactions(since = lastSmsTime)
        
        if (smsTransactions.isEmpty()) {
            return SyncResult(0, 0.0)
        }

        // 2. Update Sync Timestamp
        val latestSmsTime = smsTransactions.maxOfOrNull { it.timestamp } ?: lastSmsTime
        prefsManager.lastSmsSyncTimestamp = latestSmsTime
        
        // 3. Get all custom categorization rules
        val customRules = repository.getAllRules().first()

        // 4. Apply Local Correction Patterns and Custom Rules to incoming data
        val patternEnriched = smsTransactions.map { tx ->
            // Try matching custom merchant rules first
            val matchingRule = customRules.find { tx.counterparty.contains(it.merchantPattern, ignoreCase = true) }
            if (matchingRule != null) {
                return@map tx.copy(category = matchingRule.category, enrichedSource = "RULE")
            }

            // Try matching user correction patterns
            val amountRounded = round(tx.amount)
            val timeBucket = tx.timestamp / (10 * 60 * 1000)
            val pattern = repository.findMatchingPattern(amountRounded, timeBucket, tx.accountSource)
            
            if (pattern != null) {
                tx.copy(
                    counterparty = pattern.correctedCounterparty,
                    category = pattern.correctedCategory ?: tx.category,
                    enrichedSource = "USER"
                )
            } else tx
        }

        // 5. Match and Merge with Existing Database Records
        val dbTransactions = repository.getAllTransactions().first()
        val finalLedger = TransactionMergeEngine.merge(dbTransactions, patternEnriched)
        
        // 6. Run Subscription Detection AI
        val ledgerWithRecurring = detectRecurringTransactionsUseCase(finalLedger)
        
        // 7. Bulk update the ledger
        repository.insertTransactions(ledgerWithRecurring)

        // 8. Calculate Delta for Notification
        val newCount = patternEnriched.size
        val deltaSpent = patternEnriched.filter { it.type == "DEBIT" }.sumOf { abs(it.amount) }

        return SyncResult(newCount, deltaSpent)
    }
}
