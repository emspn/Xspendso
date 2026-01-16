package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.*

data class BalancePoint(
    val timestamp: Long,
    val balance: Double
)

class GetBalanceHistoryUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<BalancePoint> {
        // Filter transactions that have a balance snapshot and sort by time
        return transactions
            .filter { it.balanceAfter != null }
            .map { BalancePoint(it.timestamp, it.balanceAfter!!) }
            .sortedBy { it.timestamp }
    }
}
