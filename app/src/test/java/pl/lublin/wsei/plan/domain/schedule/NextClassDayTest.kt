package pl.lublin.wsei.plan.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.Subject
import pl.lublin.wsei.plan.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime

/** 2026-11-20 is a Friday; zjazdy are 11-20 … 11-22 and 11-27 … 11-28, then 12-04. */
class NextClassDayTest {

    private val zone = ScheduleFocusResolver.WARSAW

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun event(id: String, date: String, start: String, end: String) = ClassEvent(
        id = id,
        dateIso = date,
        subject = Subject(id = id, name = "Subject $id", shortCode = id),
        classType = ClassType.LECTURE,
        lecturer = Lecturer(id = "1", academicTitle = "dr", firstName = "Jan", lastName = "Kowalski"),
        room = Room(number = "101"),
        timeSlot = TimeSlot(millis(date, start), millis(date, end), start, end),
        groupCode = "W/1/I/N"
    )

    private val friA = event("friA", "2026-11-20", "15:00", "16:30")
    private val friB = event("friB", "2026-11-20", "16:45", "18:15")
    private val satA = event("satA", "2026-11-21", "08:00", "09:30")
    private val sunA = event("sunA", "2026-11-22", "08:00", "09:30")
    private val nextFri = event("nextFri", "2026-11-27", "15:00", "16:30")
    private val nextSat = event("nextSat", "2026-11-28", "08:00", "09:30")
    private val decFriA = event("decFriA", "2026-12-04", "15:20", "16:50")
    private val decFriB = event("decFriB", "2026-12-04", "17:00", "18:30")
    private val decFriC = event("decFriC", "2026-12-04", "18:40", "20:10")

    private val all = listOf(decFriC, nextSat, sunA, friB, decFriA, nextFri, satA, friA, decFriB) // unsorted

    private fun next(events: List<ClassEvent>, after: String, nowDate: String, nowTime: String = "12:00") =
        ScheduleFocusResolver.nextClassDayAfter(events, after, millis(nowDate, nowTime), zone)

    @Test
    fun `free weekday points to the next zjazd with countdown from today`() {
        val day = next(all, after = "2026-11-23", nowDate = "2026-11-23")
        assertNotNull(day)
        day!!
        assertEquals("2026-11-27", day.dateIso)
        assertEquals(4, day.daysFromToday)
        assertEquals(nextFri.timeSlot.startEpochMillis, day.firstStartMillis)
        assertEquals("15:00", day.firstStartLabel)
        assertEquals(1, day.blockCount)
        assertTrue(day.startsZjazd)
    }

    @Test
    fun `first start and block count come from the sorted day`() {
        val day = next(all, after = "2026-11-29", nowDate = "2026-11-23")!!
        assertEquals("2026-12-04", day.dateIso)
        assertEquals(11, day.daysFromToday)
        assertEquals(decFriA.timeSlot.startEpochMillis, day.firstStartMillis)
        assertEquals("15:20", day.firstStartLabel)
        assertEquals(3, day.blockCount)
        assertTrue(day.startsZjazd)
    }

    @Test
    fun `viewed day in the past still returns the next class day after it`() {
        val day = next(all, after = "2026-11-18", nowDate = "2026-11-23")!!
        assertEquals("2026-11-20", day.dateIso)
        assertEquals(-3, day.daysFromToday)
        assertEquals(2, day.blockCount)
        assertEquals(friA.timeSlot.startEpochMillis, day.firstStartMillis)
    }

    @Test
    fun `later day of the same zjazd does not start a zjazd`() {
        val sunday = next(all, after = "2026-11-21", nowDate = "2026-11-21")!!
        assertEquals("2026-11-22", sunday.dateIso)
        assertEquals(1, sunday.daysFromToday)
        assertFalse(sunday.startsZjazd)

        val saturday = next(all, after = "2026-11-27", nowDate = "2026-11-23")!!
        assertEquals("2026-11-28", saturday.dateIso)
        assertFalse(saturday.startsZjazd)
    }

    @Test
    fun `next class day can be today when browsing the past`() {
        val day = next(all, after = "2026-11-26", nowDate = "2026-11-27", nowTime = "09:00")!!
        assertEquals("2026-11-27", day.dateIso)
        assertEquals(0, day.daysFromToday)
    }

    @Test
    fun `nothing after the viewed day returns null`() {
        assertNull(next(all, after = "2026-12-04", nowDate = "2026-11-23"))
        assertNull(next(emptyList(), after = "2026-11-23", nowDate = "2026-11-23"))
    }

    @Test
    fun `malformed viewed date returns null`() {
        assertNull(next(all, after = "not-a-date", nowDate = "2026-11-23"))
    }

    @Test
    fun `countdown uses the Warsaw date of now`() {
        // 00:30 in Warsaw on Monday is still Sunday in UTC.
        val day = next(all, after = "2026-11-23", nowDate = "2026-11-23", nowTime = "00:30")!!
        assertEquals(4, day.daysFromToday)
    }

    @Test
    fun `matches the focus day once today is over`() {
        val now = millis("2026-11-22", "12:00")
        val focus = ScheduleFocusResolver.resolve(all, now, zone)
        val day = ScheduleFocusResolver.nextClassDayAfter(all, "2026-11-22", now, zone)!!
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals(focus.dateIso, day.dateIso)
        assertEquals(focus.events.size, day.blockCount)
        assertEquals(5, day.daysFromToday)
    }

    @Test
    fun `nearestClassDayFromNow always returns nearest upcoming zjazd from today regardless of later zjazdy`() {
        val dec19 = event("dec19", "2026-12-19", "08:00", "09:30")
        val schedule = listOf(friA, sunA, decFriB, dec19, decFriA, decFriC)
        // Today is Friday 2026-11-27 (free day); upcoming zjazdy are 2026-12-04 and 2026-12-19.
        // Even when the user browses 2026-12-11 or 2026-12-25 in the calendar, nearestClassDayFromNow
        // resolves strictly from today -> 2026-12-04 (in 7 days), never skipping to 2026-12-19.
        val nearest = ScheduleFocusResolver.nearestClassDayFromNow(
            events = schedule,
            nowEpochMillis = millis("2026-11-27", "13:42"),
            zone = zone
        )
        assertNotNull(nearest)
        nearest!!
        assertEquals("2026-12-04", nearest.dateIso)
        assertEquals(7, nearest.daysFromToday)
        assertEquals("15:20", nearest.firstStartLabel)
        assertEquals(3, nearest.blockCount)
        assertTrue(nearest.startsZjazd)
    }

    @Test
    fun `nearestClassDayFromNow returns today while classes remain today and advances once finished`() {
        // Before today's class ends (15:00-16:30 on 2026-11-27): points to today (daysFromToday = 0).
        val beforeStart = ScheduleFocusResolver.nearestClassDayFromNow(
            events = all,
            nowEpochMillis = millis("2026-11-27", "10:00"),
            zone = zone
        )!!
        assertEquals("2026-11-27", beforeStart.dateIso)
        assertEquals(0, beforeStart.daysFromToday)
        assertTrue(beforeStart.startsZjazd)

        val duringClass = ScheduleFocusResolver.nearestClassDayFromNow(
            events = all,
            nowEpochMillis = millis("2026-11-27", "15:30"),
            zone = zone
        )!!
        assertEquals("2026-11-27", duringClass.dateIso)
        assertEquals(0, duringClass.daysFromToday)

        // Once today's last class ends at 16:30, advances to Saturday 2026-11-28 (same zjazd).
        val afterTodayDone = ScheduleFocusResolver.nearestClassDayFromNow(
            events = all,
            nowEpochMillis = millis("2026-11-27", "16:31"),
            zone = zone
        )!!
        assertEquals("2026-11-28", afterTodayDone.dateIso)
        assertEquals(1, afterTodayDone.daysFromToday)
        assertFalse(afterTodayDone.startsZjazd)
    }

    @Test
    fun `nearestClassDayFromNow returns null when no classes remain`() {
        assertNull(
            ScheduleFocusResolver.nearestClassDayFromNow(
                events = all,
                nowEpochMillis = millis("2026-12-04", "20:15"),
                zone = zone
            )
        )
        assertNull(
            ScheduleFocusResolver.nearestClassDayFromNow(
                events = emptyList(),
                nowEpochMillis = millis("2026-11-27", "12:00"),
                zone = zone
            )
        )
    }
}
