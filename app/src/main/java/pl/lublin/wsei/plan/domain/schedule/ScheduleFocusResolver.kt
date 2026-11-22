package pl.lublin.wsei.plan.domain.schedule

import pl.lublin.wsei.plan.domain.model.ClassEvent
import java.time.ZoneId

/**
 * Pure resolver of [ScheduleFocus]: today while something is ongoing or still to come today,
 * otherwise the next day with classes (tomorrow / later this zjazd / next zjazd).
 */
object ScheduleFocusResolver {
    val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

    fun resolve(events: List<ClassEvent>, nowEpochMillis: Long, zone: ZoneId = WARSAW): ScheduleFocus =
        ScheduleFocus.None
}
