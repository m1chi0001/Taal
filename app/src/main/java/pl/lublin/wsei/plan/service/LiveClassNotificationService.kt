package pl.lublin.wsei.plan.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * ForegroundService powering the Real-Time "Live Class & Day Progress Notification".
 *
 * Tracks the entire academic day (`Zajęcia -> Przerwa -> Zajęcia -> Przerwa -> Zajęcia`),
 * updating the full-width segmented progress bar and lockscreen notification every minute.
 */
@AndroidEntryPoint
class LiveClassNotificationService : Service() {

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    companion object {
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
                android.util.Log.w("LiveClassService", "Failed to startForegroundService, OS background restriction: ${t.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, LiveClassNotificationService::class.java).apply {
                    action = ACTION_STOP_TRACKING
                }
                context.startService(intent)
            } catch (t: Throwable) {
                android.util.Log.w("LiveClassService", "Failed to stop service: ${t.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                stopTrackingAndSelf()
                return START_NOT_STICKY
            }
            ACTION_START_TRACKING, null -> {
                // Wymóg systemu: startForeground() musi nastąpić od razu po startForegroundService(),
                // inaczej aplikacja kończy się wyjątkiem ForegroundServiceDidNotStartInTimeException.
                promoteToForegroundImmediately()
                val eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
                if (eventId.isNullOrBlank()) {
                    stopTrackingAndSelf()
                    return START_NOT_STICKY
                }
                startLiveProgressLoop(eventId)
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun promoteToForegroundImmediately() {
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.LIVE_CLASS_NOTIFICATION_ID,
                notificationHelper.buildPlaceholderNotification(),
                type
            )
        } catch (t: Throwable) {
            android.util.Log.w("LiveClassService", "Immediate startForeground failed: ${t.message}")
        }
    }

    private fun startLiveProgressLoop(eventId: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val initialEvent = scheduleRepository.getEventById(eventId)
            if (initialEvent == null) {
                stopTrackingAndSelf()
                return@launch
            }

            val dayEvents = scheduleRepository.getEventsOnDay(initialEvent.dateIso)
                .sortedBy { it.timeSlot.startEpochMillis }
                .ifEmpty { listOf(initialEvent) }

            val dayEndEpochMillis = dayEvents.maxOf { it.timeSlot.endEpochMillis }

            // 1. Immediately promote to ForegroundService with initial notification
            val initialNow = System.currentTimeMillis()
            val initialNext = dayEvents.firstOrNull { it.timeSlot.startEpochMillis >= initialEvent.timeSlot.endEpochMillis && it.id != initialEvent.id }
            val initialPreview = initialNext?.let { next ->
                val brk = ((next.timeSlot.startEpochMillis - initialEvent.timeSlot.endEpochMillis) / 60_000L).toInt().coerceAtLeast(0)
                NextClassPreview(nextEvent = next, breakDurationMinutes = brk)
            }

            val initialNotification = notificationHelper.buildLiveClassNotification(
                activeClass = initialEvent,
                nextClassPreview = initialPreview,
                dayEvents = dayEvents,
                nowEpochMillis = initialNow
            )

            val fgServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

            ServiceCompat.startForeground(
                this@LiveClassNotificationService,
                NotificationHelper.LIVE_CLASS_NOTIFICATION_ID,
                initialNotification,
                fgServiceType
            )

            // 2. Minute-aligned live progress loop across the entire academic day (Zajęcia - Przerwa - Zajęcia)
            while (isActive) {
                val now = System.currentTimeMillis()
                if (now >= dayEndEpochMillis) {
                    stopTrackingAndSelf()
                    break
                }

                val currentActive = dayEvents.firstOrNull { it.isOngoing(now) }
                    ?: dayEvents.firstOrNull { it.timeSlot.startEpochMillis > now }
                    ?: initialEvent

                val nextEvent = dayEvents.firstOrNull {
                    it.timeSlot.startEpochMillis >= currentActive.timeSlot.endEpochMillis && it.id != currentActive.id
                }
                val nextPreview = nextEvent?.let { next ->
                    val breakMinutes = ((next.timeSlot.startEpochMillis - currentActive.timeSlot.endEpochMillis) / 60_000L)
                        .toInt()
                        .coerceAtLeast(0)
                    NextClassPreview(
                        nextEvent = next,
                        breakDurationMinutes = breakMinutes
                    )
                }

                notificationHelper.notifyLiveClassUpdate(
                    activeClass = currentActive,
                    nextClassPreview = nextPreview,
                    dayEvents = dayEvents,
                    nowEpochMillis = now
                )

                val millisIntoCurrentMinute = now % 60_000L
                val delayToNextMinute = (60_000L - millisIntoCurrentMinute).coerceAtLeast(1_000L)
                val remainingToDayEnd = (dayEndEpochMillis - now).coerceAtLeast(1_000L)
                delay(minOf(delayToNextMinute, remainingToDayEnd))
            }
        }
    }

    private fun stopTrackingAndSelf() {
        tickerJob?.cancel()
        notificationHelper.cancelLiveClassNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
