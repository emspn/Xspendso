package com.app.xspendso.sms

import com.app.xspendso.data.TransactionEntity

object SmsParser {
    private val DEBIT_VERBS = listOf("debited", "spent", "paid", "withdrawn", "dr a/c", "sent to", "purchase", "txn of", "payment of", "towards")
    private val CREDIT_VERBS = listOf("credited", "received", "added", "cr a/c", "remittance", "deposited", "refunded", "refund", "cashback")
    
    private val REJECT_WORDS = listOf(
        "otp", "verification", "login", "code is", "is your", "security code", 
        "bill generated", "get up to", "win", "offer", "apply", "congratulations",
        "limited period", "click", "recharge via", "cashback now", "claim", "upto", 
        "discount", "survey", "available", "valid until", "chance to", "rewards",
        "stay tuned", "subscribe", "limited offer", "exclusive", "gift", "voucher",
        "due date", "statement", "reminder", "overdue", "pay by", "last date",
        "insufficient funds", "failed", "declined", "denied", "reversed", "unsuccessful",
        "will be debited", "scheduled for", "requested for", "is linked", "registered for",
        "pre-approved", "approved for", "to apply", "eligible for", "thank you for using",
        "blocked", "activated", "deactivated", "service is now", "update on your a/c",
        "e-statement", "billed for", "info:", "gift voucher", "to verify", "code:",
        "data pack", "validity", "unlimited", "gb data", "mb data", "calling", "roaming",
        "talktime", "top-up", "plan expired", "expired", "bonus", "per day", "data left",
        "usage", "internet", "recharge now", "recharge successful", "data balance",
        "lenskart", "ajio", "myntra", "amazon", "flipkart", "order", "delivery", "track",
        "sale", "price drop", "off", "deals", "win", "spin", "luck", "points", "coins",
        "credited to your account", "wallet", "cashback will be", "credited within", 
        "book your", "appoint", "visit", "store", "nearby", "shop now", "buy 1", 
        "get 1", "flat", "extra", "off on", "discount", "promo", "code",
        "lottery", "prize", "kbc", "whatsapp", "lucky winner", "claim your", "click here",
        "update kyc", "pan card", "blocked soon", "verify now", "account suspended",
        "urgent", "action required", "bit.ly", "tinyurl", "shorturl", "link below",
        "unclaimed", "reward points", "expired today", "earn money", "work from home",
        "investment plan", "doubling", "free", "gift card", "iphone", "won a",
        "kyc pending", "ebill", "electricity cut", "bill overdue", "penalty",
        "dream11", "rummy", "my11circle", "fantasy", "team", "contest", "megacontest",
        "winnings", "bonus cash", "entry fee", "poker", "casino", "jackpot", "play now",
        "match starts", "lineups", "leaderboard", "referral bonus", "refer and earn",
        "mpl", "zupee", "winzo", "games", "tournament"
    )
    
    // Exhaustive Whitelist based on user provided list + common suffixes
    private val BANK_KEYWORDS = listOf(
        "SBI", "PNB", "BOB", "CNB", "UOB", "BOI", "CBI", "CBOI", "INB", "UCO", 
        "IOB", "PSB", "BOM", "ALB", "ANB", "CRB", "DNB", "VJB", "SYB", "OBC", 
        "HDFC", "ICICI", "AXIS", "KMB", "KOTAK", "IIB", "INDUS", "RBL", "YBL", 
        "YESB", "IDB", "IDBI", "IDFC", "FBL", "FEDERAL", "KTB", "KVB", "CUB", 
        "TMB", "SIB", "ACB", "APN", "BMB", "GSC", "MUC", "NTB", "NGB", "SRC", 
        "JSB", "HCB", "PAYTM", "GPAY", "IPPB", "DOPBNK", "POSTAL", "CENT", "CANARA"
    ).sortedByDescending { it.length }

    fun parse(body: String, sender: String, date: Long): TransactionEntity? {
        val lowerBody = body.lowercase().replace("\n", " ").trim()
        if (REJECT_WORDS.any { lowerBody.contains(it) }) return null
        
        val isBankContext = lowerBody.contains(Regex("""a/c|acct|ending|account|balance|total bal|avl bal|clr bal|acc\s*no|card\s*no|stmt"""))
        val cleanSender = getCleanSender(sender)
        
        // --- SECURE ACCOUNT SOURCE IDENTIFICATION ---
        var accountSource = ""
        
        // Priority 1: Match Sender ID part (Whole word match)
        val senderParts = cleanSender.split(Regex("[^A-Z]"))
        accountSource = BANK_KEYWORDS.find { kw -> senderParts.any { it == kw } } ?: ""
        
        // Priority 2: Special Case - Whitelist Match at the VERY END of the body
        if (accountSource.isEmpty()) {
            val endPattern = Regex("""[-\s]([A-Z]{2,10})\.?\s*$""", RegexOption.IGNORE_CASE)
            val endMatch = endPattern.find(body)?.groupValues?.get(1)?.uppercase()
            if (endMatch != null && BANK_KEYWORDS.contains(endMatch)) {
                accountSource = endMatch
            }
        }
        
        // Priority 3: Match Body (WHOLE WORD ONLY via \b to prevent NTB in CENTBANK)
        if (accountSource.isEmpty()) {
            for (keyword in BANK_KEYWORDS) {
                if (lowerBody.uppercase().contains(Regex("""\b$keyword\b"""))) {
                    accountSource = keyword
                    break
                }
            }
        }

        // Final Validation & Fallback
        val forbidden = listOf("SUCCESSFUL", "FAILED", "WITHDRAWAL", "TRANSACTION", "LIMIT", "PENDING", "COMPLETED", "BANK", "ACCOUNT", "MONEY")
        val isNumeric = accountSource.any { it.isDigit() } && accountSource.length > 5
        
        if (accountSource.isEmpty() || accountSource.uppercase() in forbidden || isNumeric || accountSource.length < 3) {
            accountSource = when {
                cleanSender.length >= 3 && cleanSender.none { it.isDigit() } -> cleanSender
                isBankContext -> "OTHER BANK"
                else -> return null // If no bank identified and not a bank msg, ignore.
            }
        }

        // Map internal names back to professional labels
        val finalAccountName = when(accountSource.uppercase()) {
            "CENT", "CBI" -> "CBOI"
            "HDF" -> "HDFC"
            "ICI" -> "ICICI"
            "AXB" -> "AXIS"
            "KMB" -> "KOTAK"
            "DOPBNK" -> "IPPB"
            else -> accountSource.uppercase()
        }

        val type = when {
            lowerBody.contains("refund") -> "CREDIT"
            DEBIT_VERBS.any { lowerBody.contains(it) } -> "DEBIT"
            CREDIT_VERBS.any { lowerBody.contains(it) } -> "CREDIT"
            else -> return null
        }

        val amount = extractBestAmount(lowerBody, type == "DEBIT") ?: return null
        val counterparty = extractMerchant(lowerBody, cleanSender, isBankContext)

        return TransactionEntity(
            accountSource = finalAccountName,
            counterparty = counterparty,
            category = "General",
            amount = if (type == "DEBIT") -amount else amount,
            timestamp = date,
            method = determineMethod(lowerBody),
            type = type,
            remark = extractRemark(lowerBody),
            enrichedSource = "SMS",
            balanceAfter = extractBalance(lowerBody)
        )
    }

    private fun getCleanSender(sender: String): String {
        val parts = sender.split("-")
        val main = parts.last().uppercase()
        if (main.all { it.isDigit() || it == '+' } || main.length < 3) {
            val first = if (parts.size > 1) parts.first().uppercase() else ""
            return if (first.length >= 3 && first.none { it.isDigit() }) first else ""
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
            
            if (body.contains(Regex("""(?i)(bal|balance|limit|total|avl|clr bal|outstanding|is now|recharge|pack|sale|off|discount|prize|won|contest|fee)\s*(?:is|:)?\s*(?:rs\.?|₹)?\s*${Regex.escape(valStr)}"""))) {
                score -= 2000
            }
            
            candidates.add(value to score)
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun extractMerchant(body: String, cleanSender: String, isBankMsg: Boolean): String {
        val patterns = listOf(
            Regex("""(?i)(?:paid to|spent on|at|towards|transfer to|from|sent to|txn to|remit to|transfer from|received from)\s+([a-z0-9 ._-]{3,30})"""),
            Regex("""(?i)vpa\s+([a-z0-9.@_-]+)"""),
            Regex("""(?i)to\s+([a-z0-9 ._-]{3,30})\s+via"""),
            Regex("""(?i)from\s+([a-z0-9 ._-]{3,30})\s+via"""),
            Regex("""(?i)(?:ref|ref no|info):\s*([a-z0-9 ._-]{3,30})""")
        )

        for (pattern in patterns) {
            val match = pattern.find(body) ?: continue
            var name = match.groupValues[1].trim()
            val noise = listOf("your", "the", "a/c", "acct", "ref", "dated", "on", "rs", "inr", "is", "of", "to", "for", "at", "by", "via", "info", "account", "transaction", "successful", "withdrawn", "credited", "debited")
            name = name.split(" ").filter { it.lowercase() !in noise }.joinToString(" ")
            val cleanName = name.split(Regex("(?i)\\s-|\\s/|:|\\*|\\son\\s|\\svia|\\sref|\\sfor|\\sid|\\shas|\\son\\b|\\swith\\b|\\sat\\b|\\sis\\b")).first().trim()
            
            if (cleanName.length >= 3 && 
                !cleanName.all { it.isDigit() } && 
                BANK_KEYWORDS.none { cleanName.uppercase().contains(it) } &&
                cleanName.lowercase() !in listOf("bank", "account", "balance", "money", "transfer", "successful", "withdrawn")) {
                return cleanName.uppercase()
            }
        }

        if (body.contains("atm") || body.contains("wdl") || body.contains("withdrawn")) return "CASH WITHDRAWAL"
        if (isBankMsg) return "BANK TRANSACTION"
        
        return "BANK TRANSACTION"
    }

    private fun extractBalance(body: String): Double? {
        val regex = Regex("""(?i)(?:bal|balance|clr bal|total bal|avl bal)\s*(?:is|:)?\s*(?:rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)""")
        return regex.findAll(body).lastOrNull()?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }
    
    private fun extractAccountEnding(body: String): String? {
        val regex = Regex("""(?i)(?:a/c|acct|ending|account|acc)\s*(?:no\.?|number)?\s*(?:[x*]*(\d{3,4}))""")
        return regex.find(body)?.groupValues?.get(1)
    }

    private fun extractRemark(body: String): String? {
        val accEnding = extractAccountEnding(body)
        val balance = extractBalance(body)
        val remarkParts = mutableListOf<String>()
        if (accEnding != null) remarkParts.add("A/c: $accEnding")
        if (balance != null) remarkParts.add("Bal: Rs. $balance")
        return if (remarkParts.isNotEmpty()) remarkParts.joinToString(" | ") else null
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
