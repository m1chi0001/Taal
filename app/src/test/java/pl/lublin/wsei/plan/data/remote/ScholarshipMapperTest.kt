package pl.lublin.wsei.plan.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.data.remote.api.MsPaymentDto
import pl.lublin.wsei.plan.data.remote.api.MsSchoolarshipInSemesterDto
import pl.lublin.wsei.plan.data.remote.api.MsSchoolarshipPaymentsDto
import pl.lublin.wsei.plan.data.remote.api.MsScholarshipDetailsDto
import pl.lublin.wsei.plan.data.remote.api.MsScholarshipRulingDto
import pl.lublin.wsei.plan.domain.model.ScholarshipStatusKind

class ScholarshipMapperTest {

    private val title = "WNIOSEK O STYPENDIUM SOCJALNE 2026/2027"

    private fun item(
        number: Int = 70,
        state: String = "",
        date: String? = null,
        granted: Boolean = false
    ) = MsSchoolarshipInSemesterDto(
        id = 501,
        date = date,
        number = number,
        name = title,
        state = state,
        schoolarshipGranted = granted
    )

    private fun details(
        status: String = "Przyjęto",
        variant: String = "Studenci",
        ruling: MsScholarshipRulingDto? = null,
        rulings: List<MsScholarshipRulingDto> = emptyList(),
        accruals: List<MsPaymentDto> = emptyList(),
        payments: List<MsPaymentDto> = emptyList(),
        number: Int = 70
    ) = MsScholarshipDetailsDto(
        id = 501,
        typeName = title,
        typeVariantName = variant,
        number = number,
        name = title,
        applicationDate = "2026-09-14T00:00:00",
        applicationStatusString = status,
        ruling = ruling,
        rulings = rulings,
        accruals = MsSchoolarshipPaymentsDto(payments = accruals),
        payments = MsSchoolarshipPaymentsDto(payments = payments)
    )

    @Test
    fun `accepted status without ruling yields no decision`() {
        val app = ScholarshipMapper.map(item(state = "Złożony"), details(), "Semestr 1")

        assertEquals(70, app.number)
        assertEquals(70, app.displayNumber)
        assertEquals(title, app.title)
        assertEquals(title, app.typeName)
        assertEquals("Studenci", app.typeVariantName)
        assertEquals("Przyjęto", app.statusString)
        assertEquals(ScholarshipStatusKind.ACCEPTED, app.statusKind)
        assertEquals("14.09.2026", app.submissionDateIso)
        assertFalse(app.hasDecision)
        assertNull(app.decisionNumber)
        assertNull(app.decisionDateIso)
        assertNull(app.decisionStatus)
        assertNull(app.amountFormatted)
        assertTrue(app.payouts.isEmpty())
    }

    @Test
    fun `empty placeholder rulings and zero amounts are ignored`() {
        val placeholder = MsScholarshipRulingDto(number = "", date = "0001-01-01T00:00:00", statusString = " ")
        val app = ScholarshipMapper.map(
            item(),
            details(
                ruling = placeholder,
                rulings = listOf(placeholder),
                accruals = listOf(MsPaymentDto(id = 1, amount = "0.00", date = "2026-10-25")),
                payments = listOf(MsPaymentDto(id = 2, amount = "", date = "2026-10-25"))
            ),
            "Semestr 1"
        )

        assertFalse(app.hasDecision)
        assertNull(app.amountFormatted)
        assertTrue(app.payouts.isEmpty())
    }

    @Test
    fun `real ruling and amount are mapped`() {
        val app = ScholarshipMapper.map(
            item(granted = true),
            details(
                status = "Rozpatrzony pozytywnie",
                rulings = listOf(
                    MsScholarshipRulingDto(
                        number = "DSS/123/2026",
                        date = "2026-10-20T00:00:00",
                        statusString = "Przyznano",
                        acceptedRules = true
                    )
                ),
                accruals = listOf(MsPaymentDto(id = 1, amount = "950,00", date = "2026-10-31")),
                payments = listOf(MsPaymentDto(id = 2, amount = "950.00 PLN", date = "2026-11-10", status = "Wypłacono"))
            ),
            "Semestr 1"
        )

        assertTrue(app.hasDecision)
        assertTrue(app.isGranted)
        assertEquals("DSS/123/2026", app.decisionNumber)
        assertEquals("20.10.2026", app.decisionDateIso)
        assertEquals("Przyznano", app.decisionStatus)
        assertEquals(ScholarshipStatusKind.GRANTED, app.statusKind)
        assertEquals("950.00 PLN", app.amountFormatted)
        assertEquals(1, app.payouts.size)
        assertEquals("950.00 PLN", app.payouts.single().amountFormatted)
        assertEquals("10.11.2026", app.payouts.single().dateIso)
    }

    @Test
    fun `ruling with only a date still counts as a decision`() {
        val app = ScholarshipMapper.map(
            item(),
            details(ruling = MsScholarshipRulingDto(date = "2026-10-20")),
            "Semestr 1"
        )

        assertTrue(app.hasDecision)
        assertNull(app.decisionNumber)
        assertEquals("20.10.2026", app.decisionDateIso)
    }

    @Test
    fun `granted flag alone does not fabricate a decision`() {
        val app = ScholarshipMapper.map(item(granted = true), details(), "Semestr 1")

        assertTrue(app.isGranted)
        assertFalse(app.hasDecision)
    }

    @Test
    fun `blank status stays null instead of inventing a value`() {
        val app = ScholarshipMapper.map(item(state = "  "), details(status = ""), "Semestr 1")

        assertNull(app.statusString)
        assertEquals(ScholarshipStatusKind.UNKNOWN, app.statusKind)
    }

    @Test
    fun `list state is used when details are missing`() {
        val app = ScholarshipMapper.map(item(state = "W rozpatrywaniu", date = "2026-09-14"), null, "Semestr 1")

        assertEquals("W rozpatrywaniu", app.statusString)
        assertEquals(ScholarshipStatusKind.UNDER_REVIEW, app.statusKind)
        assertEquals("14.09.2026", app.submissionDateIso)
        assertEquals("", app.typeVariantName)
        assertEquals(title, app.title)
        assertFalse(app.hasDecision)
    }

    @Test
    fun `variant is trimmed and blank variant is empty`() {
        assertEquals("Doktoranci", ScholarshipMapper.map(item(), details(variant = " Doktoranci "), "S").typeVariantName)
        assertEquals("", ScholarshipMapper.map(item(), details(variant = "  "), "S").typeVariantName)
    }

    @Test
    fun `zero application number is hidden`() {
        val app = ScholarshipMapper.map(item(number = 0), details(number = 0), "S")

        assertNull(app.number)
        assertNull(app.displayNumber)
    }

    @Test
    fun `number prefix is stripped from title`() {
        val app = ScholarshipMapper.map(
            item(),
            details().copy(name = "70 / $title"),
            "S"
        )

        assertEquals(title, app.title)
    }

    @Test
    fun `status classification uses normalized keywords`() {
        assertEquals(ScholarshipStatusKind.ACCEPTED, ScholarshipStatusKind.classify("PRZYJETO"))
        assertEquals(ScholarshipStatusKind.ACCEPTED, ScholarshipStatusKind.classify("Przyjęty"))
        assertEquals(ScholarshipStatusKind.GRANTED, ScholarshipStatusKind.classify("Przyznano"))
        assertEquals(ScholarshipStatusKind.GRANTED, ScholarshipStatusKind.classify("Rozpatrzony pozytywnie (Przyznane)"))
        assertEquals(ScholarshipStatusKind.REJECTED, ScholarshipStatusKind.classify("Odrzucono"))
        assertEquals(ScholarshipStatusKind.REJECTED, ScholarshipStatusKind.classify("Rozpatrzony negatywnie"))
        assertEquals(ScholarshipStatusKind.REJECTED, ScholarshipStatusKind.classify("Nie przyznano"))
        assertEquals(ScholarshipStatusKind.UNDER_REVIEW, ScholarshipStatusKind.classify("W rozpatrywaniu"))
        assertEquals(ScholarshipStatusKind.SUBMITTED, ScholarshipStatusKind.classify("Złożony"))
        assertEquals(ScholarshipStatusKind.UNKNOWN, ScholarshipStatusKind.classify("Inny"))
        assertEquals(ScholarshipStatusKind.UNKNOWN, ScholarshipStatusKind.classify(null))
    }

    @Test
    fun `amount parsing handles separators`() {
        assertEquals(1450.0, ScholarshipMapper.parseAmount("1 450.00 PLN")!!, 0.001)
        assertEquals(1450.5, ScholarshipMapper.parseAmount("1.450,50 zł")!!, 0.001)
        assertEquals(950.0, ScholarshipMapper.parseAmount("950,00")!!, 0.001)
        assertNull(ScholarshipMapper.parseAmount(""))
        assertNull(ScholarshipMapper.parseAmount("brak"))
    }
}
