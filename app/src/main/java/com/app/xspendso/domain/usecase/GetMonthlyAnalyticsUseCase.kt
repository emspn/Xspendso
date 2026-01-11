package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.*

class GetMonthlyAnalyticsUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): Map<String, Double> {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return transactions
            .filter { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { Math.abs(it.amount) } }
    }
}