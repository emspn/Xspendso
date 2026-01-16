package com.app.xspendso.sms

import com.app.xspendso.data.TransactionEntity
import kotlin.math.abs
import kotlin.math.round

object TransactionMergeEngine {
    private const val TIME_THRESHOLD_SEC = 300 // 5 minutes for SMS drift

    /**
     * BANK-GRADE CROSS-SOURCE MERGE ENGINE
     * Prevents duplicates by matching Amount, Timestamp (with drift), and Type.
     * Prioritizes high-quality information (Merchant Names, Bank Sources).
     */
    fun merge(existing: List<TransactionEntity>, incoming: List<TransactionEntity>): List<TransactionEntity> {
        val finalLedger = existing.toMutableList()

        incoming.forEach { newTx ->
            // High-precision duplicate detection:
            // Match if: Same Amount AND (Same exact timestamp OR within drift window)
            val existingIndex = finalLedger.indexOfFirst { oldTx ->
                val amountMatches = round(oldTx.amount * 100) == round(newTx.amount * 100)
                val timeDrift = abs(oldTx.timestamp - newTx.timestamp) <= TIME_THRESHOLD_SEC * 1000
                val sameType = oldTx.type == newTx.type
                
                amountMatches && timeDrift && sameType
            }

            if (existingIndex == -1) {
                // Not a duplicate, add it to the ledger
                finalLedger.add(newTx)
            } else {
                // It is a duplicate, perform "Information Enrichment"
                val existingTx = finalLedger[existingIndex]
                
                // 1. Prioritize Merchant Name (Don't let "Unknown" overwrite a known name)
                val bestCounterparty = if (existingTx.counterparty == "Unknown" && newTx.counterparty != "Unknown") {
                    newTx.counterparty
                } else {
                    existingTx.counterparty
                }

                // 2. Prioritize Account Source (Keep specific bank names over generic ones)
                val bestAccountSource = if (isGenericSource(existingTx.accountSource) && !isGenericSource(newTx.accountSource)) {
                    newTx.accountSource
                } else {
                    existingTx.accountSource
                }

                // 3. Keep any existing manual remarks or take new ones if empty
                val bestRemark = existingTx.remark ?: newTx.remark

                // Update the existing record with the best data from both sources
                finalLedger[existingIndex] = existingTx.copy(
                    counterparty = bestCounterparty,
                    accountSource = bestAccountSource,
                    remark = bestRemark,
                    category = if (existingTx.category == "General" && newTx.category != "General") newTx.category else existingTx.category,
                    balanceAfter = existingTx.balanceAfter ?: newTx.balanceAfter
                )
            }
        }
        return finalLedger.sortedByDescending { it.timestamp }
    }

    private fun isGenericSource(source: String): Boolean {
        val lower = source.lowercase()
        return lower.contains("upi") || lower.contains("gpay") || lower.contains("paytm") || lower.contains("unknown")
    }
}
