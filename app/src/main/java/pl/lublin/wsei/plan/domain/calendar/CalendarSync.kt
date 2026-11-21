package pl.lublin.wsei.plan.domain.calendar

import androidx.compose.runtime.Immutable
import pl.lublin.wsei.plan.domain.model.ClassEvent

/** A writable calendar available on the device (e.g. a Google account calendar). */
@Immutable
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val colorArgb: Int?,
    val isPrimary: Boolean
)

@Immutable
data class CalendarSyncResult(
    val inserted: Int,
    val updated: Int,
    val deleted: Int
)

/**
 * Two-way-safe, one-directional export of the class schedule into a device calendar.
 * Implementations must be idempotent: every class gets a deterministic sync marker, so repeated
 * syncs update existing events and remove ones that disappeared, never creating duplicates.
 * Callers must hold READ_CALENDAR and WRITE_CALENDAR before calling.
 */
interface CalendarSyncManager {
    suspend fun listWritableCalendars(): Result<List<DeviceCalendar>>

    /**
     * Upserts [events] into [calendarId]. When [hideLecturer] is true, lecturer names and
     * details must not be written into the event title, location or description.
     */
    suspend fun syncEvents(
        calendarId: Long,
        events: List<ClassEvent>,
        hideLecturer: Boolean
    ): Result<CalendarSyncResult>

    /** Removes every event previously created by Taal from [calendarId]; returns the count. */
    suspend fun removeSyncedEvents(calendarId: Long): Result<Int>
}
