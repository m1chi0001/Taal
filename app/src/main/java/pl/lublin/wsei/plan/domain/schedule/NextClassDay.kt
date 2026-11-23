package pl.lublin.wsei.plan.domain.schedule

import androidx.compose.runtime.Immutable

/**
 * Summary of the next day with classes, shared by Start's focus card and the day-off state of
 * Plan zajęć. Produced by [ScheduleFocusResolver.nextClassDayAfter].
 */
@Immutable
data class NextClassDay(
    /** Date of the class day (Europe/Warsaw, `YYYY-MM-DD`). */
    val dateIso: String,
    /**
     * Calendar days from today (the Warsaw date of `now`) to [dateIso]: 0 today, 1 tomorrow,
     * negative when the day is already in the past (e.g. when browsing an old week).
     */
    val daysFromToday: Int,
    /** Start of the day's first class. */
    val firstStartMillis: Long,
    /** Start time of the first class as shown in the app (e.g. "15:20"). */
    val firstStartLabel: String,
    /** Number of classes (blocks) on that day. */
    val blockCount: Int,
    /**
     * True when no other class day lies within the zjazd gap right before [dateIso], i.e. this
     * day opens a zjazd ("Następny zjazd") rather than continuing one.
     */
    val startsZjazd: Boolean
)
