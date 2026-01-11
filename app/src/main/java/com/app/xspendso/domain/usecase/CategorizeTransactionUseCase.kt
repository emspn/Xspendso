package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity

class CategorizeTransactionUseCase {
    operator fun invoke(transaction: TransactionEntity): String {
        val merchant = transaction.merchant.lowercase()
        return when {
            merchant.contains("swiggy") || merchant.contains("zomato") || merchant.contains("restaurant") -> "Food & Dining"
            merchant.contains("uber") || merchant.contains("ola") || merchant.contains("petrol") -> "Travel"
            merchant.contains("amazon") || merchant.contains("flipkart") || merchant.contains("myntra") -> "Shopping"
            merchant.contains("electricity") || merchant.contains("water") || merchant.contains("recharge") -> "Bills & Utilities"
            else -> "Others"
        }
    }
}