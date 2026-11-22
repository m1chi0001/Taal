package pl.lublin.wsei.plan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyTrackParserTest {

    private fun group(id: Int, code: String, type: String) =
        StudentGroupItem(id = id, groupCode = code, formName = type, groupNumber = "")

    private val observedGroups = listOf(
        group(1, "W/1/I/N", "Wykład"),
        group(2, "Cw1/1/I/N", "Ćwiczenia"),
        group(3, "Elrn1/1/I N", "Elearning"),
        group(4, "Lab4/1/I/N", "Laboratorium"),
        group(5, "PZZ1/1/I/N", "Prakt zaw zajęcia"),
        group(6, "ROK1/ N", "Tok")
    )

    private val observedSemester = StudentSemesterGroups(
        semesterId = 10,
        semesterLabel = "2026/2027 Z",
        semesterNumber = 1,
        groups = observedGroups
    )

    @Test
    fun `observed account`() {
        val info = StudyTrackParser.from(listOf(observedSemester), "KI/N 2026/27 Z", "niestacjonarne")!!
        assertEquals(1, info.yearOfStudy)
        assertEquals("2026/2027", info.academicYear)
        assertEquals(SemesterTerm.WINTER, info.semesterTerm)
        assertEquals(StudyMode.PART_TIME, info.studyMode)
        assertEquals("KI", info.courseCode)
        assertEquals("ROK1/ N", info.rawTrackCode)
    }

    @Test
    fun `only the study track is excluded from class groups`() {
        val classGroups = observedGroups.filterNot { it.isStudyTrack }
        assertEquals(listOf(1, 2, 3, 4, 5), classGroups.map { it.id })
    }

    @Test
    fun `short academic year label is expanded`() {
        val sem = observedSemester.copy(semesterLabel = "Semestr 2026/27 Z")
        val info = StudyTrackParser.from(listOf(sem), null, null)!!
        assertEquals("2026/2027", info.academicYear)
        assertEquals(SemesterTerm.WINTER, info.semesterTerm)
        assertEquals(StudyMode.PART_TIME, info.studyMode)
        assertNull(info.courseCode)
    }

    @Test
    fun `summer term and full-time mode`() {
        val sem = StudentSemesterGroups(1, "2025/2026 L", 2, listOf(group(1, "ROK2/S", "Tok")))
        val info = StudyTrackParser.from(listOf(sem), null, null)!!
        assertEquals(2, info.yearOfStudy)
        assertEquals("2025/2026", info.academicYear)
        assertEquals(SemesterTerm.SUMMER, info.semesterTerm)
        assertEquals(StudyMode.FULL_TIME, info.studyMode)
    }

    @Test
    fun `academic year falls back to the course code`() {
        val sem = StudentSemesterGroups(1, "Semestr 1", 1, listOf(group(1, "ROK1/ N", "Tok")))
        val info = StudyTrackParser.from(listOf(sem), "KI/N 2026/27 Z", null)!!
        assertEquals("2026/2027", info.academicYear)
        assertEquals(SemesterTerm.WINTER, info.semesterTerm)
        assertEquals("KI", info.courseCode)
    }

    @Test
    fun `mode falls back to course code and then to the text`() {
        val sem = StudentSemesterGroups(1, "2026/2027 Z", 1, listOf(group(1, "W/1/I/N", "Wykład")))
        assertEquals(StudyMode.FULL_TIME, StudyTrackParser.from(listOf(sem), "KI/S 2026/27 Z", "niestacjonarne")!!.studyMode)
        assertEquals(StudyMode.PART_TIME, StudyTrackParser.from(listOf(sem), null, "niestacjonarne")!!.studyMode)
        assertEquals(StudyMode.FULL_TIME, StudyTrackParser.from(listOf(sem), null, "stacjonarne")!!.studyMode)
        assertEquals(StudyMode.UNKNOWN, StudyTrackParser.from(listOf(sem), null, null)!!.studyMode)
        assertNull(StudyTrackParser.from(listOf(sem), null, null)!!.yearOfStudy)
    }

    @Test
    fun `most recent semester wins regardless of list order`() {
        val older = StudentSemesterGroups(1, "2025/2026 L", 2, listOf(group(1, "ROK1/ N", "Tok")))
        val newer = StudentSemesterGroups(2, "2026/2027 Z", 3, listOf(group(2, "ROK2/ N", "Tok")))
        assertEquals(2, StudyTrackParser.from(listOf(newer, older), null, null)!!.yearOfStudy)
        assertEquals(2, StudyTrackParser.from(listOf(older, newer), null, null)!!.yearOfStudy)
        assertEquals("2026/2027", StudyTrackParser.from(listOf(older, newer), null, null)!!.academicYear)
    }

    @Test
    fun `winter before summer within one academic year`() {
        val winter = StudentSemesterGroups(1, "2026/2027 Z", 1, listOf(group(1, "ROK1/ N", "Tok")))
        val summer = StudentSemesterGroups(2, "2026/2027 L", 2, listOf(group(2, "ROK1/ N", "Tok")))
        assertEquals(SemesterTerm.SUMMER, StudyTrackParser.from(listOf(winter, summer), null, null)!!.semesterTerm)
    }

    @Test
    fun `nothing known yields null`() {
        assertNull(StudyTrackParser.from(emptyList(), null, null))
        assertNull(StudyTrackParser.from(emptyList(), "  ", "inne"))
    }

    @Test
    fun `helpers`() {
        assertEquals(3, StudyTrackParser.parseYearOfStudy("Rok 3/S"))
        assertNull(StudyTrackParser.parseYearOfStudy("Lab4/1/I/N"))
        assertEquals(StudyMode.PART_TIME, StudyTrackParser.parseModeFromCode("ROK1/ N"))
        assertEquals(StudyMode.PART_TIME, StudyTrackParser.parseModeFromCode("KI/N 2026/27 Z"))
        assertNull(StudyTrackParser.parseModeFromCode("ROK1"))
        assertEquals("KI", StudyTrackParser.parseCourseCode("KI/N 2026/27 Z"))
        assertEquals(
            StudyTrackParser.AcademicYearAndTerm("2099/2100", SemesterTerm.WINTER),
            StudyTrackParser.parseAcademicYear("2099/00 Z")
        )
        assertNull(StudyTrackParser.parseAcademicYear("Semestr 1")?.year)
    }
}
