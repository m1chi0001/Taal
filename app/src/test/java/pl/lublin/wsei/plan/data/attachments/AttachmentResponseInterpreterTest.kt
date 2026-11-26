package pl.lublin.wsei.plan.data.attachments

import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.lublin.wsei.plan.data.remote.api.AprServerException
import pl.lublin.wsei.plan.data.remote.api.AprSessionExpiredException

class AttachmentResponseInterpreterTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val pdf = "%PDF-1.4\n".toByteArray(Charsets.US_ASCII)
    private val pdfBase64 = "JVBERi0xLjQK"

    private fun interpret(body: String, type: String? = "application/json", disposition: String? = null) =
        AttachmentResponseInterpreter.interpret(json, type, disposition, body.toByteArray(Charsets.UTF_8))

    private fun assertBytes(payload: AttachmentPayload, expectedName: String?) {
        assertTrue("expected bytes, got $payload", payload is AttachmentPayload.Bytes)
        payload as AttachmentPayload.Bytes
        assertArrayEquals(pdf, payload.bytes)
        assertEquals(expectedName, payload.fileName)
    }

    @Test
    fun `ABP envelope with fileBytes`() {
        val body = """{"result":{"id":5,"name":"Regulamin.pdf","fileBytes":"$pdfBase64"},"targetUrl":null,
            "success":true,"error":null,"unAuthorizedRequest":false,"__abp":true}"""
        assertBytes(interpret(body), "Regulamin.pdf")
    }

    @Test
    fun `alternative content keys and nesting`() {
        assertBytes(interpret("""{"result":{"fileName":"a.pdf","bytes":"$pdfBase64"},"success":true}"""), "a.pdf")
        assertBytes(interpret("""{"name":"b.pdf","content":"data:application/pdf;base64,$pdfBase64"}"""), "b.pdf")
        assertBytes(interpret("""{"name":"Dziekanat","file":{"name":"c.pdf","data":"$pdfBase64"}}"""), "c.pdf")
        assertBytes(interpret("""[{"name":"d.pdf","base64":"$pdfBase64"}]"""), "d.pdf")
    }

    @Test
    fun `byte array as numbers and JSON string body`() {
        val numbers = pdf.joinToString(",", "[", "]") { it.toString() }
        assertBytes(interpret("""{"result":{"name":"e.pdf","fileBytes":$numbers},"success":true}"""), "e.pdf")
        assertBytes(interpret("\"$pdfBase64\"", disposition = "attachment; filename=f.pdf"), "f.pdf")
    }

    @Test
    fun `raw binary body uses Content-Disposition name`() {
        val payload = AttachmentResponseInterpreter.interpret(
            json, "application/pdf", "attachment; filename*=UTF-8''Za%C5%82%C4%85cznik.pdf", pdf
        )
        assertBytes(payload, "Załącznik.pdf")
    }

    @Test
    fun `bare Base64 text body`() {
        assertBytes(interpret(pdfBase64, type = "text/plain"), null)
    }

    @Test
    fun `links from url fields, targetUrl and JSON strings`() {
        assertEquals(
            AttachmentPayload.Link("/Files/a.pdf", "a.pdf"),
            interpret("""{"result":{"name":"a.pdf","fileBytes":null,"url":"/Files/a.pdf"},"success":true}""")
        )
        assertEquals(
            AttachmentPayload.Link("https://x.pl/Download.ashx?id=1", null),
            interpret("""{"result":null,"targetUrl":"https://x.pl/Download.ashx?id=1","success":true,"__abp":true}""")
        )
        assertEquals(AttachmentPayload.Link("https://x.pl/a.pdf", null), interpret("\"https://x.pl/a.pdf\""))
    }

    @Test
    fun `empty results`() {
        assertEquals(AttachmentPayload.Empty(null), interpret("""{"result":null,"success":true,"__abp":true}"""))
        assertEquals(AttachmentPayload.Empty("a.pdf"), interpret("""{"result":{"name":"a.pdf","fileBytes":""},"success":true}"""))
        assertEquals(AttachmentPayload.Empty(null), interpret(""))
    }

    @Test
    fun `corrupt Base64 is reported`() {
        val broken = "%%%" + "A".repeat(100)
        assertEquals(AttachmentPayload.CorruptBase64, interpret("""{"fileBytes":"$broken"}"""))
    }

    @Test
    fun `HTML pages are reported, login pages flagged`() {
        assertEquals(
            AttachmentPayload.Html(isLoginPage = true),
            interpret("<html><form action=\"/Account/Login\"><input type=\"password\"></form></html>", type = "text/html")
        )
        assertEquals(AttachmentPayload.Html(isLoginPage = false), interpret("<!DOCTYPE html><html>Błąd</html>", type = "text/html"))
    }

    @Test(expected = AprSessionExpiredException::class)
    fun `unauthorized envelope throws session expired`() {
        interpret("""{"result":null,"success":false,"unAuthorizedRequest":true,"__abp":true}""")
    }

    @Test
    fun `ABP error envelope keeps the server message`() {
        try {
            interpret("""{"success":false,"error":{"code":0,"message":"Brak załącznika"},"__abp":true}""")
            throw AssertionError("expected AprServerException")
        } catch (e: AprServerException) {
            assertEquals("Brak załącznika", e.message)
        }
    }
}
