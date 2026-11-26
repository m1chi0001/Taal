package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentFileTypesTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun zipWithEntry(name: String): ByteArray =
        bytes(0x50, 0x4B, 0x03, 0x04) + ByteArray(26) + name.toByteArray(Charsets.US_ASCII) + ByteArray(16)

    private fun oleWithStream(name: String): ByteArray =
        bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1) + ByteArray(504) + name.toByteArray(Charsets.UTF_16LE)

    @Test
    fun `detects common signatures`() {
        assertEquals(AttachmentFileTypes.PDF, AttachmentFileTypes.sniff("%PDF-1.7\n".toByteArray()))
        assertEquals(AttachmentFileTypes.PDF, AttachmentFileTypes.sniff("\r\n%PDF-1.4".toByteArray()))
        assertEquals(AttachmentFileTypes.PNG, AttachmentFileTypes.sniff(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)))
        assertEquals(AttachmentFileTypes.JPEG, AttachmentFileTypes.sniff(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0)))
        assertEquals(AttachmentFileTypes.GIF, AttachmentFileTypes.sniff("GIF89a....".toByteArray()))
        assertEquals(AttachmentFileTypes.RTF, AttachmentFileTypes.sniff("{\\rtf1\\ansi".toByteArray()))
    }

    @Test
    fun `tells OOXML and ODF containers apart`() {
        assertEquals(AttachmentFileTypes.DOCX, AttachmentFileTypes.sniff(zipWithEntry("word/document.xml")))
        assertEquals(AttachmentFileTypes.XLSX, AttachmentFileTypes.sniff(zipWithEntry("xl/workbook.xml")))
        assertEquals(AttachmentFileTypes.PPTX, AttachmentFileTypes.sniff(zipWithEntry("ppt/presentation.xml")))
        assertEquals(AttachmentFileTypes.ODT, AttachmentFileTypes.sniff(zipWithEntry("mimetypeapplication/vnd.oasis.opendocument.text")))
        assertEquals(AttachmentFileTypes.ZIP, AttachmentFileTypes.sniff(zipWithEntry("docs/readme.txt")))
    }

    @Test
    fun `tells legacy Office documents apart`() {
        assertEquals(AttachmentFileTypes.DOC, AttachmentFileTypes.sniff(oleWithStream("WordDocument")))
        assertEquals(AttachmentFileTypes.XLS, AttachmentFileTypes.sniff(oleWithStream("Workbook")))
        assertEquals(AttachmentFileTypes.PPT, AttachmentFileTypes.sniff(oleWithStream("PowerPoint Document")))
    }

    @Test
    fun `detects plain text but not binary garbage`() {
        assertEquals(AttachmentFileTypes.TXT, AttachmentFileTypes.sniff("Zażółć gęślą jaźń\n".toByteArray()))
        assertNull(AttachmentFileTypes.sniff(bytes(0x00, 0x01, 0x02, 0x03, 0x04)))
        assertNull(AttachmentFileTypes.sniff(ByteArray(0)))
    }

    @Test
    fun `maps extensions and MIME types both ways`() {
        assertEquals("application/pdf", AttachmentFileTypes.mimeForExtension("PDF"))
        assertEquals(AttachmentFileTypes.DOCX.mimeType, AttachmentFileTypes.mimeForExtension("docx"))
        assertNull(AttachmentFileTypes.mimeForExtension("xyz"))
        assertEquals("pdf", AttachmentFileTypes.extensionForMime("application/pdf; charset=binary"))
        assertEquals("jpg", AttachmentFileTypes.extensionForMime("image/jpeg"))
        assertTrue(AttachmentFileTypes.isGenericMime("application/octet-stream"))
        assertTrue(AttachmentFileTypes.isGenericMime(null))
        assertFalse(AttachmentFileTypes.isGenericMime("application/pdf"))
    }

    @Test
    fun `container compatibility`() {
        assertTrue(AttachmentFileTypes.isCompatible(AttachmentFileTypes.ZIP, "xlsx"))
        assertTrue(AttachmentFileTypes.isCompatible(AttachmentFileTypes.DOC, "xls"))
        assertTrue(AttachmentFileTypes.isCompatible(AttachmentFileTypes.TXT, "csv"))
        assertFalse(AttachmentFileTypes.isCompatible(AttachmentFileTypes.PNG, "pdf"))
    }
}
