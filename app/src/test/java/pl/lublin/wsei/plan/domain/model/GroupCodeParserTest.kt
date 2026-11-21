package pl.lublin.wsei.plan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupCodeParserTest {

    private fun assertParsed(code: String, kind: GroupClassKind, number: String?, typeHint: String = "") {
        val parsed = GroupCodeParser.parse(code, typeHint)
        assertEquals("kind of '$code'", kind, parsed.kind)
        assertEquals("number of '$code'", number, parsed.number)
    }

    @Test
    fun `lecture code without a number has no group number`() {
        assertParsed("W/1/I/N", GroupClassKind.LECTURE, null, typeHint = "Wykład")
    }

    @Test
    fun `exercise group number is read from the code`() {
        assertParsed("Cw1/1/I/N", GroupClassKind.EXERCISES, "1", typeHint = "Ćwiczenia")
        assertParsed("Ćw2/1/I/N", GroupClassKind.EXERCISES, "2")
    }

    @Test
    fun `lab 4 is group 4, not 1`() {
        assertParsed("Lab4/1/I/N", GroupClassKind.LAB, "4", typeHint = "Laboratorium")
    }

    @Test
    fun `project group number`() {
        assertParsed("Proj2/1/I/N", GroupClassKind.PROJECT, "2")
    }

    @Test
    fun `multi digit numbers and leading zeros`() {
        assertParsed("Lab12/1/I/N", GroupClassKind.LAB, "12")
        assertParsed("Lab04/2/I/S", GroupClassKind.LAB, "4")
    }

    @Test
    fun `lowercase, short and spaced forms`() {
        assertParsed("lab4/1/i/n", GroupClassKind.LAB, "4")
        assertParsed("L4", GroupClassKind.LAB, "4")
        assertParsed("Lab 4", GroupClassKind.LAB, "4")
        assertParsed("lab.4", GroupClassKind.LAB, "4")
        assertParsed("  cw 3 / 1 / I / N ", GroupClassKind.EXERCISES, "3")
    }

    @Test
    fun `polish words`() {
        assertParsed("grupa laboratoryjna 4", GroupClassKind.LAB, "4")
        assertParsed("Grupa ćwiczeniowa 2", GroupClassKind.EXERCISES, "2")
        assertParsed("Seminarium 1", GroupClassKind.SEMINAR, "1")
    }

    @Test
    fun `letter suffix is kept`() {
        assertParsed("Cw1a/1/I/N", GroupClassKind.EXERCISES, "1A")
    }

    @Test
    fun `codes without a number`() {
        assertParsed("Lab/1/I/N", GroupClassKind.LAB, null)
        assertParsed("E-learning", GroupClassKind.ELEARNING, null)
        assertParsed("", GroupClassKind.OTHER, null)
    }

    @Test
    fun `bare study track number is not a group number`() {
        assertParsed("1/I/N", GroupClassKind.OTHER, null, typeHint = "Tok")
    }

    @Test
    fun `year-like numbers are ignored`() {
        assertNull(GroupCodeParser.parse("Lab2026/1/I/N").number)
    }

    @Test
    fun `kind falls back to the type field`() {
        assertParsed("XYZ3/1/I/N", GroupClassKind.LAB, "3", typeHint = "Laboratorium")
        assertParsed("PZZ1/1/I/N", GroupClassKind.OTHER, "1", typeHint = "Prakt zaw zajęcia")
        assertParsed("Gr1/1/I/N", GroupClassKind.ELEARNING, "1", typeHint = "Elearning")
    }

    @Test
    fun `stale cached group number is recomputed from the code`() {
        val stale = StudentGroupItem(id = 1, groupCode = "Lab4/1/I/N", formName = "Laboratorium", groupNumber = "1")
        assertEquals("4", stale.displayGroupNumber)
        assertEquals(GroupClassKind.LAB, stale.groupKind)

        val lecture = StudentGroupItem(id = 2, groupCode = "W/1/I/N", formName = "Wykład", groupNumber = "1")
        assertEquals("", lecture.displayGroupNumber)
        assertEquals(GroupClassKind.LECTURE, lecture.groupKind)
    }
}
