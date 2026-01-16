package com.app.xspendso.sms

import android.content.Context
import androidx.work.*
import com.app.xspendso.data.AppDatabase
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.data.TransactionRepositoryImpl
import com.app.xspendso.domain.usecase.DetectRecurringTransactionsUseCase
import com.app.xspendso.domain.usecase.SyncLedgerUseCase
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepositoryImpl(
            database.transactionDao(),
            database.correctionDao(),
            database.budgetDao(),
            database.goalDao(),
            database.ruleDao()
        )
        val prefsManager = PrefsManager(applicationContext)
        val smsReader = SmsReader(applicationContext)
        val detectRecurringTransactionsUseCase = DetectRecurringTransactionsUseCase()
        val notificationHelper = NotificationHelper(applicationContext)
        
        val syncLedgerUseCase = SyncLedgerUseCase(
            smsReader,
            repository,
            detectRecurringTransactionsUseCase,
            prefsManager
        )

        return try {
            val syncResult = syncLedgerUseCase()
            if (syncResult.newTransactionsCount > 0) {
                notificationHelper.showSyncNotification(
                    syncResult.newTransactionsCount,
                    syncResult.totalSpent
                )
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "XpendsoSyncWorker"

        fun schedulePeriodicSync(context: Context) {
            val prefsManager = PrefsManager(context)
            val frequency = prefsManager.syncFrequencyHours.toLong()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(frequency, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }
        
        fun runImmediateSync(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
                
            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
}
