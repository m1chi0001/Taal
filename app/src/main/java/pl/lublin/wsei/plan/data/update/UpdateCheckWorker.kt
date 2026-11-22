package pl.lublin.wsei.plan.data.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import java.util.concurrent.TimeUnit

/**
 * Periodic background check of GitHub Releases. Only refreshes [AppUpdateRepository] (throttled,
 * cached in DataStore); the UI shows the non-intrusive prompt from that state. No notification.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: AppUpdateRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Errors end up in the status (cached data is kept); the next period retries, so never
        // ask WorkManager to retry and hit GitHub's rate limit.
        repository.refresh(force = false)
        return Result.success()
    }

    companion object {
        const val PERIODIC_WORK_NAME = "taal_update_check_periodic"
        private const val INTERVAL_HOURS = 12L

        /** Enqueues the 12-hour check (needs network); an already scheduled one is kept. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
