package pl.lublin.wsei.plan.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pl.lublin.wsei.plan.data.remote.ScheduleNormalizer
import pl.lublin.wsei.plan.data.remote.api.AprSessionExpiredException
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Background sync: refreshes the timetable window (−14 … +60 days) and all academic modules into
 * Room, so the app opens with fresh data offline. The repository picks API → HTML fallback.
 */
@HiltWorker
class ScheduleSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ScheduleRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val session = repository.currentSession.value
            ?: repository.restoreEncryptedSessionIfAvailable()
            ?: return Result.success() // nobody logged in / "remember me" disabled
        if (session.isDebugAccount) return Result.success()

        val today = java.time.LocalDate.now(ScheduleNormalizer.WARSAW_ZONE)
            .let { LocalDate(it.year, it.monthValue, it.dayOfMonth) }
        val scheduleResult = repository.syncSchedule(
            startDate = today.minus(DatePeriod(days = 14)),
            endDate = today.plus(DatePeriod(days = 60)),
            scheduleType = ScheduleFilterType.MY_PLAN,
            forceRefresh = true
        )
        val modulesOk = repository.refreshAcademicSnapshots()

        val error = scheduleResult.exceptionOrNull()
        return when {
            error == null && modulesOk -> Result.success()
            error is AprSessionExpiredException -> Result.success() // needs user action; don't hammer the server
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val PERIODIC_WORK_NAME = "wd_schedule_periodic_sync"
        const val ONE_TIME_WORK_NAME = "wd_schedule_sync_now"
        private const val MAX_ATTEMPTS = 3
    }
}

@Singleton
class ScheduleSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<ScheduleSyncWorker>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ScheduleSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<ScheduleSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ScheduleSyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll() {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(ScheduleSyncWorker.PERIODIC_WORK_NAME)
            cancelUniqueWork(ScheduleSyncWorker.ONE_TIME_WORK_NAME)
        }
    }
}
