package pl.lublin.wsei.plan.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pl.lublin.wsei.plan.data.local.AppPreferencesStore
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact start alarms for cached WSEI classes so [LiveClassNotificationService]
 * launches automatically the moment a class begins.
 */
@Singleton
class ClassAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesStore: AppPreferencesStore
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Live Notification is wanted: enabled in settings and notifications are allowed. */
    fun isLiveNotificationAllowed(): Boolean =
        preferencesStore.current.isLiveNotificationEnabled &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun scheduleUpcomingClassAlarms(events: List<ClassEvent>) {
        val now = System.currentTimeMillis()

        // If a class is already ongoing right now, start the Live Notification immediately
        // (only when the user has it enabled — alarms below re-check this when they fire).
        if (isLiveNotificationAllowed()) {
            events.firstOrNull { it.isOngoing(now) }?.let { ongoingEvent ->
                LiveClassNotificationService.startForClass(context, ongoingEvent.id)
            }
        }

        // Schedule future classes starting within the next 7 days
        val horizonMillis = now + 7 * 24 * 60 * 60_000L
        events
            .filter { it.timeSlot.startEpochMillis in (now + 1)..horizonMillis }
            .forEach { upcoming ->
                scheduleSingleAlarm(upcoming)
            }
    }

    private fun scheduleSingleAlarm(event: ClassEvent) {
        val intent = Intent(context, ClassStartAlarmReceiver::class.java).apply {
            action = ClassStartAlarmReceiver.ACTION_START_LIVE_CLASS
            putExtra(LiveClassNotificationService.EXTRA_EVENT_ID, event.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                event.timeSlot.startEpochMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                event.timeSlot.startEpochMillis,
                pendingIntent
            )
        }
    }
}

/**
 * Starts the Live Notification when a class begins, and re-schedules the start alarms after a
 * reboot (alarms do not survive one; previously BOOT_COMPLETED was declared but ignored).
 */
@AndroidEntryPoint
class ClassStartAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: ClassAlarmScheduler

    @Inject
    lateinit var scheduleRepository: ScheduleRepository

    companion object {
        const val ACTION_START_LIVE_CLASS = "pl.lublin.wsei.plan.ACTION_START_LIVE_CLASS"
        private const val TAG = "ClassStartAlarmReceiver"
        private const val RESCHEDULE_DAYS = 7L
        private val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START_LIVE_CLASS -> {
                val eventId = intent.getStringExtra(LiveClassNotificationService.EXTRA_EVENT_ID) ?: return
                if (!alarmScheduler.isLiveNotificationAllowed()) return
                try {
                    LiveClassNotificationService.startForClass(context, eventId)
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to start service: ${t.message}")
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAfterBoot()
        }
    }

    private fun rescheduleAfterBoot() {
        val pending = goAsync()
        receiverScope.launch {
            try {
                val today = LocalDate.now(WARSAW)
                val events = (0..RESCHEDULE_DAYS).flatMap { offset ->
                    scheduleRepository.getEventsOnDay(today.plusDays(offset).toString())
                }
                alarmScheduler.scheduleUpcomingClassAlarms(events)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to reschedule class alarms after boot: ${t.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
