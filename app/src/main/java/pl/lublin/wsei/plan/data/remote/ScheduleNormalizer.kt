package pl.lublin.wsei.plan.data.remote

import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.Subject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Normalisation shared by the REST client and the HTML fallback, so both sources produce identical
 * domain models (same ids, same room/remote rules, same lecturer split).
 */
object ScheduleNormalizer {

    val WARSAW_ZONE: ZoneId = ZoneId.of("Europe/Warsaw")

    private val ISO_DATE = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
    private val PL_DATE = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{4})""")
    private val TIME = Regex("""(\d{1,2})[:.](\d{2})""")

    private val ACADEMIC_TITLES = setOf(
        "prof.", "prof", "dr", "dr.", "hab.", "hab", "inż.", "inż", "mgr", "mgr.", "lic.", "lic",
        "doc.", "doc", "n.", "med.", "uczelni", "wsei", "pwsz", "ks.", "płk", "mjr", "kpt.", "arch."
    )

    /** "2026-11-10T00:00:00", "2026-11-10", "10.11.2026", "Wt 10.11.2026" → "2026-11-10" (or null). */
    fun normalizeIsoDate(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val parsed = ISO_DATE.find(raw)?.let { m ->
            runCatching {
                LocalDate.of(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            }.getOrNull()
        } ?: PL_DATE.find(raw)?.let { m ->
            runCatching {
                LocalDate.of(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt())
            }.getOrNull()
        }
        return parsed?.toString()
    }

    /** "2026-11-10" → "10.11.2026"; unknown input is returned trimmed. */
    fun isoToPolishDate(raw: String?): String {
        val iso = normalizeIsoDate(raw) ?: return raw?.trim().orEmpty()
        val (y, m, d) = iso.split("-")
        return "$d.$m.$y"
    }

    /**
     * Extracts "HH:mm" from a C# `TimeSpan` ("08:00:00", "1.08:00:00"), a DateTime ("2026-11-10T08:00:00"),
     * or free text ("8.00", "godz. 8:00").
     */
    fun extractTimeHhMm(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val candidate = if (raw.contains('T')) raw.substringAfter('T') else raw.substringAfterLast(".", raw)
            .let { tail -> if (TIME.containsMatchIn(tail)) tail else raw }
        val m = TIME.find(candidate) ?: return null
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        if (h !in 0..23 || min !in 0..59) return null
        return "%02d:%02d".format(h, min)
    }

    /** "08:00 - 09:30", "8.00–9.30" → ("08:00", "09:30"). */
    fun extractTimeRange(raw: String?): Pair<String, String>? {
        if (raw.isNullOrBlank()) return null
        val times = TIME.findAll(raw).mapNotNull { extractTimeHhMm(it.value) }.toList()
        return if (times.size >= 2) times[0] to times[1] else null
    }

    fun toEpochMillis(dateIso: String, hhMm: String): Long? = runCatching {
        LocalDateTime.parse("${dateIso}T$hhMm:00").atZone(WARSAW_ZONE).toInstant().toEpochMilli()
    }.getOrNull()

    fun buildSubject(displayName: String): Subject = Subject(
        id = displayName.lowercase().trim().replace(Regex("\\s+"), "_"),
        name = displayName,
        shortCode = displayName.split(" ")
            .mapNotNull { it.firstOrNull()?.takeIf(Char::isLetter)?.uppercaseChar() }
            .joinToString("")
            .take(4)
    )

    /** "dr inż. Jan Kowalski" → title "dr inż.", first "Jan", last "Kowalski". */
    fun parseLecturer(raw: String, id: String): Lecturer {
        val tokens = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return Lecturer(id = id, academicTitle = "", firstName = "", lastName = "")
        val titleCount = tokens.takeWhile { it.lowercase() in ACADEMIC_TITLES || (it.endsWith(".") && it.length > 2) }.size
            .coerceAtMost((tokens.size - 1).coerceAtLeast(0))
        val title = tokens.take(titleCount).joinToString(" ")
        val names = tokens.drop(titleCount)
        return when (names.size) {
            0 -> Lecturer(id = id, academicTitle = "", firstName = title, lastName = "")
            1 -> Lecturer(id = id, academicTitle = title, firstName = "", lastName = names[0])
            else -> Lecturer(
                id = id,
                academicTitle = title,
                firstName = names.dropLast(1).joinToString(" "),
                lastName = names.last()
            )
        }
    }

    data class Placement(val room: Room, val deliveryMode: DeliveryMode)

    /**
     * Uniwersytet WSEI: on-site classes have a concrete room (AULA A, C209K, 205K); streaming classes have
     * an empty room or a Streaming/Teams/Zdalnie/Online note; e-learning is marked "Elrn"/"E-learning".
     */
    fun resolvePlacement(rawRoom: String, form: String, extraMeta: String): Placement {
        val cleanedRoom = rawRoom.trim()
            .removePrefix("Sala").removePrefix("sala").removePrefix("s.")
            .trim()
        val meta = "$cleanedRoom $form $extraMeta"
        val isElearning = listOf("Elrn", "E-learn", "Elearning", "Moodle").any { meta.contains(it, ignoreCase = true) }
        val isStreaming = !isElearning && (
            cleanedRoom.isBlank() || cleanedRoom == "-" || cleanedRoom.equals("Brak", ignoreCase = true) ||
                listOf("Stream", "Teams", "Zdaln", "Online", "Webinar", "Transmisja")
                    .any { meta.contains(it, ignoreCase = true) }
            )
        val mode = when {
            isElearning -> DeliveryMode.ELEARNING
            isStreaming -> DeliveryMode.REMOTE
            else -> DeliveryMode.ON_SITE
        }
        val room = when (mode) {
            DeliveryMode.ELEARNING -> Room("E-learning", "Platforma e-learningowa Uniwersytetu WSEI", isRemoteTeams = true)
            DeliveryMode.REMOTE -> Room("Streaming", "Zajęcia zdalne (Streaming)", isRemoteTeams = true)
            DeliveryMode.ON_SITE -> Room(cleanedRoom, "Kampus Uniwersytetu WSEI • Projektowa 4", isRemoteTeams = false)
        }
        return Placement(room, mode)
    }

    /** Strips HTML from announcement bodies and collapses whitespace. */
    fun stripHtml(raw: String): String = raw
        .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>"), "\n")
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        .replace(Regex("\\s*\\n\\s*"), "\n")
        .trim()
}
