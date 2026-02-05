package com.app.xspendso.sms

import android.content.Context
import android.util.Log
import com.app.xspendso.data.TransactionEntity
import com.google.mlkit.nl.entityextraction.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsAiParser @Inject constructor(private val context: Context) {

    private val entityExtractor: EntityExtractor by lazy {
        EntityExtraction.getClient(
            EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH)
                .build()
        )
    }

    private var isModelDownloaded = false

    suspend fun parseWithFallback(body: String, sender: String, date: Long): TransactionEntity? {
        // --- Tier 1: Regex (Fast Path) ---
        val regexResult = SmsParser.parse(body, sender, date)
        
        // If Regex worked perfectly, return it immediately
        if (regexResult != null && regexResult.amount != 0.0 && regexResult.accountSource != "UNKNOWN") {
            return regexResult
        }

        // --- Tier 2: ML Kit Fallback (Smart Path) ---
        return try {
            ensureModelDownloaded()
            
            val params = EntityExtractionParams.Builder(body).build()
            val annotations = entityExtractor.annotate(params).await()
            
            var extractedAmount: Double? = null
            
            // Extract Currency/Money entities
            for (annotation in annotations) {
                for (entity in annotation.entities) {
                    if (entity is MoneyEntity) {
                        // ML Kit is great at finding numbers regardless of symbols
                        extractedAmount = entity.integerPart.toDouble() + (entity.fractionalPart.toDouble() / 100.0)
                        break
                    }
                }
                if (extractedAmount != null) break
            }

            // If Regex found the bank/source but missed the amount, patch it
            if (regexResult != null && extractedAmount != null) {
                Log.d("SmsAiParser", "ML Kit patched amount: $extractedAmount")
                return regexResult.copy(
                    amount = if (regexResult.type == "DEBIT") -extractedAmount else extractedAmount
                )
            }

            // If Regex failed completely, but ML Kit found an amount, 
            // we try to determine Type (Debit/Credit) using simple keywords
            if (extractedAmount != null) {
                val lowerBody = body.lowercase()
                val isDebit = lowerBody.contains(Regex("debited|spent|paid|withdrawn|dr a/c|sent to|payment"))
                val isCredit = lowerBody.contains(Regex("credited|received|added|cr a/c|refund"))
                
                if (isDebit || isCredit) {
                    Log.d("SmsAiParser", "ML Kit created new transaction from unknown format")
                    return TransactionEntity(
                        accountSource = "AUTO-DETECT",
                        counterparty = "UNKNOWN MERCHANT",
                        category = "General",
                        amount = if (isDebit) -extractedAmount else extractedAmount,
                        timestamp = date,
                        method = if (lowerBody.contains("upi")) "UPI" else "Bank",
                        type = if (isDebit) "DEBIT" else "CREDIT",
                        enrichedSource = "AI-MLKIT"
                    )
                }
            }

            regexResult // Return original regex result (even if partial) if AI couldn't improve it
        } catch (e: Exception) {
            Log.e("SmsAiParser", "ML Kit Error: ${e.message}")
            regexResult
        }
    }

    private suspend fun ensureModelDownloaded() {
        if (!isModelDownloaded) {
            entityExtractor.downloadModelIfNeeded().await()
            isModelDownloaded = true
        }
    }
}
