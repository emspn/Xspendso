package com.app.xspendso.sms

import android.content.Context
import android.provider.Telephony
import com.app.xspendso.data.TransactionEntity

class SmsReader(private val context: Context) {
    fun readTransactions(since: Long = 0L): List<TransactionEntity> {
        val transactions = mutableListOf<TransactionEntity>()
        val selection = if (since > 0) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs = if (since > 0) arrayOf(since.toString()) else null

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            selection,
            selectionArgs,
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val body = it.getString(bodyIndex) ?: continue
                val address = it.getString(addressIndex) ?: continue
                val date = it.getLong(dateIndex)

                SmsParser.parse(body, address, date)?.let { transaction ->
                    transactions.add(transaction)
                }
            }
        }
        return transactions
    }
}
