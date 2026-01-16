package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.Calendar
import kotlin.math.abs

data class SavingsPrediction(
    val currentSpent: Double,
    val predictedSpent: Double,
    val predictedSavings: Double,
    val status: String, // "SAFE", "WARNING", "OVER_BUDGET"
    val dailyBudget: Double,
    val remainingDays: Int
)

class PredictMonthEndSavingsUseCase {
    operator fun invoke(transactions: List<TransactionEntity>, totalLimit: Double): SavingsPrediction {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val remainingDays = (daysInMonth - currentDay).coerceAtLeast(0)

        val currentMonthTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear && it.type == "DEBIT"
        }

        val currentMonthSpent = currentMonthTransactions.sumOf { abs(it.amount) }

        if (totalLimit <= 0) return SavingsPrediction(currentMonthSpent, 0.0, 0.0, "UNKNOWN", 0.0, remainingDays)

        // Calculate Average Burn Rate
        // Logic: Total spent divided by days passed. Min 1 day to avoid division by zero.
        val daysPassed = if (currentDay > 0) currentDay else 1
        val averageDailySpend = currentMonthSpent / daysPassed
        
        // Linear extrapolation
        val predictedSpent = averageDailySpend * daysInMonth
        val predictedSavings = totalLimit - predictedSpent

        // Dynamic Daily Allowance: How much can I spend PER DAY for the remaining days?
        val remainingBudget = (totalLimit - currentMonthSpent).coerceAtLeast(0.0)
        val dailyBudget = if (remainingDays > 0) remainingBudget / remainingDays else remainingBudget

        val status = when {
            predictedSpent > totalLimit -> "OVER_BUDGET"
            predictedSpent > totalLimit * 0.9 -> "WARNING"
            else -> "SAFE"
        }

        return SavingsPrediction(
            currentSpent = currentMonthSpent,
            predictedSpent = predictedSpent,
            predictedSavings = predictedSavings,
            status = status,
            dailyBudget = dailyBudget,
            remainingDays = remainingDays
        )
    }
}
