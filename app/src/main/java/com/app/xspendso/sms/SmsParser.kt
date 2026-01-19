package com.app.xspendso.sms

import com.app.xspendso.data.TransactionEntity
import com.app.xspendso.domain.usecase.CategorizeTransactionUseCase

object SmsParser {
    private val DEBIT_VERBS = listOf("debited", "spent", "paid", "withdrawn", "dr a/c", "sent to", "purchase", "txn of", "payment of", "towards")
    private val CREDIT_VERBS = listOf("credited", "received", "added", "cr a/c", "remittance", "deposited", "refunded", "refund", "cashback")
    
    private val REJECT_WORDS = listOf(
        "otp", "verification", "login", "code is", "is your", "security code", 
        "bill generated", "get up to", "win", "offer", "apply", "congratulations",
        "limited period", "click", "recharge via", "cashback now", "claim", "upto", 
        "discount", "survey", "available", "valid until", "chance to", "rewards"
    )
    
    private val BANK_KEYWORDS = listOf(
        "ACB", "ALB", "ANB", "APN", "AXB", "BOB", "BOI", "BOM", "BMB", "CNB", 
        "CBOI", "CRB", "DCB", "DNB", "FBL", "GSC", "HCB", "HDF", "ICI", "IDB", 
        "INB", "IOB", "IIB", "ING", "JSB", "KTB", "KVB", "KMB", "MUC", "NTB", 
        "NGB", "OBC", "PMC", "PSB", "PNB", "RBL", "SRC", "SBJ", "SBH", "SBI", 
        "SBM", "SBP", "SBT", "SYB", "TMB", "SIB", "UCO", "UOB", "UBI", "VJB", 
        "YBL", "HDFC", "AXIS", "KOTAK", "PAYTM", "GPAY", "BANK", "FIPL", "IPB", "ICICI"
    )

    fun parse(body: String, sender: String, date: Long): TransactionEntity? {
        val lowerBody = body.lowercase().replace("\n", " ").trim()
        
        // 1. Structural Filter (Block promotional garbage)
        if (REJECT_WORDS.any { lowerBody.contains(it) }) return null
        
        // 2. Identify if it's a Bank Message
        val isBankContext = lowerBody.contains(Regex("""a/c|acct|ending|account|balance|total bal|avl bal|clr bal"""))
        
        // 3. Determine Transaction Type (Debit/Credit)
        val isRefund = lowerBody.contains("refund")
        val hasDebit = DEBIT_VERBS.any { lowerBody.contains(it) }
        val hasCredit = CREDIT_VERBS.any { lowerBody.contains(it) }
        
        val type = when {
            isRefund -> "CREDIT"
            hasDebit && hasCredit -> "DEBIT" 
            hasDebit -> "DEBIT"
            hasCredit -> "CREDIT"
            else -> return null
        }

        // 4. Extract Account Source (Bank Name)
        val cleanSender = getCleanSender(sender)
        var accountSource = cleanSender
        
        val bankAtEndMatch = Regex("""[-\s]([A-Za-z]{3,10})\.?\s*$""").find(body)
        val bankAtEnd = bankAtEndMatch?.groupValues?.get(1)?.uppercase()
        if (bankAtEnd != null && bankAtEnd.length >= 3 && (BANK_KEYWORDS.any { bankAtEnd.contains(it) } || isBankContext)) {
            accountSource = bankAtEnd
        }
        
        if (accountSource.length < 3 && cleanSender.length >= 3) {
            accountSource = cleanSender
        }

        // 5. Amount Extraction
        val amount = extractBestAmount(lowerBody, type == "DEBIT") ?: return null

        // 6. Counterparty Extraction
        val counterparty = extractMerchant(lowerBody, cleanSender, isBankContext)

        // 7. Balance & Account Ending for Remarks
        val balance = extractBalance(lowerBody)
        val accEnding = extractAccountEnding(lowerBody)
        val remarkParts = mutableListOf<String>()
        if (accEnding != null) remarkParts.add("A/c: $accEnding")
        if (balance != null) remarkParts.add("Bal: Rs. $balance")
        val remark = if (remarkParts.isNotEmpty()) remarkParts.joinToString(" | ") else null

        val baseTx = TransactionEntity(
            accountSource = accountSource,
            counterparty = counterparty,
            category = "General",
            amount = if (type == "DEBIT") -amount else amount,
            timestamp = date,
            method = determineMethod(lowerBody),
            type = type,
            remark = remark,
            enrichedSource = "SMS",
            balanceAfter = balance
        )

        return try {
            baseTx.copy(category = CategorizeTransactionUseCase().invoke(baseTx))
        } catch (e: Exception) {
            baseTx
        }
    }

    private fun getCleanSender(sender: String): String {
        val parts = sender.split("-")
        val main = parts.last().uppercase()
        if (main.length < 3 && parts.size > 1) {
            val prefix = parts.first().uppercase()
            if (prefix.length >= 3) return prefix
            return sender.replace("-", "").uppercase()
        }
        return main
    }

    private fun extractBestAmount(body: String, isDebit: Boolean): Double? {
        val amountRegex = Regex("""(?i)(?:rs\.?|₹|inr|amt|amount)\s*([\d,]+(?:\.\d{1,2})?)|\b(\d+[\.,]\d{2})\b""")
        val matches = amountRegex.findAll(body).toList()
        val candidates = mutableListOf<Pair<Double, Int>>()
        val verbs = if (isDebit) DEBIT_VERBS else CREDIT_VERBS

        for (match in matches) {
            val valStr = (match.groupValues[1].ifEmpty { match.groupValues[2] }).replace(",", "")
            val value = valStr.toDoubleOrNull() ?: continue
            
            if (valStr.length == 8 && !body.contains(Regex("""(?i)(rs\.?|₹|inr)\s*${valStr}"""))) continue
            if (value > 2000000.0 || value < 1.0) continue

            var score = if (match.groupValues[1].isNotEmpty()) 200 else 100
            
            if (verbs.any { body.contains(Regex("""(?i)$it\s*.*?\s*${Regex.escape(valStr)}""")) || 
                           body.contains(Regex("""(?i)${Regex.escape(valStr)}\s*.*?$it""")) }) {
                score += 500
            }
            
            if (body.contains(Regex("""(?i)(bal|balance|limit|total|avl|clr bal|outstanding)\s*(?:is|:)?\s*(?:rs\.?|₹)?\s*${Regex.escape(valStr)}"""))) {
                score -= 2000
            }
            
            candidates.add(value to score)
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun extractMerchant(body: String, cleanSender: String, isBankMsg: Boolean): String {
        val patterns = listOf(
            Regex("""(?i)(?:paid to|spent on|at|towards|transfer to|from|sent to|txn to|remit to)\s+([a-z0-9 ._-]{2,30})"""),
            Regex("""(?i)vpa\s+([a-z0-9.@_-]+)"""),
            Regex("""(?i)dear\s+([a-z0-9]+)\s+customer"""),
            Regex("""(?i)to\s+([a-z0-9 ._-]{2,30})\s+via"""),
            Regex("""(?i)from\s+([a-z0-9 ._-]{2,30})\s+via""")
        )

        for (pattern in patterns) {
            val match = pattern.find(body) ?: continue
            var name = match.groupValues[1].trim()
            
            val noise = listOf("your", "the", "a/c", "acct", "ref", "dated", "on", "rs", "inr", "is", "of", "to")
            name = name.split(" ").filter { it.lowercase() !in noise }.joinToString(" ")
            
            val cleanName = name.split(Regex("(?i)\\s-|\\s/|:|\\*|\\son\\s|\\svia|\\sref|\\sfor|\\sid|\\shas|\\son\\b")).first().trim()
            if (cleanName.length >= 3 && !cleanName.all { it.isDigit() }) return cleanName.uppercase()
        }

        if (isBankMsg) return "BANK TRANSACTION"
        if (cleanSender.length >= 3 && BANK_KEYWORDS.none { cleanSender.contains(it) }) return cleanSender
        
        return "UNKNOWN MERCHANT"
    }

    private fun extractBalance(body: String): Double? {
        val regex = Regex("""(?i)(?:bal|balance|clr bal|total bal|avl bal)\s*(?:is|:)?\s*(?:rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""")
        return regex.findAll(body).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
    
    private fun extractAccountEnding(body: String): String? {
        val regex = Regex("""(?i)(?:a/c|acct|ending|account|acc)\s*(?:no\.?|number)?\s*(?:[x*]*(\d{3,4}))""")
        return regex.find(body)?.groupValues?.get(1)
    }

    private fun determineMethod(body: String): String {
        return when {
            body.contains("upi") || body.contains("@") || body.contains("vpa") -> "UPI"
            body.contains("atm") || body.contains("wdl") -> "ATM"
            body.contains("card") || body.contains("pos") || body.contains("swipe") -> "Card"
            else -> "Bank"
        }
    }
}
