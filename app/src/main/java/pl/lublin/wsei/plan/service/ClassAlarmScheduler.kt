package pl.lublin.wsei.plan.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.lublin.wsei.plan.domain.model.ClassEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules exact start alarms for cached WSEI classes so [LiveClassNotificationService]
 * launches automatically the moment a class begins.
 */
@Singleton
class ClassAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleUpcomingClassAlarms(events: List<ClassEvent>) {
        val now = System.currentTimeMillis()

        // If a class is already ongoing right now, start the Live Notification immediately
        events.firstOrNull { it.isOngoing(now) }?.let { ongoingEvent ->
            LiveClassNotificationService.startForClass(context, ongoingEvent.id)
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

@AndroidEntryPoint
class ClassStartAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_LIVE_CLASS = "pl.lublin.wsei.plan.ACTION_START_LIVE_CLASS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_START_LIVE_CLASS) {
            val eventId = intent.getStringExtra(LiveClassNotificationService.EXTRA_EVENT_ID)
                ?: return
            try {
                LiveClassNotificationService.startForClass(context, eventId)
            } catch (t: Throwable) {
                android.util.Log.w("ClassStartAlarmReceiver", "Failed to start service: ${t.message}")
            }
        }
    }
}
