package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.util.*
import kotlin.math.abs

data class DayOfWeekSpend(
    val dayName: String,
    val amount: Double,
    val dayIndex: Int // 1 for Sunday, 7 for Saturday
)

class GetSpendingByDayOfWeekUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<DayOfWeekSpend> {
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val calendar = Calendar.getInstance()
        
        val grouped = transactions
            .filter { it.type == "DEBIT" }
            .groupBy { 
                calendar.timeInMillis = it.timestamp
                calendar.get(Calendar.DAY_OF_WEEK)
            }

        return (1..7).map { dayIndex ->
            DayOfWeekSpend(
                dayName = days[dayIndex - 1],
                amount = grouped[dayIndex]?.sumOf { abs(it.amount) } ?: 0.0,
                dayIndex = dayIndex
            )
        }
    }
}
