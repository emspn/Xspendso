package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class DailyTrend(
    val dateLabel: String,
    val amount: Double,
    val timestamp: Long
)

class GetSpendingTrendsUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<DailyTrend> {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        
        return transactions
            .filter { it.type == "DEBIT" }
            .groupBy { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .map { (startOfDay, dayTxs) ->
                DailyTrend(
                    dateLabel = sdf.format(Date(startOfDay)),
                    amount = dayTxs.sumOf { abs(it.amount) },
                    timestamp = startOfDay
                )
            }
            .sortedBy { it.timestamp }
    }
}
