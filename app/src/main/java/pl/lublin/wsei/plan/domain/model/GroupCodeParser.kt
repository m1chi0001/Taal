package pl.lublin.wsei.plan.domain.model

/** Kind of classes a dean's office student group is formed for. */
enum class GroupClassKind {
    LECTURE,
    EXERCISES,
    LAB,
    PROJECT,
    SEMINAR,
    ELEARNING,
    /** Professional practice ("Praktyki zawodowe", e.g. `PZZ1/1/I/N`). */
    PRACTICE,
    /**
     * Not a class group: the study track / year of study ("Tok", e.g. `ROK1/ N`).
     * Belongs to "Tok nauki", never to the class-group list.
     */
    STUDY_TRACK,
    OTHER
}

/** Result of [GroupCodeParser.parse]; [number] is null when the code carries no group number. */
data class ParsedGroupCode(
    val kind: GroupClassKind,
    val number: String?
)

/**
 * Pure parser for APR System group codes (`MsGroupDto.name`).
 *
 * The code's first `/`-separated segment holds the class kind and the group number, the remaining
 * segments describe the year, study level and mode (e.g. `Lab4/1/I/N` → lab group 4, year 1,
 * first-cycle, part-time). `MsGroupDto.editionName` is the *edition* of the group, not its number,
 * so it must never be shown as one.
 *
 * Handled forms: `W`, `Cw1`, `Ćw2`, `Lab4`, `L4`, `Lab 4`, `lab.4`, `Lab12`, `Proj2`,
 * `grupa laboratoryjna 4`, `Cw1a`, `Elrn1/1/I N` (space instead of a slash), `PZZ1/1/I/N`
 * (professional practice). A code without a number yields `number = null` — never a fake "1".
 * When the code does not reveal the kind, the group type (`MsGroupDto.type`, e.g. "Laboratorium")
 * is used as a hint.
 *
 * Study-track entries (`ROK1/ N`, or any entry whose type is "Tok") are [GroupClassKind.STUDY_TRACK]
 * and never carry a group number: the digit in `ROK1` is the year of study, not a group.
 */
object GroupCodeParser {

    /** 1–3 digit number (leading zeros dropped) with an optional one-letter suffix ("4", "12", "1A"). */
    private val NUMBER_REGEX = Regex("(?<![0-9])0*([0-9]{1,3})([A-Za-z])?(?![A-Za-z0-9])")
    private val WORD_SPLIT_REGEX = Regex("[^a-z]+")
    private val FILLER_WORDS = setOf("grupa", "grupy", "gr", "grp", "group", "nr")
    private val WHITESPACE_REGEX = Regex("\\s+")
    /** A single token like "Elrn1", "Lab4", "Cw1a". */
    private val KIND_AND_NUMBER_REGEX = Regex("[\\p{L}.]+[0-9]{1,3}[A-Za-z]?")

    fun parse(code: String, typeHint: String = ""): ParsedGroupCode {
        val segments = code.trim().split('/')
        val head = segments.first().trim().let { raw ->
            // "Elrn1 1/I/N": the kind+number token is complete, the rest belongs to the year segment.
            val first = raw.split(WHITESPACE_REGEX).first()
            if (first != raw && KIND_AND_NUMBER_REGEX.matches(first)) first else raw
        }

        val hintKind = kindOf(typeHint)
        val kind = when (hintKind) {
            // The dean's office type "Tok" is authoritative: such an entry is never a class group.
            GroupClassKind.STUDY_TRACK -> GroupClassKind.STUDY_TRACK
            else -> kindOf(head) ?: hintKind ?: GroupClassKind.OTHER
        }
        if (kind == GroupClassKind.STUDY_TRACK) return ParsedGroupCode(kind = kind, number = null)

        // "1/I/N" (a study track) — a bare number followed by year/level segments is not a group number.
        val number = if (head.none { it.isLetter() } && segments.size > 1) {
            null
        } else {
            NUMBER_REGEX.findAll(head).lastOrNull()?.let { match ->
                match.groupValues[1].trimStart('0').ifEmpty { "0" } + match.groupValues[2].uppercase()
            }
        }
        return ParsedGroupCode(kind = kind, number = number)
    }

    private fun kindOf(text: String): GroupClassKind? =
        normalize(text)
            .split(WORD_SPLIT_REGEX)
            .asSequence()
            .filter { it.isNotEmpty() && it !in FILLER_WORDS }
            .mapNotNull(::kindOfWord)
            .firstOrNull()

    private fun kindOfWord(word: String): GroupClassKind? = when {
        word == "w" || word.startsWith("wyk") -> GroupClassKind.LECTURE
        word == "c" || word.startsWith("cw") || word.startsWith("konw") -> GroupClassKind.EXERCISES
        word == "l" || word.startsWith("lab") -> GroupClassKind.LAB
        word == "p" || word.startsWith("proj") -> GroupClassKind.PROJECT
        word == "s" || word == "sem" || word.startsWith("semin") -> GroupClassKind.SEMINAR
        word == "e" || word.startsWith("elrn") || word.startsWith("elearn") -> GroupClassKind.ELEARNING
        word == "pzz" || word.startsWith("prakt") -> GroupClassKind.PRACTICE
        word == "rok" || word == "tok" -> GroupClassKind.STUDY_TRACK
        else -> null
    }

    private fun normalize(text: String): String = buildString(text.length) {
        for (ch in text.lowercase()) {
            append(
                when (ch) {
                    'ą' -> 'a'
                    'ć' -> 'c'
                    'ę' -> 'e'
                    'ł' -> 'l'
                    'ń' -> 'n'
                    'ó' -> 'o'
                    'ś' -> 's'
                    'ź', 'ż' -> 'z'
                    else -> ch
                }
            )
        }
    }
}
