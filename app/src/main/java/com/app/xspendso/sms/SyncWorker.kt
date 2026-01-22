package com.app.xspendso.sms

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.app.xspendso.data.PrefsManager
import com.app.xspendso.domain.Resource
import com.app.xspendso.domain.usecase.SyncLedgerUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncLedgerUseCase: SyncLedgerUseCase,
    private val prefsManager: PrefsManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (val result = syncLedgerUseCase()) {
            is Resource.Success -> {
                Log.d("SyncWorker", "Sync successful: ${result.data.newTransactionsCount} new transactions")
                Result.success()
            }
            is Resource.Error -> {
                Log.e("SyncWorker", "Sync failed: ${result.error.message}")
                // Retry for network or database errors, but not for permission errors
                if (result.error is com.app.xspendso.domain.DomainError.SyncError.SmsReadPermissionDenied) {
                    Result.failure()
                } else {
                    Result.retry()
                }
            }
            is Resource.Loading -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "SmsSyncWorker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(3, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
