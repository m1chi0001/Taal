package pl.lublin.wsei.plan.data.remote.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import java.time.LocalDate

/**
 * Synthetic fixtures modelled on typical WebForms GridView output. Replace/extend them with a saved
 * copy of the real timetable page (File → Save page) once you have one.
 */
class TimetableHtmlParserTest {

    private val rowspanTable = """
        <html><body><form id="aspnetForm">
        <table class="layout"><tr><td>
          <table id="ctl00_ContentPlaceHolder1_gvPlan">
            <tr><th>Data</th><th>Od</th><th>Do</th><th>Przedmiot</th><th>Forma</th>
                <th>Prowadzący</th><th>Sala</th><th>Grupa</th><th>Uwagi</th></tr>
            <tr><td rowspan="2">Sob 14.11.2026</td><td>08:00</td><td>09:30</td>
                <td>Programowanie obiektowe</td><td>Wykład</td><td>dr inż. Jan Kowalski</td>
                <td>AULA A</td><td>KI/N 1</td><td>&nbsp;</td></tr>
            <tr><td>09:45</td><td>11:15</td><td>Bazy danych</td><td>Lab</td>
                <td>mgr Anna Nowak-Zielińska</td><td></td><td>Lab4/1</td><td>Teams</td></tr>
            <tr><td>2026-11-15</td><td>12:00</td><td>13:30</td><td>Matematyka dyskretna</td><td>Ćwiczenia</td>
                <td>prof. dr hab. Piotr Wiśniewski</td><td>C209K</td><td>Cw1</td><td></td></tr>
          </table>
        </td></tr></table>
        </form></body></html>
    """.trimIndent()

    @Test
    fun `parses rowspan dates, separate Od-Do columns and class types`() {
        val events = TimetableHtmlParser.parse(rowspanTable, "https://example.invalid/")
        assertEquals(3, events.size)

        val first = events[0]
        assertEquals("2026-11-14", first.dateIso)
        assertEquals("08:00", first.timeSlot.startFormatted)
        assertEquals("09:30", first.timeSlot.endFormatted)
        assertEquals("Programowanie obiektowe", first.subject.name)
        assertEquals(ClassType.LECTURE, first.classType)
        assertEquals("dr inż.", first.lecturer.academicTitle)
        assertEquals("Kowalski", first.lecturer.lastName)
        assertEquals("AULA A", first.room.number)
        assertEquals(DeliveryMode.ON_SITE, first.deliveryMode)
        assertEquals(DataSourceOrigin.HTML_FALLBACK, first.origin)

        val second = events[1]
        assertEquals("2026-11-14", second.dateIso) // inherited via rowspan
        assertEquals(ClassType.LAB, second.classType)
        assertEquals(DeliveryMode.REMOTE, second.deliveryMode)
        assertEquals("Nowak-Zielińska", second.lecturer.lastName)

        val third = events[2]
        assertEquals("2026-11-15", third.dateIso)
        assertEquals(ClassType.EXERCISE, third.classType)
        assertEquals("prof. dr hab.", third.lecturer.academicTitle)
    }

    private val daySeparatorTable = """
        <table>
          <tr><th>Godziny</th><th>Przedmiot</th><th>Forma zajęć</th><th>Wykładowca</th><th>Sala</th></tr>
          <tr><td colspan="5">Wtorek, 10.11.2026</td></tr>
          <tr><td>8.00 - 9.30</td><td>Sieci komputerowe</td><td>Lab</td><td>mgr Ewa Lis</td><td>205K</td></tr>
          <tr><td colspan="5">Środa, 11.11.2026</td></tr>
          <tr><td>16:00-17:30</td><td>Etyka</td><td>Elrn</td><td>dr Adam Kos</td><td></td></tr>
        </table>
    """.trimIndent()

    @Test
    fun `parses day separator rows and combined time range column`() {
        val events = TimetableHtmlParser.parse(daySeparatorTable, "https://example.invalid/")
        assertEquals(2, events.size)
        assertEquals("2026-11-10", events[0].dateIso)
        assertEquals("08:00", events[0].timeSlot.startFormatted)
        assertEquals("09:30", events[0].timeSlot.endFormatted)
        assertEquals("2026-11-11", events[1].dateIso)
        assertEquals(DeliveryMode.ELEARNING, events[1].deliveryMode)
        assertEquals(ClassType.ELEARNING, events[1].classType)
    }

    @Test
    fun `filters by requested date range`() {
        val events = TimetableHtmlParser.parse(
            daySeparatorTable, "https://example.invalid/",
            from = LocalDate.of(2026, 11, 11), to = LocalDate.of(2026, 11, 11)
        )
        assertEquals(1, events.size)
        assertEquals("Etyka", events.single().subject.name)
    }

    @Test
    fun `ignores tables that are not timetables`() {
        val html = "<table><tr><th>Nazwa</th><th>Kwota</th></tr><tr><td>Czesne</td><td>500 PLN</td></tr></table>"
        assertTrue(TimetableHtmlParser.parse(html, "https://example.invalid/").isEmpty())
    }

    @Test
    fun `ids are stable between parses`() {
        val a = TimetableHtmlParser.parse(rowspanTable, "https://example.invalid/").map { it.id }
        val b = TimetableHtmlParser.parse(rowspanTable, "https://example.invalid/").map { it.id }
        assertEquals(a, b)
        assertNull(a.groupBy { it }.values.firstOrNull { it.size > 1 })
    }
}
