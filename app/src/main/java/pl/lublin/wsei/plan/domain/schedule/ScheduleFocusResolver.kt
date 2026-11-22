package pl.lublin.wsei.plan.domain.schedule

import pl.lublin.wsei.plan.domain.model.ClassEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Pure resolver of [ScheduleFocus]: today while something is ongoing or still to come today,
 * otherwise the next day with classes (tomorrow / later this zjazd / next zjazd).
 *
 * Rules:
 * - Today (date of `now` in [zone]) has a class in progress or still to start → [ScheduleFocusKind.TODAY].
 * - Otherwise the first date after today with classes is chosen:
 *   - it is tomorrow → [ScheduleFocusKind.TOMORROW];
 *   - the last class day (today or yesterday) is at most [MAX_ZJAZD_GAP_DAYS] days before it
 *     (e.g. Fri → Sun of one weekend) → [ScheduleFocusKind.LATER_THIS_ZJAZD];
 *   - else → [ScheduleFocusKind.NEXT_ZJAZD].
 * - No upcoming classes → [ScheduleFocus.None].
 */
object ScheduleFocusResolver {
    val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

    /** Largest gap (in days) between two class days that still belong to the same zjazd. */
    private const val MAX_ZJAZD_GAP_DAYS = 2L

    fun resolve(events: List<ClassEvent>, nowEpochMillis: Long, zone: ZoneId = WARSAW): ScheduleFocus {
        if (events.isEmpty()) return ScheduleFocus.None
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()

        val byDate: Map<LocalDate, List<ClassEvent>> = events
            .mapNotNull { event -> event.localDate(zone)?.let { it to event } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dayEvents) -> dayEvents.sortedWith(START_ORDER) }

        val todayEvents = byDate[today].orEmpty()
        val active = todayEvents.firstOrNull { it.isOngoing(nowEpochMillis) }
        val next = todayEvents.firstOrNull { it.isUpcoming(nowEpochMillis) }
        if (active != null || next != null) {
            return ScheduleFocus(
                kind = ScheduleFocusKind.TODAY,
                dateIso = today.toString(),
                events = todayEvents,
                activeEvent = active,
                nextEvent = next
            )
        }

        val nextDate = byDate.keys.filter { it.isAfter(today) }.minOrNull() ?: return ScheduleFocus.None
        val nextDayEvents = byDate.getValue(nextDate)

        val lastClassDay = when {
            todayEvents.isNotEmpty() -> today
            byDate[today.minusDays(1)].orEmpty().isNotEmpty() -> today.minusDays(1)
            else -> null
        }
        val kind = when {
            nextDate == today.plusDays(1) -> ScheduleFocusKind.TOMORROW
            lastClassDay != null &&
                ChronoUnit.DAYS.between(lastClassDay, nextDate) <= MAX_ZJAZD_GAP_DAYS -> ScheduleFocusKind.LATER_THIS_ZJAZD
            else -> ScheduleFocusKind.NEXT_ZJAZD
        }
        return ScheduleFocus(
            kind = kind,
            dateIso = nextDate.toString(),
            events = nextDayEvents,
            activeEvent = null,
            nextEvent = nextDayEvents.firstOrNull()
        )
    }

    private val START_ORDER = compareBy<ClassEvent>({ it.timeSlot.startEpochMillis }, { it.timeSlot.endEpochMillis })

    /** Date of the event: its [ClassEvent.dateIso], or the start instant in [zone] if that is malformed. */
    private fun ClassEvent.localDate(zone: ZoneId): LocalDate? =
        try {
            LocalDate.parse(dateIso.trim().take(10))
        } catch (e: DateTimeParseException) {
            if (timeSlot.startEpochMillis > 0) {
                Instant.ofEpochMilli(timeSlot.startEpochMillis).atZone(zone).toLocalDate()
            } else {
                null
            }
        }
}
