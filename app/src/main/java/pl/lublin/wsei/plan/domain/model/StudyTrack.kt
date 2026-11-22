package pl.lublin.wsei.plan.domain.model

import androidx.compose.runtime.Immutable

/** Mode of study parsed from course / study-track codes (`…/N` = part-time, `…/S` = full-time). */
enum class StudyMode { FULL_TIME, PART_TIME, UNKNOWN }

/** Winter (`Z`) or summer (`L`) semester of an academic year. */
enum class SemesterTerm { WINTER, SUMMER }

/**
 * "Tok nauki": the student's place in the programme, kept apart from class groups.
 * Built from study-track entries such as `ROK1/ N`, semester labels like `2026/2027 Z`
 * and the course code `KI/N 2026/27 Z`. Every field is null/UNKNOWN when not present in the data.
 */
@Immutable
data class StudyTrackInfo(
    /** Year of study, e.g. 1 for `ROK1/ N`. */
    val yearOfStudy: Int?,
    /** Academic year as "2026/2027". */
    val academicYear: String?,
    val semesterTerm: SemesterTerm?,
    val studyMode: StudyMode,
    /** Course of study code, e.g. "KI". */
    val courseCode: String?,
    /** Raw study-track code as returned by the dean's office, e.g. "ROK1/ N". */
    val rawTrackCode: String?
)
