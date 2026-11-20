package pl.lublin.wsei.plan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.lublin.wsei.plan.MainActivity
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.l10n.labelRes

/**
 * A chronological segment of the student's academic day (`Zajęcia` or `Przerwa`) for the
 * segmented Live Notification progress bar.
 */
sealed class DayTimelineSegment(
    val startMillis: Long,
    val endMillis: Long
) {
    val durationMinutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(1)

    /** Visual weight on the bar, so short 5–10 min breaks stay visible as separate segments. */
    val visualLengthUnits: Int
        get() = when (this) {
            is ClassSegment -> durationMinutes.coerceAtLeast(20)
            is BreakSegment -> durationMinutes.coerceIn(12, 45)
        }

    fun progressAt(nowMillis: Long): Float {
        val total = (endMillis - startMillis).coerceAtLeast(1L)
        val elapsed = (nowMillis - startMillis).coerceIn(0L, total)
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun isOngoing(nowMillis: Long): Boolean = nowMillis in startMillis until endMillis

    class ClassSegment(val event: ClassEvent) : DayTimelineSegment(
        startMillis = event.timeSlot.startEpochMillis,
        endMillis = event.timeSlot.endEpochMillis
    )

    class BreakSegment(startMillis: Long, endMillis: Long, val nextEvent: ClassEvent) :
        DayTimelineSegment(startMillis = startMillis, endMillis = endMillis)
}

/**
 * Builds the Live Notification of the current class day, in a Material 3 Expressive hierarchy that
 * uses only standard templates (custom RemoteViews would disqualify the Android 16 "Live Update"
 * promotion):
 *
 * - **Header** (`subText` + count-down chronometer): status — „Trwa teraz · do 12:55” or
 *   „Za 12 min · o 13:05” — and a live countdown to the end / start.
 * - **Title**: subject of the current (or next) class.
 * - **Text**: class type · room · lecturer (lecturer omitted when „Ukryj dane” is on, and always on
 *   the lock-screen public version).
 * - **Large icon**: subject badge on an accent (class) or neutral (break) rounded tile.
 * - **Progress**: Android 16+ `Notification.ProgressStyle` with the whole day as segments
 *   (classes / breaks, current class in the accent colour); older versions get the standard
 *   progress bar of the current class or break plus an expanded `BigTextStyle`.
 *
 * Colours come from `res/values(-night)/colors_notification.xml`, resolved against the **system**
 * night mode (the shade follows the system, not the in-app theme), so contrast is right in both.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val strings: LocalizedStrings
) {
    companion object {
        private const val LEGACY_CHANNEL_ID = "wsei_live_class_progress_channel"
        private const val LEGACY_CHANNEL_ID_V3 = "wsei_live_day_progress_lockscreen_v3"
        const val LIVE_CLASS_CHANNEL_ID = "taal_live_progress_style_v4"
        const val LIVE_CLASS_NOTIFICATION_ID = 4010

        // Same keys as Notification.EXTRA_REQUEST_PROMOTED_ONGOING / EXTRA_SHORT_CRITICAL_TEXT (API 36);
        // set as extras so they are present on every version (some OEM shells read them earlier).
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

        private const val SEPARATOR = " · "
        private const val PROGRESS_MAX = 1000
        private const val BADGE_SIZE_PX = 128
        private const val TRACKER_SIZE_PX = 64
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannelCreated()
    }

    fun ensureChannelCreated() {
        runCatching {
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID_V3)
        }
        val channel = NotificationChannel(
            LIVE_CLASS_CHANNEL_ID,
            strings.get(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = strings.get(R.string.notif_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        // Re-creating an existing channel only updates its name/description (user settings are kept).
        notificationManager.createNotificationChannel(channel)
    }

    /** Builds the chronological `Zajęcia – Przerwa – Zajęcia` segments of the day. */
    fun buildDaySegments(dayEvents: List<ClassEvent>): List<DayTimelineSegment> {
        val sorted = dayEvents.sortedBy { it.timeSlot.startEpochMillis }
        val segments = mutableListOf<DayTimelineSegment>()
        sorted.forEachIndexed { idx, event ->
            segments += DayTimelineSegment.ClassSegment(event)
            val next = sorted.getOrNull(idx + 1)
            if (next != null && next.timeSlot.startEpochMillis > event.timeSlot.endEpochMillis) {
                segments += DayTimelineSegment.BreakSegment(
                    startMillis = event.timeSlot.endEpochMillis,
                    endMillis = next.timeSlot.startEpochMillis,
                    nextEvent = next
                )
            }
        }
        return segments
    }

    /** Minimal notification for the immediate `startForeground()`, before the class data is loaded. */
    fun buildPlaceholderNotification(): Notification {
        val palette = Palette.resolve(context)
        return Notification.Builder(context, LIVE_CLASS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taal_notification)
            .setColor(palette.accent)
            .setContentTitle(strings.get(R.string.app_name))
            .setContentText(strings.get(R.string.notif_loading))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent(null))
            .build()
    }

    /**
     * @param activeClass class the service tracks (ongoing, or the next one during a break).
     * @param hideLecturer true when „Ukryj dane” is on — lecturer names are left out entirely.
     */
    fun buildLiveClassNotification(
        activeClass: ClassEvent,
        nextClassPreview: NextClassPreview?,
        dayEvents: List<ClassEvent> = listOf(activeClass),
        nowEpochMillis: Long = System.currentTimeMillis(),
        hideLecturer: Boolean = false
    ): Notification {
        val palette = Palette.resolve(context)
        val day = dayEvents.ifEmpty { listOf(activeClass) }.sortedBy { it.timeSlot.startEpochMillis }
        val state = LiveState.of(day, activeClass, nextClassPreview, nowEpochMillis)

        val publicVersion = buildNotification(state, day, palette, nowEpochMillis, showLecturer = false, publicVersion = null)
        return buildNotification(state, day, palette, nowEpochMillis, showLecturer = !hideLecturer, publicVersion = publicVersion)
    }

    fun notifyLiveClassUpdate(
        activeClass: ClassEvent,
        nextClassPreview: NextClassPreview?,
        dayEvents: List<ClassEvent>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        hideLecturer: Boolean = false
    ) {
        publishLiveNotification(
            buildLiveClassNotification(activeClass, nextClassPreview, dayEvents, nowEpochMillis, hideLecturer)
        )
    }

    fun publishLiveNotification(notification: Notification) {
        notificationManager.notify(LIVE_CLASS_NOTIFICATION_ID, notification)
    }

    fun cancelLiveNotification() {
        notificationManager.cancel(LIVE_CLASS_NOTIFICATION_ID)
    }

    fun cancelLiveClassNotification() = cancelLiveNotification()

    // ---------------------------------------------------------------------------------------------

    /** What the notification shows at a given moment. */
    private class LiveState(
        /** Class shown in the title: the ongoing one, or the next one during a break / before the first. */
        val focused: ClassEvent,
        val isOngoing: Boolean,
        /** End of the previous class when in a break between classes, null otherwise. */
        val breakStartMillis: Long?,
        /** Class after [focused] (only while [isOngoing]). */
        val next: ClassEvent?
    ) {
        val targetMillis: Long
            get() = if (isOngoing) focused.timeSlot.endEpochMillis else focused.timeSlot.startEpochMillis

        fun minutesToTarget(now: Long): Int =
            ceil((targetMillis - now).coerceAtLeast(0L) / 60_000.0).toInt()

        /** Progress of the current class or break, null before the first class of the day. */
        fun segmentProgress(now: Long): Float? = when {
            isOngoing -> focused.progressFraction(now)
            breakStartMillis != null -> {
                val total = (focused.timeSlot.startEpochMillis - breakStartMillis).coerceAtLeast(1L)
                ((now - breakStartMillis).toFloat() / total).coerceIn(0f, 1f)
            }
            else -> null
        }

        companion object {
            fun of(day: List<ClassEvent>, active: ClassEvent, preview: NextClassPreview?, now: Long): LiveState {
                val focused = day.firstOrNull { it.isOngoing(now) }
                    ?: day.firstOrNull { it.timeSlot.startEpochMillis > now }
                    ?: active
                val isOngoing = focused.isOngoing(now)
                val breakStart = if (isOngoing) {
                    null
                } else {
                    day.filter { it.timeSlot.endEpochMillis <= now && it.timeSlot.endEpochMillis <= focused.timeSlot.startEpochMillis }
                        .maxOfOrNull { it.timeSlot.endEpochMillis }
                }
                val next = if (!isOngoing) {
                    null
                } else {
                    preview?.nextEvent?.takeIf { it.id != focused.id && it.timeSlot.startEpochMillis >= focused.timeSlot.endEpochMillis }
                        ?: day.firstOrNull {
                            it.id != focused.id && it.timeSlot.startEpochMillis >= focused.timeSlot.endEpochMillis
                        }
                }
                return LiveState(focused, isOngoing, breakStart, next)
            }
        }
    }

    private fun buildNotification(
        state: LiveState,
        day: List<ClassEvent>,
        palette: Palette,
        now: Long,
        showLecturer: Boolean,
        publicVersion: Notification?
    ): Notification {
        val event = state.focused
        val minutes = state.minutesToTarget(now)
        val status = when {
            state.isOngoing -> strings.get(R.string.notif_status_ongoing, event.timeSlot.endFormatted)
            minutes <= 0 -> strings.get(R.string.notif_status_starting, event.timeSlot.startFormatted)
            else -> strings.get(R.string.notif_status_upcoming, formatDuration(minutes), event.timeSlot.startFormatted)
        }
        val lecturer = event.lecturer.fullDisplayName.takeIf { showLecturer && it.isNotBlank() }
        val details = listOfNotNull(strings.get(event.classType.labelRes), roomLabel(event), lecturer).joinToString(SEPARATOR)

        val expandedLines = buildList {
            add(details)
            when {
                state.isOngoing -> add(strings.get(R.string.notif_remaining, formatDuration(minutes)))
                state.breakStartMillis != null -> add(strings.get(R.string.notif_break_until, event.timeSlot.startFormatted))
            }
            state.next?.let { add(strings.get(R.string.notif_next, it.subject.name, it.timeSlot.startFormatted)) }
        }

        val isBreak = !state.isOngoing
        val badge = WseiIcons.renderSubjectBadgeBitmap(
            subjectName = event.subject.name,
            classType = event.classType,
            sizePx = BADGE_SIZE_PX,
            containerColorArgb = if (isBreak) palette.breakSegment else palette.accent,
            iconColorArgb = if (isBreak) 0xFFFFFFFF.toInt() else palette.onAccent
        )

        val builder = Notification.Builder(context, LIVE_CLASS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taal_notification)
            .setColor(palette.accent)
            .setContentTitle(event.subject.name)
            .setContentText(details)
            .setSubText(status)
            .setLargeIcon(Icon.createWithBitmap(badge))
            .setShowWhen(true)
            .setWhen(state.targetMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent(event.id))
            .addExtras(
                Bundle().apply {
                    putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                    putString(EXTRA_SHORT_CRITICAL_TEXT, formatDuration(minutes.coerceAtLeast(1)))
                }
            )
        publicVersion?.let(builder::setPublicVersion)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setStyle(dayProgressStyle(day, now, palette))
        } else {
            builder.setStyle(Notification.BigTextStyle().bigText(expandedLines.joinToString("\n")))
            state.segmentProgress(now)?.let { fraction ->
                builder.setProgress(PROGRESS_MAX, (fraction * PROGRESS_MAX).toInt(), false)
            }
        }
        return builder.build()
    }

    /** Android 16 segmented bar of the whole day: classes, breaks (neutral) and the current class (accent). */
    @RequiresApi(36)
    private fun dayProgressStyle(day: List<ClassEvent>, now: Long, palette: Palette): Notification.ProgressStyle {
        val segments = buildDaySegments(day)
        val platformSegments = segments.map { seg ->
            Notification.ProgressStyle.Segment(seg.visualLengthUnits).apply {
                setColor(
                    when {
                        seg is DayTimelineSegment.BreakSegment -> palette.breakSegment
                        seg.isOngoing(now) -> palette.accent
                        else -> palette.classSegment
                    }
                )
            }
        }
        return Notification.ProgressStyle().apply {
            // Part of the day already passed is drawn solid, the rest faded.
            setStyledByProgress(true)
            setProgressSegments(platformSegments)
            setProgress(visualProgressUnits(segments, now))
            setProgressTrackerIcon(Icon.createWithBitmap(renderTrackerThumb(palette)))
        }
    }

    private fun visualProgressUnits(segments: List<DayTimelineSegment>, now: Long): Int {
        var accumulated = 0
        for (seg in segments) {
            when {
                now >= seg.endMillis -> accumulated += seg.visualLengthUnits
                now > seg.startMillis -> return accumulated + (seg.visualLengthUnits * seg.progressAt(now)).toInt()
                else -> return accumulated
            }
        }
        return accumulated
    }

    /** Round tracker thumb: accent dot with a ring in the shade's surface colour. */
    private fun renderTrackerThumb(palette: Palette): Bitmap {
        val bmp = Bitmap.createBitmap(TRACKER_SIZE_PX, TRACKER_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val center = TRACKER_SIZE_PX / 2f
        paint.color = palette.trackerRing
        canvas.drawCircle(center, center, TRACKER_SIZE_PX * 0.48f, paint)
        paint.color = palette.accent
        canvas.drawCircle(center, center, TRACKER_SIZE_PX * 0.34f, paint)
        return bmp
    }

    private fun roomLabel(event: ClassEvent): String = when {
        event.deliveryMode == DeliveryMode.ELEARNING -> strings.get(R.string.notif_room_elearning)
        event.deliveryMode == DeliveryMode.REMOTE || event.room.isRemoteTeams -> strings.get(R.string.notif_room_streaming)
        event.room.number.isBlank() -> strings.get(R.string.notif_room_tbd)
        else -> strings.get(R.string.notif_room, event.room.number)
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> strings.get(R.string.notif_duration_min, minutes)
            rest == 0 -> strings.get(R.string.notif_duration_hours, hours)
            else -> strings.get(R.string.notif_duration_hours_min, hours, rest)
        }
    }

    private fun contentIntent(eventId: String?): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "schedule")
            if (eventId != null) putExtra("focused_event_id", eventId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** Notification colours resolved for the system (shade) light/dark mode. */
    private class Palette(
        val accent: Int,
        val onAccent: Int,
        val classSegment: Int,
        val breakSegment: Int,
        val trackerRing: Int
    ) {
        companion object {
            fun resolve(context: Context): Palette {
                val systemNight = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                val config = Configuration(context.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or systemNight
                }
                val themed = context.createConfigurationContext(config)
                fun color(@ColorRes id: Int) = themed.getColor(id)
                return Palette(
                    accent = color(R.color.notif_accent),
                    onAccent = color(R.color.notif_on_accent),
                    classSegment = color(R.color.notif_segment_class),
                    breakSegment = color(R.color.notif_segment_break),
                    trackerRing = color(R.color.notif_tracker_ring)
                )
            }
        }
    }
}
