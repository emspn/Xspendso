package com.app.xspendso.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.app.xspendso.data.TransactionEntity

class SmsReader(private val context: Context) {
    fun readTransactions(): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                val address = it.getString(addressIndex)
                val date = it.getLong(dateIndex)

                // Stub for parsing logic
                SmsParser.parse(body, address, date)?.let { transaction ->
                    transactions.add(transaction)
                }
            }
        }
        return transactions
    }
}