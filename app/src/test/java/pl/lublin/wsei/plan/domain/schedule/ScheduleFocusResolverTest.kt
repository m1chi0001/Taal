package pl.lublin.wsei.plan.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.Subject
import pl.lublin.wsei.plan.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime

/** 2026-11-20 is a Friday, 11-21 Saturday, 11-22 Sunday; the next zjazd is 11-27 … 11-29. */
class ScheduleFocusResolverTest {

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
    private val satB = event("satB", "2026-11-21", "09:45", "11:15")
    private val sunA = event("sunA", "2026-11-22", "08:00", "09:30")
    private val nextFri = event("nextFri", "2026-11-27", "15:00", "16:30")
    private val nextSat = event("nextSat", "2026-11-28", "08:00", "09:30")

    private val all = listOf(nextSat, sunA, satB, friB, nextFri, satA, friA) // deliberately unsorted

    private fun resolve(events: List<ClassEvent>, date: String, time: String) =
        ScheduleFocusResolver.resolve(events, millis(date, time), zone)

    @Test
    fun `active class mid-day`() {
        val focus = resolve(all, "2026-11-21", "08:30")
        assertEquals(ScheduleFocusKind.TODAY, focus.kind)
        assertEquals("2026-11-21", focus.dateIso)
        assertEquals(listOf(satA, satB), focus.events)
        assertSame(satA, focus.activeEvent)
        assertSame(satB, focus.nextEvent)
    }

    @Test
    fun `break between classes stays on today`() {
        val focus = resolve(all, "2026-11-21", "09:35")
        assertEquals(ScheduleFocusKind.TODAY, focus.kind)
        assertNull(focus.activeEvent)
        assertSame(satB, focus.nextEvent)
    }

    @Test
    fun `before the first class of the day`() {
        val focus = resolve(all, "2026-11-21", "06:00")
        assertEquals(ScheduleFocusKind.TODAY, focus.kind)
        assertNull(focus.activeEvent)
        assertSame(satA, focus.nextEvent)
    }

    @Test
    fun `Saturday after classes with Sunday classes is tomorrow`() {
        val focus = resolve(all, "2026-11-21", "11:15")
        assertEquals(ScheduleFocusKind.TOMORROW, focus.kind)
        assertEquals("2026-11-22", focus.dateIso)
        assertEquals(listOf(sunA), focus.events)
        assertNull(focus.activeEvent)
        assertSame(sunA, focus.nextEvent)
    }

    @Test
    fun `after the last class of the zjazd the next zjazd is shown`() {
        val focus = resolve(all, "2026-11-22", "20:00")
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals("2026-11-27", focus.dateIso)
        assertSame(nextFri, focus.nextEvent)
    }

    @Test
    fun `Friday after classes with Sunday classes is later this zjazd`() {
        val events = listOf(friA, friB, sunA)
        val focus = resolve(events, "2026-11-20", "19:00")
        assertEquals(ScheduleFocusKind.LATER_THIS_ZJAZD, focus.kind)
        assertEquals("2026-11-22", focus.dateIso)
    }

    @Test
    fun `empty Saturday between Friday and Sunday classes is tomorrow, gap from Friday counts`() {
        val events = listOf(friA, sunA, nextFri)
        assertEquals(ScheduleFocusKind.TOMORROW, resolve(events, "2026-11-21", "12:00").kind)
    }

    @Test
    fun `Monday after Friday-only zjazd is not the same zjazd`() {
        val monday = event("mon", "2026-11-23", "08:00", "09:00")
        val focus = resolve(listOf(friA, monday), "2026-11-21", "12:00")
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals("2026-11-23", focus.dateIso)
    }

    @Test
    fun `empty weekday before a weekend zjazd is next zjazd`() {
        // Wednesday 2026-11-25, nothing until Friday 11-27.
        val focus = resolve(all, "2026-11-25", "10:00")
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals("2026-11-27", focus.dateIso)
        assertSame(nextFri, focus.nextEvent)
    }

    @Test
    fun `zjazd starting on Saturday`() {
        val focus = resolve(listOf(nextSat), "2026-11-25", "10:00")
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals("2026-11-28", focus.dateIso)
    }

    @Test
    fun `Thursday before a Friday zjazd is tomorrow`() {
        val focus = resolve(all, "2026-11-26", "21:00")
        assertEquals(ScheduleFocusKind.TOMORROW, focus.kind)
        assertEquals("2026-11-27", focus.dateIso)
    }

    @Test
    fun `no events and no upcoming events`() {
        assertEquals(ScheduleFocus.None, resolve(emptyList(), "2026-11-22", "10:00"))
        assertEquals(ScheduleFocus.None, resolve(listOf(friA, satA), "2026-11-22", "10:00"))
    }

    @Test
    fun `class ending exactly now is over`() {
        val focus = resolve(listOf(sunA, nextFri), "2026-11-22", "09:30")
        assertEquals(ScheduleFocusKind.NEXT_ZJAZD, focus.kind)
        assertEquals("2026-11-27", focus.dateIso)
    }
}
