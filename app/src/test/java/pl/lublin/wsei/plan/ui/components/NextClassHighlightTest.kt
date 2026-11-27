package pl.lublin.wsei.plan.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.Subject
import pl.lublin.wsei.plan.domain.model.TimeSlot
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
import pl.lublin.wsei.plan.ui.navigation.predictiveBackEasing
import java.time.LocalDate
import java.time.LocalTime

class NextClassHighlightTest {

    private val zone = ScheduleFocusResolver.WARSAW

    private fun millis(date: String, time: String): Long =
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone).toInstant().toEpochMilli()

    private fun event(id: String, date: String, start: String, end: String) = ClassEvent(
        id = id,
        dateIso = date,
        subject = Subject(id = id, name = "Subject $id", shortCode = id),
        classType = ClassType.LECTURE,
        lecturer = Lecturer(id = "1", academicTitle = "dr", firstName = "Jan", lastName = "Kowalski"),
        room = Room(number = "208"),
        timeSlot = TimeSlot(millis(date, start), millis(date, end), start, end),
        groupCode = "W/1/I/N"
    )

    @Test
    fun `does not highlight class as Next days ahead of an upcoming zjazd`() {
        val dec4Class = event("dec4", "2026-12-04", "15:20", "16:50")
        val now = millis("2026-11-27", "13:45")
        assertFalse(shouldHighlightAsNextClass(dec4Class, now, todayIso = "2026-11-27"))
    }

    @Test
    fun `does not highlight tomorrow morning class more than an hour before start`() {
        val morningClass = event("sat8", "2026-12-05", "08:00", "09:30")
        val prevEvening = millis("2026-12-04", "21:00")
        assertFalse(shouldHighlightAsNextClass(morningClass, prevEvening, todayIso = "2026-12-04"))
    }

    @Test
    fun `highlights first upcoming class on the day of classes`() {
        val dec4First = event("dec4_1", "2026-12-04", "15:20", "16:50")
        val morningOfZjazd = millis("2026-12-04", "08:30")
        assertTrue(shouldHighlightAsNextClass(dec4First, morningOfZjazd, todayIso = "2026-12-04"))
    }

    @Test
    fun `highlights class within one hour lead window even across midnight`() {
        val midnightClass = event("early", "2026-12-05", "00:30", "02:00")
        val fortyFiveMinBefore = millis("2026-12-04", "23:45")
        assertTrue(shouldHighlightAsNextClass(midnightClass, fortyFiveMinBefore, todayIso = "2026-12-04"))
    }

    @Test
    fun `does not highlight once class has started or finished`() {
        val dec4First = event("dec4_1", "2026-12-04", "15:20", "16:50")
        assertFalse(shouldHighlightAsNextClass(dec4First, millis("2026-12-04", "15:20"), todayIso = "2026-12-04"))
        assertFalse(shouldHighlightAsNextClass(dec4First, millis("2026-12-04", "16:00"), todayIso = "2026-12-04"))
        assertFalse(shouldHighlightAsNextClass(dec4First, millis("2026-12-04", "17:00"), todayIso = "2026-12-04"))
    }

    @Test
    fun `predictiveBackEasing is bounded and monotonically increasing`() {
        assertEquals(0f, predictiveBackEasing(0f), 1e-5f)
        assertEquals(1f, predictiveBackEasing(1f), 1e-5f)
        assertEquals(0f, predictiveBackEasing(-0.5f), 1e-5f)
        assertEquals(1f, predictiveBackEasing(1.5f), 1e-5f)

        var prev = 0f
        for (step in 1..20) {
            val current = predictiveBackEasing(step / 20f)
            assertTrue("Expected monotonic increase at step $step", current > prev)
            prev = current
        }
    }
}
