package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DetectRecurringTransactionsUseCase {
    /**
     * ADVANCED SUBSCRIPTION DETECTION ENGINE
     * Identifies regular intervals (Monthly, Weekly, Quarterly)
     * Handles variance in amounts (e.g., utility bills) and dates (e.g., weekend shifts).
     */
    operator fun invoke(transactions: List<TransactionEntity>): List<TransactionEntity> {
        val groupedByMerchant = transactions.filter { it.type == "DEBIT" }.groupBy { it.counterparty }
        val recurringIds = mutableSetOf<Long>()

        groupedByMerchant.forEach { (merchant, txs) ->
            if (merchant == "Unknown" || txs.size < 2) return@forEach
            
            val sortedTxs = txs.sortedByDescending { it.timestamp }
            
            // Check for Monthly cadence (standard subscriptions like Netflix, Rent)
            // Interval: 25 to 35 days to handle different month lengths and weekends
            if (hasIntervalMatch(sortedTxs, 25..35)) {
                markAllAsRecurring(sortedTxs, recurringIds)
                return@forEach
            }

            // Check for Weekly cadence (Standard for gym, specific habits)
            if (hasIntervalMatch(sortedTxs, 6..8)) {
                markAllAsRecurring(sortedTxs, recurringIds)
                return@forEach
            }

            // Check for Quarterly (90 days +/- 5)
            if (hasIntervalMatch(sortedTxs, 85..95)) {
                markAllAsRecurring(sortedTxs, recurringIds)
                return@forEach
            }
        }

        return transactions.map { 
            if (it.id in recurringIds) it.copy(isRecurring = true) else it 
        }
    }

    private fun hasIntervalMatch(txs: List<TransactionEntity>, dayRange: IntRange): Boolean {
        for (i in 0 until txs.size - 1) {
            val current = txs[i]
            val previous = txs[i + 1]
            val timeDiffDays = TimeUnit.MILLISECONDS.toDays(abs(current.timestamp - previous.timestamp))
            
            // If we find even one pair matching the interval with a similar amount (within 20% variance)
            // Or exact same amount for fixed subs.
            val amountVariance = abs(current.amount - previous.amount) / abs(previous.amount).coerceAtLeast(1.0)
            
            if (timeDiffDays in dayRange && (amountVariance < 0.20 || abs(current.amount - previous.amount) < 2.0)) {
                return true
            }
        }
        return false
    }

    private fun markAllAsRecurring(txs: List<TransactionEntity>, ids: MutableSet<Long>) {
        txs.forEach { ids.add(it.id) }
    }
}
