package pl.lublin.wsei.plan.data.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncManager
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncResult
import pl.lublin.wsei.plan.domain.calendar.DeviceCalendar
import pl.lublin.wsei.plan.domain.model.ClassEvent
import javax.inject.Inject
import javax.inject.Singleton

/** [CalendarSyncManager] backed by Android `CalendarContract`. */
@Singleton
class AndroidCalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarSyncManager {
    override suspend fun listWritableCalendars(): Result<List<DeviceCalendar>> =
        Result.failure(UnsupportedOperationException())

    override suspend fun syncEvents(
        calendarId: Long,
        events: List<ClassEvent>,
        hideLecturer: Boolean
    ): Result<CalendarSyncResult> = Result.failure(UnsupportedOperationException())

    override suspend fun removeSyncedEvents(calendarId: Long): Result<Int> =
        Result.failure(UnsupportedOperationException())
}
