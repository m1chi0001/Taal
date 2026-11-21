package pl.lublin.wsei.plan.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pl.lublin.wsei.plan.data.local.AppPreferencesStore
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Foreground service powering the Live Notification of the current class day
 * (`Zajęcia → Przerwa → Zajęcia`). It refreshes the notification every minute and exactly at
 * every class start/end, re-reads the day from Room (so schedule refreshes are picked up), reacts
 * immediately to „Ukryj dane” and stops itself after the last class or when the feature is disabled.
 */
@AndroidEntryPoint
class LiveClassNotificationService : Service() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var preferencesStore: AppPreferencesStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null
    private var settingsJob: Job? = null
    private var trackedEventId: String? = null
    private var lastNotification: Notification? = null

    /** Wakes the ticker for an immediate re-render (settings changed). */
    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)

    companion object {
        private const val TAG = "LiveClassService"
        const val ACTION_START_TRACKING = "pl.lublin.wsei.plan.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "pl.lublin.wsei.plan.action.STOP_TRACKING"
        const val EXTRA_EVENT_ID = "extra_event_id"

        fun startForClass(context: Context, eventId: String) {
            try {
                val intent = Intent(context, LiveClassNotificationService::class.java).apply {
                    action = ACTION_START_TRACKING
                    putExtra(EXTRA_EVENT_ID, eventId)
                }
                context.startForegroundService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to startForegroundService, OS background restriction: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, LiveClassNotificationService::class.java))
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop service: ${t.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsJob = serviceScope.launch {
            preferencesStore.settings
                .map { it.isLiveNotificationEnabled to it.hideSensitiveData }
                .distinctUntilChanged()
                .drop(1)
                .collect { (enabled, _) ->
                    if (!enabled) stopTrackingAndSelf() else refreshSignal.trySend(Unit)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Every startForegroundService() must be followed by startForeground() right away, otherwise
        // the app crashes with ForegroundServiceDidNotStartInTimeException. Re-use the current
        // notification when one is already shown, so repeated starts do not flash a placeholder.
        promoteToForeground(lastNotification ?: notificationHelper.buildPlaceholderNotification())

        if (intent?.action == ACTION_STOP_TRACKING || !preferencesStore.current.isLiveNotificationEnabled) {
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }
        val eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        if (eventId.isNullOrBlank()) {
            if (tickerJob?.isActive != true) stopTrackingAndSelf()
            return START_NOT_STICKY
        }
        if (eventId == trackedEventId && tickerJob?.isActive == true) {
            refreshSignal.trySend(Unit)
        } else {
            startLiveProgressLoop(eventId)
        }
        return START_REDELIVER_INTENT
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NotificationHelper.LIVE_CLASS_NOTIFICATION_ID, notification, type)
        } catch (t: Throwable) {
            Log.w(TAG, "startForeground failed: ${t.message}")
        }
    }

    private fun startLiveProgressLoop(eventId: String) {
        tickerJob?.cancel()
        trackedEventId = eventId
        tickerJob = serviceScope.launch {
            val initialEvent = scheduleRepository.getEventById(eventId)
            if (initialEvent == null) {
                stopTrackingAndSelf()
                return@launch
            }
            var dayEvents = listOf(initialEvent)
            var firstFrame = true

            while (isActive) {
                // Re-read the day every tick: a schedule refresh may have moved or cancelled classes.
                dayEvents = scheduleRepository.getEventsOnDay(initialEvent.dateIso)
                    .sortedBy { it.timeSlot.startEpochMillis }
                    .ifEmpty { dayEvents }

                val now = System.currentTimeMillis()
                val dayEnd = dayEvents.maxOf { it.timeSlot.endEpochMillis }
                if (now >= dayEnd) {
                    stopTrackingAndSelf()
                    break
                }

                val current = dayEvents.firstOrNull { it.isOngoing(now) }
                    ?: dayEvents.firstOrNull { it.timeSlot.startEpochMillis > now }
                    ?: initialEvent
                val notification = notificationHelper.buildLiveClassNotification(
                    activeClass = current,
                    nextClassPreview = nextPreview(current, dayEvents),
                    dayEvents = dayEvents,
                    nowEpochMillis = now,
                    hideLecturer = preferencesStore.current.hideSensitiveData
                )
                lastNotification = notification
                if (firstFrame) {
                    promoteToForeground(notification)
                    firstFrame = false
                } else {
                    notificationHelper.publishLiveNotification(notification)
                }

                withTimeoutOrNull(delayUntilNextChange(now, dayEvents)) { refreshSignal.receive() }
            }
        }
    }

    private fun nextPreview(current: ClassEvent, dayEvents: List<ClassEvent>): NextClassPreview? {
        val next = dayEvents.firstOrNull {
            it.timeSlot.startEpochMillis >= current.timeSlot.endEpochMillis && it.id != current.id
        } ?: return null
        val breakMinutes = ((next.timeSlot.startEpochMillis - current.timeSlot.endEpochMillis) / 60_000L)
            .toInt()
            .coerceAtLeast(0)
        return NextClassPreview(nextEvent = next, breakDurationMinutes = breakMinutes)
    }

    /** Until the next full minute, or the next class start/end if that comes sooner. */
    private fun delayUntilNextChange(now: Long, dayEvents: List<ClassEvent>): Long {
        val nextMinute = 60_000L - now % 60_000L
        val nextBoundary = dayEvents
            .flatMap { listOf(it.timeSlot.startEpochMillis, it.timeSlot.endEpochMillis) }
            .filter { it > now }
            .minOfOrNull { it - now }
            ?: nextMinute
        return minOf(nextMinute, nextBoundary).coerceAtLeast(1_000L)
    }

    private fun stopTrackingAndSelf() {
        tickerJob?.cancel()
        tickerJob = null
        trackedEventId = null
        lastNotification = null
        notificationHelper.cancelLiveClassNotification()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        settingsJob?.cancel()
        notificationHelper.cancelLiveClassNotification()
        serviceScope.cancel()
        super.onDestroy()
    }
}
