package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentResponseClassifierTest {

    private fun classify(body: String, type: String? = null, disposition: String? = null) =
        AttachmentResponseClassifier.classify(type, disposition, body.toByteArray(Charsets.UTF_8))

    @Test
    fun `empty and blank bodies`() {
        assertEquals(AttachmentBodyKind.EMPTY, AttachmentResponseClassifier.classify("application/json", null, ByteArray(0)))
        assertEquals(AttachmentBodyKind.EMPTY, classify("  \r\n "))
    }

    @Test
    fun `JSON envelope, bare object, array and string`() {
        assertEquals(AttachmentBodyKind.JSON, classify("{\"result\":{\"fileBytes\":\"JVBE\"},\"__abp\":true}", "application/json"))
        assertEquals(AttachmentBodyKind.JSON, classify("\uFEFF [1,2,3]", "text/plain"))
        assertEquals(AttachmentBodyKind.JSON, classify("\"JVBERi0xLjQK\"", "application/json; charset=utf-8"))
    }

    @Test
    fun `magic bytes win over a wrong content type`() {
        assertEquals(AttachmentBodyKind.BINARY, classify("%PDF-1.4 ...", "text/html"))
        assertEquals(AttachmentBodyKind.BINARY, classify("%PDF-1.4 ...", "application/json"))
    }

    @Test
    fun `HTML pages are never files`() {
        assertEquals(AttachmentBodyKind.HTML, classify("<!DOCTYPE html><html><body>Error</body></html>", "text/html"))
        assertEquals(AttachmentBodyKind.HTML, classify("\r\n  <html lang=\"pl\">", "application/octet-stream"))
        assertEquals(AttachmentBodyKind.HTML, classify("Wystąpił błąd serwera", "text/html; charset=utf-8"))
        assertEquals(AttachmentBodyKind.HTML, classify("<?xml version=\"1.0\"?><html xmlns=\"x\"><body></body></html>"))
    }

    @Test
    fun `explicit HTML attachment is a file`() {
        assertEquals(
            AttachmentBodyKind.BINARY,
            classify("<html><body>Harmonogram</body></html>", "text/html", "attachment; filename=\"harmonogram.html\"")
        )
    }

    @Test
    fun `bare Base64 text body`() {
        assertEquals(AttachmentBodyKind.BASE64_TEXT, classify("JVBERi0xLjQKJcfsj6IK", "text/plain"))
        assertEquals(AttachmentBodyKind.BASE64_TEXT, classify("data:application/pdf;base64,JVBERi0xLjQK"))
    }

    @Test
    fun `plain text attachment stays binary`() {
        assertEquals(AttachmentBodyKind.BINARY, classify("Lista obecności\nJan Kowalski", "text/plain", "attachment; filename=lista.txt"))
        assertEquals(AttachmentBodyKind.BINARY, classify("a;b;c\n1;2;3", "text/csv", "attachment; filename=\"dane.csv\""))
    }

    @Test
    fun `login page detection`() {
        assertTrue(AttachmentResponseClassifier.looksLikeLoginPage("<form action=\"/Account/Login\"><input type=\"password\"></form>".toByteArray()))
        assertTrue(AttachmentResponseClassifier.looksLikeLoginPage("<title>Logowanie</title>".toByteArray()))
        assertFalse(AttachmentResponseClassifier.looksLikeLoginPage("<html><body>404 Not Found</body></html>".toByteArray()))
    }
}
