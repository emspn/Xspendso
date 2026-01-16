package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import kotlin.math.abs

class GetMonthlyAnalyticsUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): Map<String, Double> {
        // Transactions are already filtered by time in the ViewModel
        return transactions
            .filter { it.type == "DEBIT" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
    }
}
