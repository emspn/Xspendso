package com.app.xspendso.domain.usecase

import com.app.xspendso.data.PrefsManager
import com.app.xspendso.domain.DomainError
import com.app.xspendso.domain.Resource
import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.sms.SmsReader
import com.app.xspendso.sms.TransactionMergeEngine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.round

data class SyncResult(
    val newTransactionsCount: Int,
    val totalSpent: Double
)

class SyncLedgerUseCase @Inject constructor(
    private val smsReader: SmsReader,
    private val repository: TransactionRepository,
    private val detectRecurringTransactionsUseCase: DetectRecurringTransactionsUseCase,
    private val categorizeTransactionUseCase: CategorizeTransactionUseCase,
    private val prefsManager: PrefsManager
) {
    suspend operator fun invoke(): Resource<SyncResult> {
        return try {
            // 1. Ingest raw data from SMS (Delta Sync)
            val lastSmsTime = prefsManager.lastSmsSyncTimestamp
            val smsTransactions = try {
                smsReader.readTransactions(since = lastSmsTime)
            } catch (e: SecurityException) {
                return Resource.Error(DomainError.SyncError.SmsReadPermissionDenied)
            } catch (e: Exception) {
                return Resource.Error(DomainError.SyncError.UnexpectedSyncError(e))
            }
            
            if (smsTransactions.isEmpty()) {
                return Resource.Success(SyncResult(0, 0.0))
            }

            // 2. Update Sync Timestamp
            val latestSmsTime = smsTransactions.maxOfOrNull { it.timestamp } ?: lastSmsTime
            prefsManager.lastSmsSyncTimestamp = latestSmsTime
            
            // 3. Get all custom categorization rules
            val customRules = repository.getAllRules().first()

            // 4. Apply Local Correction Patterns, Custom Rules, and fallback Categorization
            val enrichedTransactions = smsTransactions.map { tx ->
                // Priority 1: Custom User Rules
                val matchingRule = customRules.find { tx.counterparty.contains(it.merchantPattern, ignoreCase = true) }
                if (matchingRule != null) {
                    return@map tx.copy(category = matchingRule.category, enrichedSource = "RULE")
                }

                // Priority 2: Correction Patterns
                val amountRounded = round(tx.amount)
                val timeBucket = tx.timestamp / (10 * 60 * 1000)
                val pattern = repository.findMatchingPattern(amountRounded, timeBucket, tx.accountSource)
                
                val patternEnrichedTx = if (pattern != null) {
                    tx.copy(
                        counterparty = pattern.correctedCounterparty,
                        category = pattern.correctedCategory ?: tx.category,
                        enrichedSource = "USER"
                    )
                } else tx

                // Priority 3: Automated Categorization
                if (patternEnrichedTx.category == "General") {
                    patternEnrichedTx.copy(category = categorizeTransactionUseCase(patternEnrichedTx))
                } else {
                    patternEnrichedTx
                }
            }

            // 5. Match and Merge with Existing Database Records
            val dbTransactions = repository.getAllTransactions().first()
            val finalLedger = TransactionMergeEngine.merge(dbTransactions, enrichedTransactions)
            
            // 6. Run Subscription Detection AI
            val ledgerWithRecurring = detectRecurringTransactionsUseCase(finalLedger)
            
            // 7. Bulk update the ledger
            try {
                repository.insertTransactions(ledgerWithRecurring)
            } catch (e: Exception) {
                return Resource.Error(DomainError.SyncError.DatabaseWriteError)
            }

            // 8. Calculate Delta
            val newCount = enrichedTransactions.size
            val deltaSpent = enrichedTransactions.filter { it.type == "DEBIT" }.sumOf { abs(it.amount) }

            Resource.Success(SyncResult(newCount, deltaSpent))
        } catch (e: Exception) {
            Resource.Error(DomainError.SyncError.UnexpectedSyncError(e))
        }
    }
}
