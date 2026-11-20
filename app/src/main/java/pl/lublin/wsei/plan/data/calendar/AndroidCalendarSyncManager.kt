package pl.lublin.wsei.plan.data.calendar

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncManager
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncResult
import pl.lublin.wsei.plan.domain.calendar.DeviceCalendar
import pl.lublin.wsei.plan.domain.model.ClassEvent
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Failure of a calendar operation with a user-facing (Polish) [message]. */
class CalendarSyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * [CalendarSyncManager] backed by Android `CalendarContract`.
 *
 * ### How Taal recognises its own events
 * Taal is a regular app, not a sync adapter, so the `SYNC_DATA*` columns are off limits, and
 * `UID_2445` is unreliable (account sync adapters, e.g. Google, overwrite it with their own iCal UID).
 * Every exported class therefore carries a deterministic key — the first 20 hex chars of
 * SHA-256(`ClassEvent.id`) — stored twice:
 * 1. in `CUSTOM_APP_PACKAGE` (= our package) + `CUSTOM_APP_URI` (`taal://class/<key>`), which apps may
 *    write and which make the event open Taal from the calendar app;
 * 2. as a `[taal:<key>]` marker on the last line of `DESCRIPTION`, a fallback for providers that drop
 *    the custom-app columns after a server round-trip.
 *
 * An event is treated as Taal's only when one of these yields a key, so user events are never touched.
 * A sync upserts by key and deletes orphaned Taal events only inside the synced days.
 */
@Singleton
class AndroidCalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) : CalendarSyncManager {

    private val resolver get() = context.contentResolver

    override suspend fun listWritableCalendars(): Result<List<DeviceCalendar>> = calendarCall {
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.OWNER_ACCOUNT,
            Calendars.CALENDAR_COLOR,
            Calendars.IS_PRIMARY
        )
        val selection = "${Calendars.VISIBLE} = 1 AND ${Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val calendars = mutableListOf<DeviceCalendar>()
        resolver.query(Calendars.CONTENT_URI, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Calendars._ID)
            val nameCol = c.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME)
            val accountCol = c.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME)
            val ownerCol = c.getColumnIndexOrThrow(Calendars.OWNER_ACCOUNT)
            val colorCol = c.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR)
            val primaryCol = c.getColumnIndexOrThrow(Calendars.IS_PRIMARY)
            while (c.moveToNext()) {
                val account = c.stringOrEmpty(accountCol)
                val owner = c.stringOrEmpty(ownerCol)
                calendars += DeviceCalendar(
                    id = c.getLong(idCol),
                    displayName = c.stringOrEmpty(nameCol)
                        .ifBlank { account.ifBlank { context.getString(R.string.calendar_unnamed) } },
                    accountName = account,
                    colorArgb = if (c.isNull(colorCol)) null else c.getInt(colorCol),
                    isPrimary = (!c.isNull(primaryCol) && c.getInt(primaryCol) == 1) ||
                        (account.isNotBlank() && account.equals(owner, ignoreCase = true))
                )
            }
        }
        calendars.sortedWith(
            compareByDescending<DeviceCalendar> { it.isPrimary }
                .thenBy { it.accountName.lowercase() }
                .thenBy { it.displayName.lowercase() }
        )
    }

    override suspend fun syncEvents(
        calendarId: Long,
        events: List<ClassEvent>,
        hideLecturer: Boolean
    ): Result<CalendarSyncResult> = calendarCall {
        requireWritableCalendar(calendarId)

        val desired = events
            .filter { it.timeSlot.endEpochMillis > it.timeSlot.startEpochMillis }
            .associateBy { syncKey(it.id) }
        if (desired.isEmpty()) return@calendarCall CalendarSyncResult(inserted = 0, updated = 0, deleted = 0)

        // Orphans are removed only inside the synced days, so syncing one month keeps the others.
        val rangeStart = startOfDay(desired.values.minOf { it.timeSlot.startEpochMillis })
        val rangeEnd = endOfDay(desired.values.maxOf { it.timeSlot.endEpochMillis })

        val existing = queryTaalEvents(calendarId)
        val ops = ArrayList<ContentProviderOperation>()
        var inserted = 0
        var updated = 0
        var deleted = 0
        val seenKeys = HashSet<String>()

        for (row in existing) {
            val target = desired[row.key]
            when {
                target != null && seenKeys.add(row.key) -> {
                    val values = eventValues(calendarId, row.key, target, hideLecturer)
                    if (!row.matches(values)) {
                        ops += ContentProviderOperation.newUpdate(eventUri(row.id)).withValues(values).build()
                        updated++
                    }
                }
                // A duplicate of an already matched key, or a class that is gone from the synced range.
                target != null || row.startMillis in rangeStart..rangeEnd -> {
                    ops += ContentProviderOperation.newDelete(eventUri(row.id)).build()
                    deleted++
                }
            }
        }
        for ((key, event) in desired) {
            if (key in seenKeys) continue
            ops += ContentProviderOperation.newInsert(Events.CONTENT_URI)
                .withValues(eventValues(calendarId, key, event, hideLecturer))
                .build()
            inserted++
        }
        applyInBatches(ops)
        CalendarSyncResult(inserted = inserted, updated = updated, deleted = deleted)
    }

    override suspend fun removeSyncedEvents(calendarId: Long): Result<Int> = calendarCall {
        val ops = queryTaalEvents(calendarId).map { ContentProviderOperation.newDelete(eventUri(it.id)).build() }
        applyInBatches(ops)
        ops.size
    }

    // ---------------------------------------------------------------------------------------------

    private fun requireWritableCalendar(calendarId: Long) {
        val writable = resolver.query(
            ContentUris.withAppendedId(Calendars.CONTENT_URI, calendarId),
            arrayOf(Calendars.CALENDAR_ACCESS_LEVEL),
            null,
            null,
            null
        )?.use { c -> c.moveToFirst() && c.getInt(0) >= Calendars.CAL_ACCESS_CONTRIBUTOR } ?: false
        if (!writable) throw CalendarSyncException(context.getString(R.string.calendar_error_unavailable))
    }

    private fun queryTaalEvents(calendarId: Long): List<ExistingEvent> {
        val projection = arrayOf(
            Events._ID,
            Events.TITLE,
            Events.EVENT_LOCATION,
            Events.DESCRIPTION,
            Events.DTSTART,
            Events.DTEND,
            Events.EVENT_TIMEZONE,
            Events.CUSTOM_APP_PACKAGE,
            Events.CUSTOM_APP_URI
        )
        val selection = "${Events.CALENDAR_ID} = ? AND ${Events.DELETED} = 0 AND " +
            "(${Events.CUSTOM_APP_PACKAGE} = ? OR ${Events.DESCRIPTION} LIKE ?)"
        val args = arrayOf(calendarId.toString(), context.packageName, "%$MARKER_PREFIX%")
        val result = mutableListOf<ExistingEvent>()
        resolver.query(Events.CONTENT_URI, projection, selection, args, "${Events.DTSTART} ASC")?.use { c ->
            while (c.moveToNext()) {
                val description = c.stringOrEmpty(3)
                val appUri = c.stringOrEmpty(8)
                val key = appUri.takeIf { it.startsWith(APP_URI_PREFIX) }?.removePrefix(APP_URI_PREFIX)
                    ?.takeIf { KEY_REGEX.matches(it) }
                    ?: MARKER_REGEX.find(description)?.groupValues?.get(1)
                    ?: continue
                result += ExistingEvent(
                    id = c.getLong(0),
                    key = key,
                    title = c.stringOrEmpty(1),
                    location = c.stringOrEmpty(2),
                    description = description,
                    startMillis = c.getLong(4),
                    endMillis = if (c.isNull(5)) 0L else c.getLong(5),
                    timeZone = c.stringOrEmpty(6),
                    appPackage = c.stringOrEmpty(7),
                    appUri = appUri
                )
            }
        }
        return result
    }

    private fun eventValues(
        calendarId: Long,
        key: String,
        event: ClassEvent,
        hideLecturer: Boolean
    ): ContentValues = ContentValues().apply {
        put(Events.CALENDAR_ID, calendarId)
        put(Events.TITLE, context.getString(R.string.calendar_event_title, event.subject.name, event.classType.polishLabel))
        put(Events.EVENT_LOCATION, location(event))
        put(Events.DESCRIPTION, description(key, event, hideLecturer))
        put(Events.DTSTART, event.timeSlot.startEpochMillis)
        put(Events.DTEND, event.timeSlot.endEpochMillis)
        put(Events.EVENT_TIMEZONE, TIME_ZONE)
        put(Events.CUSTOM_APP_PACKAGE, context.packageName)
        put(Events.CUSTOM_APP_URI, APP_URI_PREFIX + key)
    }

    private fun location(event: ClassEvent): String =
        listOfNotNull(event.room.displayLabel, event.room.addressLabel)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    private fun description(key: String, event: ClassEvent, hideLecturer: Boolean): String {
        val lines = mutableListOf<String>()
        if (!hideLecturer && event.lecturer.hasName) {
            lines += context.getString(R.string.calendar_event_lecturer, event.lecturer.fullDisplayName)
        }
        if (event.groupCode.isNotBlank()) {
            lines += context.getString(R.string.calendar_event_group, event.groupCode)
        }
        lines += context.getString(R.string.calendar_event_mode, event.deliveryMode.polishBadge)
        event.incidentAlert?.takeIf { it.isNotBlank() }?.let {
            lines += context.getString(R.string.calendar_event_alert, it)
        }
        lines += ""
        lines += context.getString(R.string.calendar_event_footer) + " $MARKER_PREFIX$key]"
        return lines.joinToString("\n")
    }

    private fun applyInBatches(ops: List<ContentProviderOperation>) {
        // Small batches keep every Binder transaction well below the 1 MB limit.
        ops.chunked(BATCH_SIZE).forEach { chunk ->
            resolver.applyBatch(CalendarContract.AUTHORITY, ArrayList(chunk))
        }
    }

    /** Runs [block] on the IO dispatcher and maps platform errors to Polish messages. */
    private suspend fun <T> calendarCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CalendarSyncException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(CalendarSyncException(context.getString(R.string.calendar_error_permission), e))
        } catch (e: Exception) {
            Result.failure(CalendarSyncException(context.getString(R.string.calendar_error_generic), e))
        }
    }

    private fun eventUri(id: Long) = ContentUris.withAppendedId(Events.CONTENT_URI, id)

    private fun startOfDay(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(WARSAW).toLocalDate().atStartOfDay(WARSAW).toInstant().toEpochMilli()

    private fun endOfDay(epochMillis: Long): Long =
        Instant.ofEpochMilli(epochMillis).atZone(WARSAW).toLocalDate().plusDays(1)
            .atStartOfDay(WARSAW).toInstant().toEpochMilli() - 1

    private fun Cursor.stringOrEmpty(index: Int): String = if (isNull(index)) "" else getString(index).orEmpty()

    private class ExistingEvent(
        val id: Long,
        val key: String,
        val title: String,
        val location: String,
        val description: String,
        val startMillis: Long,
        val endMillis: Long,
        val timeZone: String,
        val appPackage: String,
        val appUri: String
    ) {
        fun matches(values: ContentValues): Boolean =
            title == values.getAsString(Events.TITLE) &&
                location == values.getAsString(Events.EVENT_LOCATION) &&
                description == values.getAsString(Events.DESCRIPTION) &&
                startMillis == values.getAsLong(Events.DTSTART) &&
                endMillis == values.getAsLong(Events.DTEND) &&
                timeZone == values.getAsString(Events.EVENT_TIMEZONE) &&
                appPackage == values.getAsString(Events.CUSTOM_APP_PACKAGE) &&
                appUri == values.getAsString(Events.CUSTOM_APP_URI)
    }

    private companion object {
        const val TIME_ZONE = "Europe/Warsaw"
        val WARSAW: ZoneId = ZoneId.of(TIME_ZONE)
        const val APP_URI_PREFIX = "taal://class/"
        const val MARKER_PREFIX = "[taal:"
        const val BATCH_SIZE = 100
        val KEY_REGEX = Regex("[0-9a-f]{20}")
        val MARKER_REGEX = Regex("\\[taal:([0-9a-f]{20})]")

        /** Deterministic 20-hex-char key of a class; stable across syncs as long as its id is. */
        fun syncKey(eventId: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(eventId.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
                .take(20)
    }
}
