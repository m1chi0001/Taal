package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentFileNamesTest {

    private val pdf = "%PDF-1.4\n%%EOF".toByteArray()
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0)

    @Test
    fun `sanitizes paths, illegal characters and bidi overrides`() {
        assertEquals("plan.pdf", AttachmentFileNames.sanitize("C:\\Users\\x\\plan.pdf"))
        assertEquals("plan.pdf", AttachmentFileNames.sanitize("../../plan.pdf"))
        assertEquals("a_b_c.pdf", AttachmentFileNames.sanitize("a:b?c.pdf"))
        assertEquals("_fdp.exe", AttachmentFileNames.sanitize("\u202Efdp.exe"))
        assertEquals("zalacznik", AttachmentFileNames.sanitize("  ..  "))
        assertEquals("fallback", AttachmentFileNames.sanitize(null, "fallback"))
    }

    @Test
    fun `caps long names but keeps the extension`() {
        val result = AttachmentFileNames.sanitize("a".repeat(300) + ".docx")
        assertTrue(result.length <= 120)
        assertTrue(result.endsWith(".docx"))
    }

    @Test
    fun `recognizes real extensions only`() {
        assertEquals("pdf", AttachmentFileNames.extensionOf("Plan.PDF"))
        assertEquals("7z", AttachmentFileNames.extensionOf("a.7z"))
        assertNull(AttachmentFileNames.extensionOf("Komunikat 12.2024"))
        assertNull(AttachmentFileNames.extensionOf("README"))
        assertNull(AttachmentFileNames.extensionOf(".pdf"))
    }

    @Test
    fun `adds the detected extension to a name without one`() {
        val resolved = AttachmentFileNames.resolve(listOf("Komunikat 12.2024"), pdf)
        assertEquals("Komunikat 12.2024.pdf", resolved.fileName)
        assertEquals("application/pdf", resolved.mimeType)
    }

    @Test
    fun `trusts the bytes over a contradicting extension`() {
        val resolved = AttachmentFileNames.resolve(listOf("skan.pdf"), png)
        assertEquals("skan.png", resolved.fileName)
        assertEquals("image/png", resolved.mimeType)
    }

    @Test
    fun `keeps a compatible container extension`() {
        val zip = byteArrayOf(0x50, 0x4B, 0x03, 0x04) + ByteArray(40)
        val resolved = AttachmentFileNames.resolve(listOf("oceny.xlsx"), zip)
        assertEquals("oceny.xlsx", resolved.fileName)
        assertEquals(AttachmentFileTypes.XLSX.mimeType, resolved.mimeType)
    }

    @Test
    fun `borrows the extension from a later candidate`() {
        val resolved = AttachmentFileNames.resolve(listOf("Regulamin", "Regulamin.docx"), "plain words here".toByteArray())
        assertEquals("Regulamin.docx", resolved.fileName)
    }

    @Test
    fun `uses declared MIME type, then fallback name, then octet-stream`() {
        val unknown = byteArrayOf(0x00, 0x13, 0x37)
        assertEquals("zalacznik_5.pdf", AttachmentFileNames.resolve(listOf(null), unknown, "application/pdf", "zalacznik_5").fileName)
        val generic = AttachmentFileNames.resolve(listOf("dane.xyz"), unknown, "application/octet-stream")
        assertEquals("dane.xyz", generic.fileName)
        assertEquals(AttachmentFileTypes.OCTET_STREAM, generic.mimeType)
    }

    @Test
    fun `generic response names lose to the list name`() {
        assertTrue(AttachmentFileNames.isGenericName("GetAttachmentBytes"))
        assertTrue(AttachmentFileNames.isGenericName("download.ashx"))
        assertTrue(AttachmentFileNames.isGenericName("3f2504e0-4f89-11d3-9a0c-0305e82c3301.pdf"))
        assertFalse(AttachmentFileNames.isGenericName("Regulamin.pdf"))
        assertEquals(
            listOf("Regulamin.pdf", "x.pdf", "file.pdf"),
            AttachmentFileNames.candidates("file.pdf", "Regulamin.pdf", "x.pdf")
        )
        assertEquals(
            listOf("Plan.pdf", "Regulamin.pdf", null),
            AttachmentFileNames.candidates("Plan.pdf", "Regulamin.pdf", null)
        )
    }

    @Test
    fun `name from URL`() {
        assertEquals("plan zajęć.pdf", AttachmentFileNames.nameFromUrl("https://x/Files/plan%20zaj%C4%99%C4%87.pdf?v=1"))
        assertNull(AttachmentFileNames.nameFromUrl("https://x/Download/"))
        assertNull(AttachmentFileNames.nameFromUrl(null))
    }
}
