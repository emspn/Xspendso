package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import kotlin.math.abs

data class AccountBreakdown(
    val accountName: String,
    val totalSpent: Double,
    val totalReceived: Double,
    val latestBalance: Double?
)

class GetAccountBreakdownUseCase {
    operator fun invoke(transactions: List<TransactionEntity>): List<AccountBreakdown> {
        return transactions.groupBy { it.accountSource }.map { (account, txs) ->
            val spent = txs.filter { it.type == "DEBIT" }.sumOf { abs(it.amount) }
            val received = txs.filter { it.type == "CREDIT" }.sumOf { abs(it.amount) }
            val latestTx = txs.maxByOrNull { it.timestamp }
            
            AccountBreakdown(
                accountName = account,
                totalSpent = spent,
                totalReceived = received,
                latestBalance = latestTx?.balanceAfter
            )
        }.sortedByDescending { it.totalSpent }
    }
}
