package pl.lublin.wsei.plan.data.remote.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.data.remote.ScheduleNormalizer

class AbpEnvelopeAndLenientJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `unwraps ABP envelope and tolerates mistyped fields`() {
        val body = """
            {"result":{"classScheduleGroups":[
              {"id":"17","name":"Bazy danych","form":"Lab","date":"2026-11-14T00:00:00",
               "hourFrom":"08:00:00","hourTo":"09:30:00","idLecturer":null,"lecturerVisible":"True",
               "scheduleType":5,"location":null}
            ]},"targetUrl":null,"success":true,"error":null,"unAuthorizedRequest":false,"__abp":true}
        """.trimIndent()
        val dto = AbpEnvelope.decode(json, body, MsMyClassScheduleDto.serializer())!!
        val item = dto.classScheduleGroups.single()
        assertEquals(17, item.id)
        assertEquals(0, item.idLecturer)
        assertEquals(true, item.lecturerVisible)
        assertEquals("", item.location)
        assertEquals("08:00", ScheduleNormalizer.extractTimeHhMm(item.hourFrom))
    }

    @Test
    fun `decimal and enum fields accept strings and numbers`() {
        val dash = AbpEnvelope.decode(
            json,
            """{"result":{"semestr":"3","averageCurrentSemester":"4,25","studyAverage":4.1},"success":true,"__abp":true}""",
            MsCourseOfStudyDashboardDto.serializer()
        )!!
        assertEquals(3, dash.semestr)
        assertEquals(4.25, dash.averageCurrentSemester, 0.0001)
        assertEquals(4.1, dash.studyAverage, 0.0001)

        val auth = AbpEnvelope.decode(
            json,
            """{"result":{"accessToken":"abc","userId":"123","personId":7,"personType":"Student"},"success":true,"__abp":true}""",
            MobileAuthResponseDto.serializer()
        )!!
        assertEquals("abc", auth.accessToken)
        assertEquals(123L, auth.userId)
        assertEquals("Student", auth.personType)
    }

    @Test
    fun `bare objects without envelope are accepted`() {
        val dto = AbpEnvelope.decode(json, """{"paymentUrl":"https://pay"}""", PaymentAnswerDto.serializer())!!
        assertEquals("https://pay", dto.paymentUrl)
    }

    @Test
    fun `null result decodes to null`() {
        assertNull(AbpEnvelope.decode(json, """{"result":null,"success":true,"__abp":true}""", MsFinancesDto.serializer()))
        assertNull(AbpEnvelope.decode(json, "", MsFinancesDto.serializer()))
    }

    @Test
    fun `errors are typed`() {
        val server = runCatching {
            AbpEnvelope.unwrap(json, """{"success":false,"error":{"code":0,"message":"Brak uprawnień"},"__abp":true}""")
        }.exceptionOrNull()
        assertTrue(server is AprServerException)
        assertEquals("Brak uprawnień", server!!.message)

        val unauthorized = runCatching {
            AbpEnvelope.unwrap(json, """{"success":false,"unAuthorizedRequest":true,"__abp":true}""")
        }.exceptionOrNull()
        assertTrue(unauthorized is AprSessionExpiredException)

        val html = runCatching { AbpEnvelope.unwrap(json, "<!DOCTYPE html><html>login</html>") }.exceptionOrNull()
        assertTrue(html is AprMalformedResponseException)
    }

    @Test
    fun `normalizer handles dates and times`() {
        assertEquals("2026-11-10", ScheduleNormalizer.normalizeIsoDate("Wt, 10.11.2026"))
        assertEquals("2026-11-10", ScheduleNormalizer.normalizeIsoDate("2026-11-10T00:00:00"))
        assertNull(ScheduleNormalizer.normalizeIsoDate("31.02.2026"))
        assertEquals("08:05", ScheduleNormalizer.extractTimeHhMm("1.08:05:00"))
        assertEquals("08:00", ScheduleNormalizer.extractTimeHhMm("8.00"))
        assertEquals("08:00" to "09:30", ScheduleNormalizer.extractTimeRange("8.00–9.30"))
        assertEquals("10.11.2026", ScheduleNormalizer.isoToPolishDate("2026-11-10T00:00:00"))
    }
}
