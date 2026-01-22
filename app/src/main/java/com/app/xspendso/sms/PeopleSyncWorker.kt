package com.app.xspendso.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.app.xspendso.domain.PeopleLedgerRepository
import com.app.xspendso.domain.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class PeopleSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PeopleLedgerRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return when (repository.syncWithCloud()) {
            is Resource.Success -> Result.success()
            is Resource.Error -> Result.retry()
            is Resource.Loading -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "PeopleSyncWorker"

        fun scheduleSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<PeopleSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
        
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<PeopleSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME + "_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
