package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentBase64Test {

    private val pdfHeader = "%PDF-1.4\n".toByteArray(Charsets.US_ASCII)

    @Test
    fun `decodes standard Base64`() {
        assertArrayEquals(pdfHeader, AttachmentBase64.decode("JVBERi0xLjQK"))
    }

    @Test
    fun `strips data URI prefix, whitespace and escaped line breaks`() {
        assertArrayEquals(pdfHeader, AttachmentBase64.decode("data:application/pdf;base64,JVBE\r\nRi0x LjQK"))
        assertArrayEquals(pdfHeader, AttachmentBase64.decode("JVBE\\r\\nRi0xLjQK"))
        assertArrayEquals(pdfHeader, AttachmentBase64.decode("\"JVBERi0xLjQK\""))
    }

    @Test
    fun `accepts URL-safe alphabet and missing padding`() {
        val bytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte(), 0xBF.toByte(), 0x01)
        val standard = java.util.Base64.getEncoder().encodeToString(bytes) // "+/+/AQ=="
        val urlSafeUnpadded = standard.replace('+', '-').replace('/', '_').trimEnd('=')
        assertArrayEquals(bytes, AttachmentBase64.decode(urlSafeUnpadded))
        assertEquals(standard, AttachmentBase64.normalize(urlSafeUnpadded))
    }

    @Test
    fun `rejects non Base64 input`() {
        assertNull(AttachmentBase64.decode(null))
        assertNull(AttachmentBase64.decode(""))
        assertNull(AttachmentBase64.decode("not base64!"))
        assertNull(AttachmentBase64.decode("abcde")) // impossible length
        assertNull(AttachmentBase64.normalize("<html>"))
    }

    @Test
    fun `tells Base64 bodies from prose`() {
        assertTrue(AttachmentBase64.looksLikeBase64("JVBERi0xLjQKJcfsj6IK"))
        assertTrue(AttachmentBase64.looksLikeBase64("data:application/pdf;base64,JVBERi0xLjQK"))
        assertFalse(AttachmentBase64.looksLikeBase64("Brak pliku"))
        assertFalse(AttachmentBase64.looksLikeBase64("abc"))
    }
}
