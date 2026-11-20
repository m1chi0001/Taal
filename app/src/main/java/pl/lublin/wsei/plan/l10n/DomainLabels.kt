package pl.lublin.wsei.plan.l10n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.datetime.DayOfWeek
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType

/*
 * Localized display labels for domain enums and value objects.
 *
 * The Polish properties on the domain models (`ClassType.polishLabel`, `Room.displayLabel`, …) stay
 * untouched because the data layer uses them to match and normalize server data; everything shown
 * to the user goes through the resource-based labels below.
 */

/** Full, human-readable name of the class form ("Lecture", "Laboratory"…). */
@get:StringRes
val ClassType.labelRes: Int
    get() = when (this) {
        ClassType.LECTURE -> R.string.class_type_lecture
        ClassType.LAB -> R.string.class_type_lab
        ClassType.EXERCISE -> R.string.class_type_exercise
        ClassType.PROJECT -> R.string.class_type_project
        ClassType.ELEARNING -> R.string.class_type_elearning
        ClassType.PRACTICE -> R.string.class_type_practice
        ClassType.SEMINAR -> R.string.class_type_seminar
        ClassType.EXAM -> R.string.class_type_exam
    }

/** Badge describing how the class is delivered (on site, streaming, e-learning). */
@get:StringRes
val DeliveryMode.labelRes: Int
    get() = when (this) {
        DeliveryMode.ON_SITE -> R.string.delivery_mode_on_site
        DeliveryMode.REMOTE -> R.string.delivery_mode_remote
        DeliveryMode.ELEARNING -> R.string.delivery_mode_elearning
    }

/** Name of the schedule variant (the student's own plan or the whole group's plan). */
@get:StringRes
val ScheduleFilterType.labelRes: Int
    get() = when (this) {
        ScheduleFilterType.MY_PLAN -> R.string.schedule_filter_my_plan
        ScheduleFilterType.MY_GROUP_PLAN -> R.string.schedule_filter_group_plan
    }

/** Full day name, e.g. "Poniedziałek" / "Monday" / "Понеділок". */
@get:StringRes
val DayOfWeek.fullNameRes: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> R.string.day_monday
        DayOfWeek.TUESDAY -> R.string.day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday
        DayOfWeek.THURSDAY -> R.string.day_thursday
        DayOfWeek.FRIDAY -> R.string.day_friday
        DayOfWeek.SATURDAY -> R.string.day_saturday
        DayOfWeek.SUNDAY -> R.string.day_sunday
        else -> R.string.day_monday
    }

/** Abbreviated day name for the week strip, e.g. "Pon" / "Mon" / "Пн". */
@get:StringRes
val DayOfWeek.shortNameRes: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> R.string.day_monday_short
        DayOfWeek.TUESDAY -> R.string.day_tuesday_short
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday_short
        DayOfWeek.THURSDAY -> R.string.day_thursday_short
        DayOfWeek.FRIDAY -> R.string.day_friday_short
        DayOfWeek.SATURDAY -> R.string.day_saturday_short
        DayOfWeek.SUNDAY -> R.string.day_sunday_short
        else -> R.string.day_monday_short
    }

/** Room line without the address: "Sala 205K", "Streaming (zajęcia zdalne)", "E-learning". */
@Composable
fun Room.localizedLabel(): String = when {
    isRemoteTeams && number.equals("E-learning", ignoreCase = true) -> stringResource(R.string.room_elearning)
    isRemoteTeams -> stringResource(R.string.room_streaming_remote)
    number.isBlank() -> stringResource(R.string.room_tbd)
    else -> stringResource(R.string.room_number, number)
}

/** Same as the composable [localizedLabel], for code outside Compose (calendar export, notifications). */
fun Room.localizedLabel(context: Context): String = when {
    isRemoteTeams && number.equals("E-learning", ignoreCase = true) -> context.getString(R.string.room_elearning)
    isRemoteTeams -> context.getString(R.string.room_streaming_remote)
    number.isBlank() -> context.getString(R.string.room_tbd)
    else -> context.getString(R.string.room_number, number)
}

/** Lecturer name, or a readable placeholder when the dean's office did not provide one. */
@Composable
fun Lecturer.localizedNameOrPlaceholder(): String =
    fullDisplayName.ifBlank { stringResource(R.string.lecturer_missing) }
