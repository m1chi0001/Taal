package pl.lublin.wsei.plan.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.TimeSlot

class ScheduleNormalizerLecturerTest {

    private fun event(id: String, subject: String, type: ClassType, lecturer: String) = ClassEvent(
        id = id,
        dateIso = "2026-11-10",
        subject = ScheduleNormalizer.buildSubject(subject),
        classType = type,
        lecturer = ScheduleNormalizer.parseLecturer(lecturer, "l_$id"),
        room = Room("205K"),
        timeSlot = TimeSlot(0L, 5_400_000L, "08:00", "09:30"),
        groupCode = "G1"
    )

    @Test
    fun `fills missing lecturer from same subject and form first`() {
        val events = listOf(
            event("1", "Bazy danych", ClassType.LAB, "dr inż. Jan Kowalski"),
            event("2", "Bazy danych", ClassType.LECTURE, "prof. Anna Nowak"),
            event("3", "Bazy danych", ClassType.LAB, "")
        )
        val filled = ScheduleNormalizer.fillMissingLecturers(events)
        assertEquals("Kowalski", filled[2].lecturer.lastName)
    }

    @Test
    fun `falls back to subject data and ignores differences in case and spacing`() {
        val events = listOf(event("1", "Sieci  komputerowe", ClassType.LECTURE, ""))
        val subjectLecturers = mapOf(ScheduleNormalizer.subjectKey("sieci komputerowe") to "dr Piotr Wiśniewski")
        val filled = ScheduleNormalizer.fillMissingLecturers(events, subjectLecturers)
        assertEquals("dr", filled.single().lecturer.academicTitle)
        assertEquals("Wiśniewski", filled.single().lecturer.lastName)
    }

    @Test
    fun `leaves event untouched when nothing is known`() {
        val filled = ScheduleNormalizer.fillMissingLecturers(listOf(event("1", "Etyka", ClassType.LECTURE, "")))
        assertFalse(filled.single().lecturer.hasName)
        assertEquals("Brak prowadzącego", filled.single().lecturer.displayNameOrPlaceholder)
    }

    @Test
    fun `initials are not treated as academic title`() {
        val lecturer: Lecturer = ScheduleNormalizer.parseLecturer("J. Kowalski", "x")
        assertEquals("", lecturer.academicTitle)
        assertTrue(lecturer.fullDisplayName.contains("Kowalski"))
    }
}
