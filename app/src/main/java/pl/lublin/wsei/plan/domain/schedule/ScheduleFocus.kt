package pl.lublin.wsei.plan.domain.schedule

import androidx.compose.runtime.Immutable
import pl.lublin.wsei.plan.domain.model.ClassEvent

/** Why a day was chosen as the one to show on Start and as the default day of Plan zajęć. */
enum class ScheduleFocusKind {
    /** Today, with a class in progress or still to come. */
    TODAY,
    /** Today is over (or empty) and the next class day is tomorrow. */
    TOMORROW,
    /** A later day of the same zjazd (consecutive class days, e.g. Fri → Sun). */
    LATER_THIS_ZJAZD,
    /** The first day of the next zjazd. */
    NEXT_ZJAZD,
    /** No upcoming classes are known. */
    NONE
}

/**
 * The day the app should focus on right now. [dateIso] is in Europe/Warsaw like [ClassEvent.dateIso];
 * [events] are that day's classes sorted by start time.
 */
@Immutable
data class ScheduleFocus(
    val kind: ScheduleFocusKind,
    val dateIso: String?,
    val events: List<ClassEvent>,
    /** Class in progress now (only for [ScheduleFocusKind.TODAY]). */
    val activeEvent: ClassEvent?,
    /** Next class that has not started yet within [events]. */
    val nextEvent: ClassEvent?
) {
    val isToday: Boolean get() = kind == ScheduleFocusKind.TODAY

    companion object {
        val None = ScheduleFocus(ScheduleFocusKind.NONE, null, emptyList(), null, null)
    }
}
