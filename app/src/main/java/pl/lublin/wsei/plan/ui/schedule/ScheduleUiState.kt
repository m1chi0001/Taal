package pl.lublin.wsei.plan.ui.schedule

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pl.lublin.wsei.plan.data.repository.toPolishFullName
import pl.lublin.wsei.plan.data.repository.toPolishShortName
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncResult
import pl.lublin.wsei.plan.domain.calendar.DeviceCalendar
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.StudentSemesterGroups
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.model.StudyPlanOverview
import pl.lublin.wsei.plan.domain.model.StudyTrackInfo
import pl.lublin.wsei.plan.domain.model.StudyTrackParser
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocus
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
import java.time.Instant

/** Kept for source compatibility — the enum lives in the domain layer. */
typealias AppThemeMode = pl.lublin.wsei.plan.domain.model.AppThemeMode

/** Progress of the schedule → device calendar export. */
sealed interface CalendarSyncStatus {
    data object Idle : CalendarSyncStatus
    data object LoadingCalendars : CalendarSyncStatus
    data object Syncing : CalendarSyncStatus
    data class Done(val result: CalendarSyncResult) : CalendarSyncStatus
    data class Failed(val message: String?) : CalendarSyncStatus
}

@Immutable
data class ScheduleUiState(
    val currentSession: StudentSession? = null,
    val savedEncryptedLoginHint: String? = null,
    val isAuthenticating: Boolean = false,
    val isSessionRestoring: Boolean = true,
    val authError: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val nowEpochMillis: Long = System.currentTimeMillis(),
    val selectedDate: LocalDate = currentWarsawLocalDate(nowEpochMillis),
    val displayedYear: Int = selectedDate.year,
    val displayedMonth: Int = selectedDate.monthNumber,
    val isCalendarExpanded: Boolean = false,
    val allEvents: List<ClassEvent> = emptyList(),
    val scheduleFilterType: ScheduleFilterType = ScheduleFilterType.MY_PLAN,
    val isHeroExpanded: Boolean = true,
    val syncOrigin: DataSourceOrigin = DataSourceOrigin.APR_WD_API,
    val studentGroups: List<StudentSemesterGroups> = emptyList(),
    val studyPlans: List<StudyPlanOverview> = emptyList(),
    val grades: List<GradeItem> = emptyList(),
    val scholarships: List<ScholarshipApplication> = emptyList(),
    val financials: List<FinancialObligation> = emptyList(),
    val announcements: List<DeanAnnouncement> = emptyList(),
    val statusBannerMessage: String? = null,
    val errorMessage: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val colorPalette: AppColorPalette = AppColorPalette.TAAL,
    val isLiveNotificationEnabled: Boolean = true,
    val isHideSensitiveData: Boolean = false,
    /** Writable device calendars, loaded on demand for the sync picker. */
    val deviceCalendars: List<DeviceCalendar> = emptyList(),
    /** Calendar last used for sync (persisted), preselected in the picker. */
    val syncCalendarId: Long? = null,
    val calendarSyncStatus: CalendarSyncStatus = CalendarSyncStatus.Idle,
    /** Id of the attachment being downloaded/opened right now, for a per-item loading indicator. */
    val openingAttachmentId: Int? = null
) {
    /**
     * Lecturer name/details as they may be shown: masked when "Ukryj dane" is on.
     * Use for every lecturer label (cards, sheets, subjects, notifications).
     */
    fun maskLecturer(value: String): String =
        if (isHideSensitiveData && value.isNotBlank()) "••••••" else value

    /** Masks sensitive student fields (`••••••`) when privacy mode is enabled. */
    fun maskSensitive(value: String): String = if (isHideSensitiveData) "••••••" else value

    /** Unread announcements count — computed once per state instance with lock-free thread safety. */
    val unreadAnnouncementsCount: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        announcements.count { !it.isRead }
    }

    /**
     * Dynamically builds the 7-day Monday..Sunday week containing [selectedDate], filtering to the
     * 7-day window before grouping so multi-month event lists do not allocate unused map buckets.
     */
    val days: List<AcademicDay> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val weekMonday = selectedDate.minus(DatePeriod(days = selectedDate.dayOfWeek.ordinal))
        val weekSunday = weekMonday.plus(DatePeriod(days = 6))
        val fromIso = weekMonday.toString()
        val toIso = weekSunday.toString()
        val eventsByDate = allEvents
            .filter { it.dateIso in fromIso..toIso }
            .groupBy { it.dateIso }
        (0..6).map { offset ->
            val date = weekMonday.plus(DatePeriod(days = offset))
            val dayEvents = eventsByDate[date.toString()]
                .orEmpty()
                .sortedBy { it.timeSlot.startEpochMillis }
            AcademicDay(
                date = date,
                dayOfWeekLabel = date.dayOfWeek.toPolishFullName(),
                shortDayLabel = date.dayOfWeek.toPolishShortName(),
                dayOfMonthNumber = date.dayOfMonth.toString().padStart(2, '0'),
                events = dayEvents,
                isWeekendExtramuralSession = date.dayOfWeek == DayOfWeek.SATURDAY ||
                    date.dayOfWeek == DayOfWeek.SUNDAY
            )
        }
    }

    /**
     * Number of classes in the current calendar week (Mon–Sun around [nowEpochMillis]), independent
     * of [selectedDate]. Used by the Start dashboard tile.
     */
    val currentWeekClassCount: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val today = currentWarsawLocalDate(nowEpochMillis)
        val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val from = monday.toString()
        val to = monday.plus(DatePeriod(days = 6)).toString()
        allEvents.count { it.dateIso in from..to }
    }

    val selectedDayIndex: Int
        get() = selectedDate.dayOfWeek.ordinal

    val selectedDay: AcademicDay? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        days.getOrNull(selectedDayIndex)
    }

    /**
     * All day-of-month numbers (`1..31`) in `[displayedYear]-[displayedMonth]` that have scheduled classes.
     */
    val scheduledMonthDays: Set<Int> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val prefix = buildString(8) {
            append(displayedYear)
            append('-')
            if (displayedMonth < 10) append('0')
            append(displayedMonth)
            append('-')
        }
        allEvents
            .asSequence()
            .filter { it.dateIso.startsWith(prefix) }
            .mapNotNull { it.dateIso.takeLast(2).toIntOrNull() }
            .toSet()
    }

    val activeClass: ClassEvent? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        allEvents.firstOrNull { it.isOngoing(nowEpochMillis) }
    }

    /**
     * Event whose live notification should stay active: either an ongoing class or the next class
     * after a break (when an earlier class has already finished today).
     */
    val liveTrackingEventId: String? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        activeClass?.id ?: run {
            val todayIso = currentWarsawLocalDate(nowEpochMillis).toString()
            val today = allEvents.filter { it.dateIso == todayIso }
            val hasFinishedToday = today.any { it.timeSlot.endEpochMillis <= nowEpochMillis }
            if (!hasFinishedToday) {
                null
            } else {
                today
                    .filter { it.timeSlot.startEpochMillis > nowEpochMillis }
                    .minByOrNull { it.timeSlot.startEpochMillis }
                    ?.id
            }
        }
    }

    val activeDayEvents: List<ClassEvent> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val targetDateIso = activeClass?.dateIso ?: selectedDate.toString()
        allEvents
            .filter { it.dateIso == targetDateIso }
            .sortedBy { it.timeSlot.startEpochMillis }
    }

    val nextClassPreview: NextClassPreview? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val active = activeClass ?: return@lazy null
        // When activeClass != null, activeDayEvents is already filtered to active.dateIso and sorted.
        val next = activeDayEvents.firstOrNull {
            it.timeSlot.startEpochMillis >= active.timeSlot.endEpochMillis && it.id != active.id
        } ?: return@lazy null
        val breakMinutes = ((next.timeSlot.startEpochMillis - active.timeSlot.endEpochMillis) / 60_000L)
            .toInt()
            .coerceAtLeast(0)
        NextClassPreview(
            nextEvent = next,
            breakDurationMinutes = breakMinutes
        )
    }

    /**
     * Day to focus on now (today, tomorrow, later this zjazd or the next zjazd) — shared by Start
     * and the default day of Plan zajęć. Recomputed every minute via [nowEpochMillis].
     */
    val scheduleFocus: ScheduleFocus by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ScheduleFocusResolver.resolve(allEvents, nowEpochMillis)
    }

    /** "Tok nauki" (year of study, academic year, semester, mode) — separate from class groups. */
    val studyTrack: StudyTrackInfo? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StudyTrackParser.from(studentGroups, currentSession?.courseOfStudyCode, currentSession?.studyMode)
    }

    /** Class groups per semester, without study-track ("Tok") entries. */
    val classGroupsBySemester: List<StudentSemesterGroups> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        studentGroups.map { sem -> sem.copy(groups = sem.groups.filterNot { it.isStudyTrack }) }
    }

    /** First real group code (or the course-of-study code); null when the student has none. */
    val primaryStudentGroupLabel: String?
        get() {
            val firstGroup = studentGroups
                .firstOrNull()
                ?.groups
                ?.firstOrNull { !it.isStudyTrack && !it.formName.equals("Tok", ignoreCase = true) }
                ?.groupCode
            return firstGroup ?: currentSession?.courseOfStudyCode
        }
}

internal fun currentWarsawLocalDate(nowMillis: Long = System.currentTimeMillis()): LocalDate {
    val jt = Instant.ofEpochMilli(nowMillis).atZone(ScheduleFocusResolver.WARSAW).toLocalDate()
    return LocalDate(jt.year, jt.monthValue, jt.dayOfMonth)
}
