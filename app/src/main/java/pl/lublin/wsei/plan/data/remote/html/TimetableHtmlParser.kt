package pl.lublin.wsei.plan.data.remote.html

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import pl.lublin.wsei.plan.data.remote.ScheduleNormalizer
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.TimeSlot
import java.time.LocalDate

/**
 * Parses timetable tables from the web Wirtualny Dziekanat into [ClassEvent] (`Lesson`) models.
 *
 * It does not depend on element ids/classes (those differ between APR versions). Instead it finds every
 * `<table>` whose header row names enough known columns (Data, Od, Do, Godziny, Przedmiot, Forma,
 * Prowadzący, Sala, Grupa, Uwagi) and maps cells by header text. It understands:
 * - `rowspan`/`colspan` (a date cell spanning all lessons of that day),
 * - "day separator" rows (a single wide cell with the date, followed by lesson rows),
 * - separate Od/Do columns or one "08:00 - 09:30" column,
 * - dates as `2026-11-10`, `10.11.2026`, `Wt, 10.11.2026`.
 */
object TimetableHtmlParser {

    enum class Column { DATE, FROM, TO, TIME_RANGE, SUBJECT, FORM, LECTURER, ROOM, GROUP, NOTES }

    private fun header(vararg alternatives: String) =
        Regex("^(?:${alternatives.joinToString("|")})(?=$|[\\s.:/(-])")

    private val HEADER_SYNONYMS: List<Pair<Column, Regex>> = listOf(
        Column.FROM to header("od", "godz\\.? ?od", "godzina od", "początek", "poczatek", "start"),
        Column.TO to header("do", "godz\\.? ?do", "godzina do", "koniec"),
        Column.TIME_RANGE to header("godziny", "godz\\.?", "godzina", "czas", "w godz\\.?"),
        Column.SUBJECT to header("przedmiot", "nazwa przedmiotu", "zajęcia", "zajecia", "nazwa", "kurs"),
        Column.FORM to header("forma", "forma zajęć", "forma zajec", "typ", "rodzaj", "f\\. ?zaj\\.?"),
        Column.LECTURER to header("prowadzący", "prowadzacy", "wykładowca", "wykladowca", "nauczyciel", "osoba prowadząca"),
        Column.ROOM to header("sala", "miejsce", "lokalizacja", "budynek", "s\\."),
        Column.GROUP to header("grupa", "grupy", "gr\\."),
        Column.NOTES to header("uwagi", "komentarz", "informacje", "opis")
    )
    private val DATE_PRIMARY = header("data", "data zajęć", "data zajec")
    private val DATE_SECONDARY = header("dzień", "dzien", "termin")

    fun parse(html: String, baseUri: String, from: LocalDate? = null, to: LocalDate? = null): List<ClassEvent> {
        val document = Jsoup.parse(html, baseUri)
        return document.select("table")
            .filter { it.select("table").size == 1 } // innermost tables only (select() includes the element itself)
            .flatMap { parseTable(it) }
            .filter { event ->
                val day = LocalDate.parse(event.dateIso)
                (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to))
            }
            .distinctBy { it.id }
            .sortedBy { it.timeSlot.startEpochMillis }
    }

    private fun parseTable(table: Element): List<ClassEvent> {
        val grid = expandGrid(table)
        val headerIndex = grid.indexOfFirst { row -> mapHeader(row).let { it.containsKey(Column.SUBJECT) && it.size >= 3 } }
        if (headerIndex < 0) return emptyList()
        val columns = mapHeader(grid[headerIndex])
        val width = grid[headerIndex].size

        val events = mutableListOf<ClassEvent>()
        var currentDate: String? = null
        for ((rowIndex, row) in grid.withIndex()) {
            if (rowIndex <= headerIndex) continue
            val distinct = row.map(String::trim).filter(String::isNotEmpty).distinct()
            // Day separator row: one meaningful cell (often colspan=all) containing a date.
            if (distinct.size == 1 && (row.size < width / 2 + 1 || row.distinct().size == 1)) {
                ScheduleNormalizer.normalizeIsoDate(distinct[0])?.let { currentDate = it }
                continue
            }
            if (distinct.isEmpty()) continue
            fun cell(c: Column): String = columns[c]?.let { row.getOrNull(it) }?.trim().orEmpty()

            val rowDate = ScheduleNormalizer.normalizeIsoDate(cell(Column.DATE))
            if (rowDate != null) currentDate = rowDate
            val date = rowDate ?: currentDate ?: continue

            val range = ScheduleNormalizer.extractTimeRange(cell(Column.TIME_RANGE))
                ?: ScheduleNormalizer.extractTimeRange(cell(Column.FROM))
            val start = range?.first ?: ScheduleNormalizer.extractTimeHhMm(cell(Column.FROM)) ?: continue
            val end = range?.second ?: ScheduleNormalizer.extractTimeHhMm(cell(Column.TO))
            val subjectName = cell(Column.SUBJECT)
            if (subjectName.isBlank()) continue

            val startMillis = ScheduleNormalizer.toEpochMillis(date, start) ?: continue
            val endMillis = end?.let { ScheduleNormalizer.toEpochMillis(date, it) }?.takeIf { it > startMillis }
                ?: (startMillis + 90 * 60_000L)
            val endFormatted = java.time.Instant.ofEpochMilli(endMillis)
                .atZone(ScheduleNormalizer.WARSAW_ZONE).toLocalTime().toString().take(5)

            val form = cell(Column.FORM)
            val notes = cell(Column.NOTES)
            val group = cell(Column.GROUP)
            val placement = ScheduleNormalizer.resolvePlacement(cell(Column.ROOM), form, "$notes $subjectName")
            val lecturerRaw = cell(Column.LECTURER)

            events += ClassEvent(
                id = "html_${date}_${start}_${(subjectName + group + form).hashCode().toUInt().toString(16)}",
                dateIso = date,
                subject = ScheduleNormalizer.buildSubject(subjectName),
                classType = ClassType.fromRawCode(form),
                lecturer = ScheduleNormalizer.parseLecturer(lecturerRaw, "html_" + lecturerRaw.hashCode().toUInt().toString(16)),
                room = placement.room,
                timeSlot = TimeSlot(startMillis, endMillis, start, endFormatted),
                groupCode = group.ifBlank { "Grupa dziekanatowa" },
                deliveryMode = placement.deliveryMode,
                notes = notes.ifBlank { null },
                origin = DataSourceOrigin.HTML_FALLBACK
            )
        }
        return events
    }

    private fun mapHeader(row: List<String>): Map<Column, Int> {
        val result = linkedMapOf<Column, Int>()
        val normalized = row.map { it.trim().lowercase().trimEnd(':') }
        (normalized.indexOfFirst { DATE_PRIMARY.containsMatchIn(it) }.takeIf { it >= 0 }
            ?: normalized.indexOfFirst { DATE_SECONDARY.containsMatchIn(it) }.takeIf { it >= 0 })
            ?.let { result[Column.DATE] = it }
        normalized.forEachIndexed { index, text ->
            if (text.isEmpty() || index == result[Column.DATE]) return@forEachIndexed
            HEADER_SYNONYMS.firstOrNull { (column, regex) -> column !in result && regex.containsMatchIn(text) }
                ?.let { (column, _) -> result[column] = index }
        }
        return result
    }

    /** Builds a rectangular text grid honouring rowspan/colspan. */
    internal fun expandGrid(table: Element): List<List<String>> {
        val rows = table.select("tr").filter { it.closest("table") == table }
        val grid = mutableListOf<MutableList<String?>>()
        rows.forEachIndexed { r, tr ->
            while (grid.size <= r) grid += mutableListOf<String?>()
            var c = 0
            for (cell in tr.children().filter { it.tagName() == "td" || it.tagName() == "th" }) {
                while (c < grid[r].size && grid[r][c] != null) c++
                val text = cell.wholeText().replace(' ', ' ').replace(Regex("\\s+"), " ").trim()
                val rowSpan = cell.attr("rowspan").toIntOrNull()?.coerceIn(1, 100) ?: 1
                val colSpan = cell.attr("colspan").toIntOrNull()?.coerceIn(1, 50) ?: 1
                for (dr in 0 until rowSpan) {
                    val rr = r + dr
                    while (grid.size <= rr) grid += mutableListOf<String?>()
                    for (dc in 0 until colSpan) {
                        val cc = c + dc
                        while (grid[rr].size <= cc) grid[rr] += null
                        grid[rr][cc] = text
                    }
                }
                c += colSpan
            }
        }
        return grid.map { row -> row.map { it.orEmpty() } }
    }
}
