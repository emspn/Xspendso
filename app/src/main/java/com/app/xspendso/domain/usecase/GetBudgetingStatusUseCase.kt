package com.app.xspendso.domain.usecase

import com.app.xspendso.data.BudgetEntity
import com.app.xspendso.data.TransactionEntity
import java.util.Calendar
import kotlin.math.abs

data class BudgetStatus(
    val totalLimit: Double,
    val totalSpent: Double,
    val remaining: Double,
    val percentageUsed: Float,
    val categoryStatus: Map<String, CategoryBudgetStatus>
)

data class CategoryBudgetStatus(
    val limit: Double,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Float
)

class GetBudgetingStatusUseCase {
    operator fun invoke(transactions: List<TransactionEntity>, budget: BudgetEntity?): BudgetStatus {
        if (budget == null) {
            return BudgetStatus(0.0, 0.0, 0.0, 0f, emptyMap())
        }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val currentMonthTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        // CRITICAL FIX: Only sum DEBIT transactions for spending
        val totalSpent = currentMonthTransactions
            .filter { it.type == "DEBIT" }
            .sumOf { abs(it.amount) }
        
        val categoryStatus = budget.categoryLimits.mapValues { (category, limit) ->
            val spent = currentMonthTransactions
                .filter { it.type == "DEBIT" && it.category == category }
                .sumOf { abs(it.amount) }
            
            CategoryBudgetStatus(
                limit = limit,
                spent = spent,
                remaining = (limit - spent).coerceAtLeast(0.0),
                percentageUsed = if (limit > 0) (spent / limit).toFloat() else 0f
            )
        }

        return BudgetStatus(
            totalLimit = budget.totalLimit,
            totalSpent = totalSpent,
            remaining = (budget.totalLimit - totalSpent).coerceAtLeast(0.0),
            percentageUsed = if (budget.totalLimit > 0) (totalSpent / budget.totalLimit).toFloat() else 0f,
            categoryStatus = categoryStatus
        )
    }
}
