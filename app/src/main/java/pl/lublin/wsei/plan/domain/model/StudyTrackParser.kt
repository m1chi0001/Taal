package pl.lublin.wsei.plan.domain.model

/**
 * Builds [StudyTrackInfo] from groups (study-track entries) and the session's course code.
 *
 * Observed inputs:
 * - study-track group `ROK1/ N` (type "Tok") → year 1, part-time;
 * - semester label `2026/2027 Z` or `Semestr 2026/27 Z` → academic year "2026/2027", winter term;
 * - course code `KI/N 2026/27 Z` → course "KI", part-time (and the academic year/term as a fallback);
 * - study mode text "niestacjonarne" / "stacjonarne" as the last fallback for the mode.
 *
 * The most recent semester is used: the newest academic year / term, then the highest semester
 * number; on a tie the earlier list entry wins (the API data source lists the newest first).
 * Returns null when nothing at all could be read.
 */
object StudyTrackParser {

    /** `ROK1`, `Rok 2`, `rok03`. */
    private val YEAR_OF_STUDY_REGEX = Regex("""\brok\s*0*([0-9]{1,2})(?![0-9])""", RegexOption.IGNORE_CASE)

    /** A `/`-separated segment that is just the mode letter: `ROK1/ N`, `KI/N 2026/27 Z`. */
    private val MODE_SEGMENT_REGEX = Regex("""/\s*([NSns])(?![\p{L}0-9])""")

    /** `2026/2027`, `2026/27`, `2026 / 27`, optionally followed by the term letter `Z` / `L`. */
    private val ACADEMIC_YEAR_REGEX =
        Regex("""(?<![0-9])((?:19|20)[0-9]{2})\s*/\s*([0-9]{4}|[0-9]{2})(?![0-9])(?:\s*([ZLzl])(?![\p{L}0-9]))?""")

    /** Course code before the first slash: `KI` in `KI/N 2026/27 Z`. */
    private val COURSE_CODE_REGEX = Regex("""^\s*([\p{L}][\p{L}0-9-]*)\s*/""")

    fun from(
        semesters: List<StudentSemesterGroups>,
        courseOfStudyCode: String?,
        studyModeText: String?
    ): StudyTrackInfo? {
        val semester = mostRecent(semesters.filter { sem -> sem.groups.any { it.isStudyTrack } })
            ?: mostRecent(semesters)
        val trackCode = semester?.groups?.firstOrNull { it.isStudyTrack }?.groupCode?.trim()?.takeIf { it.isNotEmpty() }
        val course = courseOfStudyCode?.trim()?.takeIf { it.isNotEmpty() }

        val labelYear = semester?.semesterLabel?.let(::parseAcademicYear)
        val courseYear = course?.let(::parseAcademicYear)
        // Take the term together with the year it was written with, so the two never disagree.
        val academic = labelYear?.takeIf { it.year != null } ?: courseYear?.takeIf { it.year != null }
        val academicYear = academic?.year
        val term = academic?.term
            ?: labelYear?.term
            ?: courseYear?.term
            ?: semester?.semesterLabel?.let(::termFromWords)

        val info = StudyTrackInfo(
            yearOfStudy = trackCode?.let(::parseYearOfStudy),
            academicYear = academicYear,
            semesterTerm = term,
            studyMode = trackCode?.let(::parseModeFromCode)
                ?: course?.let(::parseModeFromCode)
                ?: studyModeText?.let(::parseModeFromText)
                ?: StudyMode.UNKNOWN,
            courseCode = course?.let(::parseCourseCode),
            rawTrackCode = trackCode
        )
        val isEmpty = info.yearOfStudy == null && info.academicYear == null && info.semesterTerm == null &&
            info.studyMode == StudyMode.UNKNOWN && info.courseCode == null && info.rawTrackCode == null
        return if (isEmpty) null else info
    }

    /** Year of study from `ROK<n>`; null when the code has none. */
    fun parseYearOfStudy(code: String): Int? =
        YEAR_OF_STUDY_REGEX.find(code)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..10 }

    /** `…/N` → part-time, `…/S` → full-time; null when the code has no mode segment. */
    fun parseModeFromCode(code: String): StudyMode? =
        when (MODE_SEGMENT_REGEX.find(code)?.groupValues?.get(1)?.uppercase()) {
            "N" -> StudyMode.PART_TIME
            "S" -> StudyMode.FULL_TIME
            else -> null
        }

    /** "niestacjonarne" / "zaoczne" → part-time, "stacjonarne" / "dzienne" → full-time. */
    fun parseModeFromText(text: String): StudyMode? {
        val t = text.lowercase()
        return when {
            "niestacjonarn" in t || "zaoczn" in t || "part-time" in t || "part time" in t -> StudyMode.PART_TIME
            "stacjonarn" in t || "dzienn" in t || "full-time" in t || "full time" in t -> StudyMode.FULL_TIME
            else -> null
        }
    }

    /** "KI" from `KI/N 2026/27 Z`; null when the code has no leading course segment. */
    fun parseCourseCode(code: String): String? =
        COURSE_CODE_REGEX.find(code)?.groupValues?.get(1)?.uppercase()

    data class AcademicYearAndTerm(val year: String?, val term: SemesterTerm?)

    /** `2026/2027 Z`, `Semestr 2026/27 Z`, `KI/N 2026/27 Z` → ("2026/2027", WINTER). */
    fun parseAcademicYear(text: String): AcademicYearAndTerm? {
        val match = ACADEMIC_YEAR_REGEX.find(text) ?: return termFromWords(text)?.let { AcademicYearAndTerm(null, it) }
        val first = match.groupValues[1].toInt()
        val secondRaw = match.groupValues[2]
        val second = if (secondRaw.length == 2) {
            val candidate = first / 100 * 100 + secondRaw.toInt()
            if (candidate < first) candidate + 100 else candidate
        } else {
            secondRaw.toInt()
        }
        // "2026/2027" only; anything else (e.g. a date "2026/11") is not an academic year.
        if (second != first + 1) return termFromWords(text)?.let { AcademicYearAndTerm(null, it) }
        val term = when (match.groupValues[3].uppercase()) {
            "Z" -> SemesterTerm.WINTER
            "L" -> SemesterTerm.SUMMER
            else -> termFromWords(text)
        }
        return AcademicYearAndTerm("$first/$second", term)
    }

    private fun termFromWords(text: String): SemesterTerm? {
        val t = text.lowercase()
        return when {
            "zimow" in t -> SemesterTerm.WINTER
            "letni" in t -> SemesterTerm.SUMMER
            else -> null
        }
    }

    private fun mostRecent(semesters: List<StudentSemesterGroups>): StudentSemesterGroups? {
        if (semesters.isEmpty()) return null
        val order = compareBy<StudentSemesterGroups>(
            { sem -> parseAcademicYear(sem.semesterLabel)?.year?.take(4)?.toIntOrNull() ?: -1 },
            { sem -> parseAcademicYear(sem.semesterLabel)?.term?.ordinal ?: -1 },
            { sem -> sem.semesterNumber }
        )
        // maxWithOrNull keeps the first of equal elements, i.e. the earlier (newer) list entry.
        return semesters.maxWithOrNull(order)
    }
}
