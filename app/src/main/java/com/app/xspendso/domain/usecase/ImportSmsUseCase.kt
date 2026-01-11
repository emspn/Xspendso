package com.app.xspendso.domain.usecase

import com.app.xspendso.domain.TransactionRepository
import com.app.xspendso.sms.SmsReader

class ImportSmsUseCase(
    private val smsReader: SmsReader,
    private val repository: TransactionRepository
) {
    suspend operator fun invoke() {
        val transactions = smsReader.readTransactions()
        repository.insertTransactions(transactions)
    }
}