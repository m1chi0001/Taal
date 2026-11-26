package pl.lublin.wsei.plan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudentSessionSpecializationTest {

    private fun session(
        specialization: String,
        studyMode: String = "niestacjonarne",
        fieldOfStudy: String = "Informatyka"
    ) = StudentSession(
        userId = 1L,
        personId = 10,
        studId = 100,
        login = "12345",
        firstName = "Jan",
        lastName = "Kowalski",
        albumNumber = "12345",
        fieldOfStudy = fieldOfStudy,
        studyMode = studyMode,
        specialization = specialization,
        accessToken = "token",
        isDebugAccount = false
    )

    @Test
    fun `study mode labels in cached session are rejected as specialization`() {
        assertNull(session("niestacjonarne").cleanSpecialization)
        assertNull(session("Niestacjonarne ").cleanSpecialization)
        assertNull(session("Studia niestacjonarne").cleanSpecialization)
        assertNull(session("stacjonarne", studyMode = "stacjonarne").cleanSpecialization)
        assertNull(session("zaoczne").cleanSpecialization)
        assertNull(session("dzienne").cleanSpecialization)
        assertEquals("", session("niestacjonarne").sanitized().specialization)
    }

    @Test
    fun `field of study duplicate or blank specialization is rejected`() {
        assertNull(session("").cleanSpecialization)
        assertNull(session("   ").cleanSpecialization)
        assertNull(session("Informatyka", fieldOfStudy = "Informatyka").cleanSpecialization)
        assertNull(session("informatyka", fieldOfStudy = "Informatyka").cleanSpecialization)
    }

    @Test
    fun `genuine specialization is preserved and trimmed`() {
        val s = session("  Inżynieria oprogramowania i sztuczna inteligencja  ")
        assertEquals("Inżynieria oprogramowania i sztuczna inteligencja", s.cleanSpecialization)
        assertEquals("Inżynieria oprogramowania i sztuczna inteligencja", s.sanitized().specialization)
    }
}
