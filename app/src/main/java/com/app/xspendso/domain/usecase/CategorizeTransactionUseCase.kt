package com.app.xspendso.domain.usecase

import com.app.xspendso.data.TransactionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorizeTransactionUseCase @Inject constructor() {
    
    private val categories = mapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "blinkit", "zepto", "starbucks", "mcdonalds", "kfc", 
            "burger king", "dominos", "pizza", "restaurant", "cafe", "dhaba", "eats", "bakery"
        ),
        "Travel" to listOf(
            "uber", "ola", "rapido", "irctc", "indigo", "air india", "vistara", 
            "makemytrip", "goibibo", "petrol", "shell", "iocl", "hpcl", "bpcl", "metro", "fastag"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "decathlon", 
            "reliance trends", "max fashion", "h&m", "zara", "tatacliq", "jiomart"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "bescom", "uppcl", "tneb", "water", "recharge", "jio", "airtel", 
            "vi ", "bsnl", "act fibernet", "broadband", "insurance", "lic", "bill"
        ),
        "Groceries" to listOf(
            "bigbasket", "dmart", "reliance smart", "more retail", "spencers", "grocery"
        ),
        "Entertainment" to listOf(
            "netflix", "prime video", "hotstar", "bookmyshow", "pvr", "inox", "spotify", "youtube premium"
        ),
        "Health" to listOf(
            "pharmeasy", "1mg", "apollo", "hospital", "pharmacy", "doctor", "clinic"
        ),
        "Investment" to listOf(
            "zerodha", "groww", "upstox", "angel one", "mutual fund", "sip", "stocks"
        )
    )

    operator fun invoke(transaction: TransactionEntity): String {
        val counterparty = transaction.counterparty.lowercase()
        val remark = (transaction.remark ?: "").lowercase()

        // 0. Merchant Keyword Check
        for ((category, keywords) in categories) {
            if (keywords.any { counterparty.contains(it) || remark.contains(it) }) {
                return category
            }
        }

        // 1. Special Logic for Transfers/Income
        if (transaction.type == "CREDIT") {
            if (remark.contains("refund") || counterparty.contains("refund")) return "Refund"
            return "Income"
        }
        
        if (transaction.method == "ATM") return "Cash Withdrawal"

        return "General"
    }
}
