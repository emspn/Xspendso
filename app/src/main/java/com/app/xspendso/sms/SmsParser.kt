package com.app.xspendso.sms

import com.app.xspendso.data.TransactionEntity

object SmsParser {
    
    private val AMOUNT_REGEX = Regex("""(?i)(?:rs\.?|inr|amt)\.?\s*([\d,]+(?:\.\d{1,2})?)""")
    private val MERCHANT_REGEX_AT = Regex("""(?i)at\s+([^,.\n]+)""")
    private val MERCHANT_REGEX_TO = Regex("""(?i)to\s+([^,.\n]+)""")

    fun parse(body: String, address: String, date: Long): TransactionEntity? {
        val lowercaseBody = body.lowercase()
        
        // Check if it's a transaction SMS
        val isDebit = lowercaseBody.contains("debited") || lowercaseBody.contains("spent") || lowercaseBody.contains("used at")
        val isCredit = lowercaseBody.contains("credited") || lowercaseBody.contains("received")
        
        if (!isDebit && !isCredit) return null

        val amount = extractAmount(body) ?: return null
        val type = if (isCredit) "CREDIT" else "DEBIT"
        
        // Determine Merchant
        var merchant = extractMerchant(body)
        if (merchant.isBlank()) {
            merchant = address // Fallback to sender ID
        }

        val method = when {
            lowercaseBody.contains("upi") -> "UPI"
            lowercaseBody.contains("card") || lowercaseBody.contains("ending in") -> "Card"
            else -> "Bank"
        }

        // Clean up merchant name (take first two words usually)
        val cleanMerchant = merchant.split(" ").take(2).joinToString(" ").trim()

        return TransactionEntity(
            merchant = cleanMerchant,
            category = "Others", // Will be categorized by use case
            amount = if (type == "DEBIT") -amount else amount,
            timestamp = date,
            method = method,
            type = type,
            source = "SMS"
        )
    }

    private fun extractAmount(body: String): Double? {
        return AMOUNT_REGEX.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun extractMerchant(body: String): String {
        // Try "at" first, then "to"
        val atMatch = MERCHANT_REGEX_AT.find(body)
        if (atMatch != null) return atMatch.groupValues[1].trim()
        
        val toMatch = MERCHANT_REGEX_TO.find(body)
        if (toMatch != null) return toMatch.groupValues[1].trim()
        
        return ""
    }
}