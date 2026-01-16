package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.*
import kotlin.math.abs

data class MerchantAnalytics(
    val merchantName: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val lastTransactionDate: Long,
    val category: String
)

class GetMerchantAnalyticsUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<MerchantAnalytics> {
        return transactions
            .filter { it.type == "DEBIT" }
            .groupBy { it.counterparty }
            .map { (merchant, txs) ->
                MerchantAnalytics(
                    merchantName = merchant,
                    totalSpent = txs.sumOf { abs(it.amount) },
                    transactionCount = txs.size,
                    lastTransactionDate = txs.maxOf { it.timestamp },
                    category = txs.first().category
                )
            }
            .sortedByDescending { it.totalSpent }
    }
}
