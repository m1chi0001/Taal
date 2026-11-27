package pl.lublin.wsei.plan

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.lublin.wsei.plan.data.update.UpdateCheckWorker
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import javax.inject.Inject

@HiltAndroidApp
class TaalApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var updateRepository: AppUpdateRepository

    /** Process-wide scope for fire-and-forget startup work. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** On-demand WorkManager init with Hilt-injected workers (default initializer removed in the manifest). */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate() // Hilt injects the fields here.
        try {
            UpdateCheckWorker.schedule(this)
        } catch (e: Exception) {
            // Never block app start on WorkManager (e.g. a corrupted WorkManager database).
            Log.w(TAG, "Could not schedule the update check", e)
        }
        // Launch update check; the UI may call refresh(false) too — the repository serializes and debounces calls.
        appScope.launch { updateRepository.refresh(force = false) }
    }

    private companion object {
        const val TAG = "TaalApplication"
    }
}
