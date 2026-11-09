package pl.lublin.wsei.plan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.lublin.wsei.plan.MainActivity
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a chronological segment of the student's academic day
 * (`Zajęcia` or `Przerwa`) for the segmented Live Notification progress bar.
 */
sealed class DayTimelineSegment(
    val startMillis: Long,
    val endMillis: Long,
    val shortLabel: String
) {
    val durationMinutes: Int
        get() = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(1)

    /**
     * Visual weight on the segmented progress bar so short 5–10 min breaks remain clearly visible
     * as distinct grey pill segments (matching Koleo's transfer segments).
     */
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

    class ClassSegment(
        val indexInDay: Int,
        val event: ClassEvent
    ) : DayTimelineSegment(
        startMillis = event.timeSlot.startEpochMillis,
        endMillis = event.timeSlot.endEpochMillis,
        shortLabel = "Zaj. $indexInDay"
    )

    class BreakSegment(
        startMillis: Long,
        endMillis: Long,
        val nextEvent: ClassEvent
    ) : DayTimelineSegment(
        startMillis = startMillis,
        endMillis = endMillis,
        shortLabel = "Przerwa"
    )
}

/**
 * Builds Android 16 Progress-Centric Live Notifications (`Notification.ProgressStyle`)
 * modeled 1:1 on the Koleo Travel Mode notification layout:
 *
 * - **Left Icon**: Circular Taal volcano badge (`R.drawable.ic_taal_notification` + blue accent `#0077FF`).
 * - **Header (`SubText`)**: `$elapsed min z $total min • Koniec o HH:mm`
 * - **Title (`ContentTitle`)**: `Przedmiot • Forma nauczania` (e.g. `Architektura systemów komputerowych • Laboratorium`)
 * - **Subtitle (`ContentText`)**: `Sala / Streaming • Prowadzący • x min z y min (do HH:mm)`
 * - **Right Icon (`LargeIcon`)**: Clean white monochrome Material Icon matching the subject on a transparent background.
 * - **Full-Width Progress Bar (`Notification.ProgressStyle`)**:
 *   - Multi-segment bar (`Zajęcia` = Blue `#0077FF`, `Przerwa` = Grey `#8A9199`, `Trwające zajęcia` = Green `#38A832`)
 *   - Circular white-ringed blue chevron tracker thumb (`setProgressTrackerIcon`) at the current time position.
 * - **Bottom Action Pills**: `"Szczegóły zajęć"` and `"Plan zajęć"`.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TaalLiveNotif"
        private const val LEGACY_CHANNEL_ID = "wsei_live_class_progress_channel"
        private const val LEGACY_CHANNEL_ID_V3 = "wsei_live_day_progress_lockscreen_v3"
        const val LIVE_CLASS_CHANNEL_ID = "taal_live_progress_style_v4"
        const val LIVE_CLASS_NOTIFICATION_ID = 4010
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"

        // Koleo-matching segment palette
        private const val COLOR_CLASS_BLUE = 0xFF0077FF.toInt()
        private const val COLOR_BREAK_GREY = 0xFF8A9199.toInt()
        private const val COLOR_ACTIVE_GREEN = 0xFF38A832.toInt()
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
            "Tryb Zajęć Taal (Na żywo)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                "Widoczne na ekranie blokady powiadomienie Live z pełnoszerokościowym paskiem postępu dnia (zajęcia - przerwa - zajęcia)."
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds the chronological sequence of `Zajęcia – Przerwa – Zajęcia` segments for the day.
     */
    fun buildDaySegments(dayEvents: List<ClassEvent>): List<DayTimelineSegment> {
        val sorted = dayEvents.sortedBy { it.timeSlot.startEpochMillis }
        if (sorted.isEmpty()) return emptyList()

        val segments = mutableListOf<DayTimelineSegment>()
        sorted.forEachIndexed { idx, event ->
            segments += DayTimelineSegment.ClassSegment(indexInDay = idx + 1, event = event)
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

    /**
     * Constructs the real-time ongoing Live Notification using Android 16's native `Notification.ProgressStyle`
     * (matching Koleo's layout) with Lock Screen visibility.
     */
    /** Minimalne powiadomienie wymagane do natychmiastowego `startForeground()` zanim wczytamy dane zajęć. */
    fun buildPlaceholderNotification(): Notification {
        ensureChannelCreated()
        return Notification.Builder(context, LIVE_CLASS_CHANNEL_ID)
            .setSmallIcon(pl.lublin.wsei.plan.R.drawable.ic_taal_notification)
            .setContentTitle("Taal")
            .setContentText("Ładowanie planu zajęć…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun buildLiveClassNotification(
        activeClass: ClassEvent,
        nextClassPreview: NextClassPreview?,
        dayEvents: List<ClassEvent> = listOf(activeClass),
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Notification {
        ensureChannelCreated()

        val allDayEvents = if (dayEvents.isEmpty()) {
            listOf(activeClass)
        } else {
            dayEvents.sortedBy { it.timeSlot.startEpochMillis }
        }
        val segments = buildDaySegments(allDayEvents)
        val activeSegment = segments.firstOrNull { it.isOngoing(nowEpochMillis) }
            ?: segments.firstOrNull { it is DayTimelineSegment.ClassSegment && it.event.id == activeClass.id }
            ?: segments.firstOrNull()

        val isBreakState = activeSegment is DayTimelineSegment.BreakSegment
        val focusedEvent = when (activeSegment) {
            is DayTimelineSegment.ClassSegment -> activeSegment.event
            is DayTimelineSegment.BreakSegment -> activeSegment.nextEvent
            null -> activeClass
        }

        // 1. Czas zajęć (x min z y min) + o której koniec zajęć
        val (elapsedMinutes, totalMinutes, endFormatted, remainingMinutes) =
            if (isBreakState && activeSegment is DayTimelineSegment.BreakSegment) {
                val totalBrk = activeSegment.durationMinutes
                val elapsedBrk = (((nowEpochMillis - activeSegment.startMillis).coerceAtLeast(0L)) / 60_000L)
                    .toInt()
                    .coerceAtMost(totalBrk)
                val remBrk = (totalBrk - elapsedBrk).coerceAtLeast(0)
                Quadruple(elapsedBrk, totalBrk, focusedEvent.timeSlot.startFormatted, remBrk)
            } else {
                val totalCls = focusedEvent.totalDurationMinutes
                val elapsedCls = focusedEvent.elapsedMinutes(nowEpochMillis)
                val remCls = focusedEvent.remainingMinutes(nowEpochMillis)
                Quadruple(elapsedCls, totalCls, focusedEvent.timeSlot.endFormatted, remCls)
            }

        // 2. Przedmiot + forma nauczania (główny pogrubiony tytuł jak w Koleo)
        val titleText = if (isBreakState) {
            "Przerwa • Następne: ${focusedEvent.subject.name} (${focusedEvent.classType.polishLabel})"
        } else {
            "${focusedEvent.subject.name} • ${focusedEvent.classType.polishLabel}"
        }

        // 3. Informacja o sali/streamingu + kto prowadzi zajęcia + czas trwania i godzina końca
        val roomLabel = when {
            focusedEvent.deliveryMode == DeliveryMode.ELEARNING -> "E-learning"
            focusedEvent.deliveryMode == DeliveryMode.REMOTE || focusedEvent.room.isRemoteTeams -> "Streaming"
            else -> "Sala ${focusedEvent.room.number}"
        }
        val subtitleText = if (isBreakState) {
            "$roomLabel • ${focusedEvent.lecturer.fullDisplayName} • Start o $endFormatted ($elapsedMinutes z $totalMinutes min)"
        } else {
            "$roomLabel • ${focusedEvent.lecturer.fullDisplayName} • $elapsedMinutes z $totalMinutes min (koniec o $endFormatted)"
        }

        val headerSubText = if (isBreakState) {
            "Przerwa $elapsedMinutes z $totalMinutes min • Start o $endFormatted"
        } else {
            "$elapsedMinutes z $totalMinutes min • Koniec o $endFormatted"
        }

        // 4. Right-side clean white monochrome Material Icon matching the subject (transparent container, Koleo style)
        val rightSubjectIconBitmap = WseiIcons.renderSubjectBadgeBitmap(
            subjectName = focusedEvent.subject.name,
            classType = focusedEvent.classType,
            sizePx = 128,
            containerColorArgb = 0x00000000,
            iconColorArgb = 0xFFFFFFFF.toInt()
        )
        val rightSubjectIcon = Icon.createWithBitmap(rightSubjectIconBitmap)

        // 5. PendingIntents for notification tap & bottom pill buttons (Koleo style)
        val openDetailsIntent = PendingIntent.getActivity(
            context,
            focusedEvent.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("focused_event_id", focusedEvent.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openScheduleIntent = PendingIntent.getActivity(
            context,
            focusedEvent.id.hashCode() + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_tab", "schedule")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalVisualUnits = segments.sumOf { it.visualLengthUnits }.coerceAtLeast(1)
        val currentProgressUnits = computeVisualProgressUnits(segments, nowEpochMillis)

        val promotedExtras = Bundle().apply {
            putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            putString(
                EXTRA_SHORT_CRITICAL_TEXT,
                if (isBreakState) "Przerwa • ${remainingMinutes}m" else "Do $endFormatted"
            )
        }

        val leftActionTitle = if (isBreakState) {
            "Start o $endFormatted"
        } else {
            "Koniec o $endFormatted ($elapsedMinutes/$totalMinutes min)"
        }
        val rightActionTitle = "Plan zajęć"

        val publicVersion = buildPlatformNotification(
            titleText = titleText,
            subtitleText = subtitleText,
            headerSubText = headerSubText,
            rightSubjectIcon = rightSubjectIcon,
            segments = segments,
            activeSegment = activeSegment,
            totalVisualUnits = totalVisualUnits,
            currentProgressUnits = currentProgressUnits,
            openDetailsIntent = openDetailsIntent,
            openScheduleIntent = openScheduleIntent,
            leftActionTitle = leftActionTitle,
            rightActionTitle = rightActionTitle,
            promotedExtras = promotedExtras,
            publicNotification = null
        )

        return buildPlatformNotification(
            titleText = titleText,
            subtitleText = subtitleText,
            headerSubText = headerSubText,
            rightSubjectIcon = rightSubjectIcon,
            segments = segments,
            activeSegment = activeSegment,
            totalVisualUnits = totalVisualUnits,
            currentProgressUnits = currentProgressUnits,
            openDetailsIntent = openDetailsIntent,
            openScheduleIntent = openScheduleIntent,
            leftActionTitle = leftActionTitle,
            rightActionTitle = rightActionTitle,
            promotedExtras = promotedExtras,
            publicNotification = publicVersion
        )
    }

    private fun buildPlatformNotification(
        titleText: String,
        subtitleText: String,
        headerSubText: String,
        rightSubjectIcon: Icon,
        segments: List<DayTimelineSegment>,
        activeSegment: DayTimelineSegment?,
        totalVisualUnits: Int,
        currentProgressUnits: Int,
        openDetailsIntent: PendingIntent,
        openScheduleIntent: PendingIntent,
        leftActionTitle: String,
        rightActionTitle: String,
        promotedExtras: Bundle,
        publicNotification: Notification?
    ): Notification {
        val builder = Notification.Builder(context, LIVE_CLASS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taal_notification)
            .setColor(COLOR_CLASS_BLUE)
            .setContentTitle(titleText)
            .setContentText(subtitleText)
            .setSubText(headerSubText)
            .setLargeIcon(rightSubjectIcon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openDetailsIntent)
            .addExtras(promotedExtras)
            .addAction(
                Notification.Action.Builder(
                    null,
                    leftActionTitle,
                    openDetailsIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    rightActionTitle,
                    openScheduleIntent
                ).build()
            )

        if (publicNotification != null) {
            builder.setPublicVersion(publicNotification)
        }

        // Apply Android 16 native Notification.ProgressStyle (exact Koleo segmented bar + tracker icon)
        val appliedNativeProgressStyle = applyAndroid16ProgressStyle(
            builder = builder,
            segments = segments,
            activeSegment = activeSegment,
            currentProgressUnits = currentProgressUnits
        )

        if (!appliedNativeProgressStyle) {
            builder.setProgress(totalVisualUnits, currentProgressUnits, false)
        }

        return builder.build()
    }

    /**
     * Invokes Android 16 (`API 36+`) `android.app.Notification$ProgressStyle` directly on [builder]:
     * - Creates `Notification.ProgressStyle.Segment(visualLengthUnits)` for every class and break in the day
     * - Colors classes Blue (`#0077FF`), breaks Grey (`#8A9199`), and the currently active class Green (`#38A832`)
     * - Sets `setStyledByProgress(false)` so all segments retain their distinct colors across the full bar
     * - Attaches the circular white+blue chevron tracker thumb via `setProgressTrackerIcon`
     */
    private fun applyAndroid16ProgressStyle(
        builder: Notification.Builder,
        segments: List<DayTimelineSegment>,
        activeSegment: DayTimelineSegment?,
        currentProgressUnits: Int
    ): Boolean = runCatching {
        val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
        val segmentClass = Class.forName("android.app.Notification\$ProgressStyle\$Segment")

        val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
        val segmentConstructor = segmentClass.getDeclaredConstructor(Int::class.javaPrimitiveType)
        val setSegmentColor = segmentClass.getMethod("setColor", Int::class.javaPrimitiveType)

        val platformSegments = ArrayList<Any>(segments.size)
        for (seg in segments) {
            val segInstance = segmentConstructor.newInstance(seg.visualLengthUnits)
            val segColor = when {
                seg is DayTimelineSegment.BreakSegment -> COLOR_BREAK_GREY
                seg === activeSegment -> COLOR_ACTIVE_GREEN
                else -> COLOR_CLASS_BLUE
            }
            setSegmentColor.invoke(segInstance, segColor)
            platformSegments.add(segInstance)
        }

        runCatching {
            progressStyleClass
                .getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
                .invoke(progressStyle, false)
        }

        progressStyleClass
            .getMethod("setProgressSegments", List::class.java)
            .invoke(progressStyle, platformSegments)

        progressStyleClass
            .getMethod("setProgress", Int::class.javaPrimitiveType)
            .invoke(progressStyle, currentProgressUnits)

        val trackerBitmap = WseiIcons.renderProgressTrackerThumbBitmap(
            sizePx = 72,
            circleColorArgb = COLOR_CLASS_BLUE
        )
        val trackerIcon = Icon.createWithBitmap(trackerBitmap)
        progressStyleClass
            .getMethod("setProgressTrackerIcon", Icon::class.java)
            .invoke(progressStyle, trackerIcon)

        // Enable Android 16 promoted ongoing live notification if method exists on Builder
        runCatching {
            Notification.Builder::class.java
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        }

        builder.setStyle(progressStyle as Notification.Style)
        true
    }.onFailure { err ->
        Log.w(TAG, "Fallback to standard progress bar: ${err.message}")
    }.getOrDefault(false)

    private fun computeVisualProgressUnits(
        segments: List<DayTimelineSegment>,
        nowMillis: Long
    ): Int {
        if (segments.isEmpty()) return 0
        var accumulated = 0
        for (seg in segments) {
            when {
                nowMillis >= seg.endMillis -> {
                    accumulated += seg.visualLengthUnits
                }
                nowMillis > seg.startMillis -> {
                    val fraction = seg.progressAt(nowMillis)
                    accumulated += (seg.visualLengthUnits * fraction).toInt()
                    return accumulated
                }
                else -> {
                    return accumulated
                }
            }
        }
        return accumulated
    }

    fun publishLiveNotification(notification: Notification) {
        notificationManager.notify(LIVE_CLASS_NOTIFICATION_ID, notification)
    }

    fun notifyLiveClassUpdate(
        activeClass: ClassEvent,
        nextClassPreview: NextClassPreview?,
        dayEvents: List<ClassEvent>,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) {
        val notification = buildLiveClassNotification(
            activeClass = activeClass,
            nextClassPreview = nextClassPreview,
            dayEvents = dayEvents,
            nowEpochMillis = nowEpochMillis
        )
        notificationManager.notify(LIVE_CLASS_NOTIFICATION_ID, notification)
    }

    fun cancelLiveNotification() {
        notificationManager.cancel(LIVE_CLASS_NOTIFICATION_ID)
    }

    fun cancelLiveClassNotification() {
        notificationManager.cancel(LIVE_CLASS_NOTIFICATION_ID)
    }

    /**
     * Kept for in-app preview compatibility if needed.
     */
    fun renderFullWidthDayProgressBitmap(
        segments: List<DayTimelineSegment>,
        nowMillis: Long,
        widthPx: Int = 960,
        heightPx: Int = 52,
        showLabels: Boolean = false
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (segments.isEmpty()) return bmp

        val gapPx = 10f
        val horizontalPad = 18f
        val usableWidth = (widthPx - 2 * horizontalPad - gapPx * (segments.size - 1).coerceAtLeast(0)).coerceAtLeast(100f)
        val totalWeight = segments.sumOf { it.visualLengthUnits }.toFloat().coerceAtLeast(1f)
        val barTop = heightPx * 0.25f
        val barHeight = heightPx * 0.42f
        val barBottom = barTop + barHeight
        val cornerRadius = barHeight / 2f

        var curX = horizontalPad
        var thumbX: Float? = null

        segments.forEach { seg ->
            val segW = (usableWidth * (seg.visualLengthUnits / totalWeight)).coerceAtLeast(18f)
            val rect = RectF(curX, barTop, curX + segW, barBottom)
            val isOngoing = seg.isOngoing(nowMillis)

            paint.style = Paint.Style.FILL
            paint.color = when {
                seg is DayTimelineSegment.BreakSegment -> COLOR_BREAK_GREY
                isOngoing -> COLOR_ACTIVE_GREEN
                else -> COLOR_CLASS_BLUE
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            if (isOngoing) {
                val frac = seg.progressAt(nowMillis)
                thumbX = (curX + segW * frac).coerceIn(curX + cornerRadius, curX + segW - cornerRadius)
            }
            curX += segW + gapPx
        }

        thumbX?.let { cx ->
            val cy = (barTop + barBottom) / 2f
            paint.style = Paint.Style.FILL
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy, barHeight * 0.72f, paint)
            paint.color = COLOR_CLASS_BLUE
            canvas.drawCircle(cx, cy, barHeight * 0.52f, paint)
        }

        if (showLabels) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        return bmp
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
