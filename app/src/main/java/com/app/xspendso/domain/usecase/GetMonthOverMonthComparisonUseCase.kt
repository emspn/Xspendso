package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.*
import kotlin.math.abs

data class ComparisonResult(
    val category: String,
    val currentMonthAmount: Double,
    val previousMonthAmount: Double,
    val percentageChange: Float
)

class GetMonthOverMonthComparisonUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<ComparisonResult> {
        val calendar = Calendar.getInstance()
        
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.add(Calendar.MONTH, -1)
        val prevMonth = calendar.get(Calendar.MONTH)
        val prevYear = calendar.get(Calendar.YEAR)

        val currentTxs = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val prevTxs = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }

        val categories = (currentTxs + prevTxs).map { it.category }.distinct()

        return categories.map { category ->
            val currentAmount = currentTxs.filter { it.category == category && it.type == "DEBIT" }.sumOf { abs(it.amount) }
            val prevAmount = prevTxs.filter { it.category == category && it.type == "DEBIT" }.sumOf { abs(it.amount) }
            
            val change = if (prevAmount > 0) {
                ((currentAmount - prevAmount) / prevAmount * 100).toFloat()
            } else 0f

            ComparisonResult(category, currentAmount, prevAmount, change)
        }.sortedByDescending { abs(it.percentageChange) }
    }
}
