package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentUrlsTest {

    private val base = "https://dziekanat.wsei.lublin.pl:20830/"

    @Test
    fun `resolves relative, root-relative and ASP_NET paths`() {
        assertEquals("https://dziekanat.wsei.lublin.pl:20830/Files/a.pdf", AttachmentUrls.resolve(base, "/Files/a.pdf"))
        assertEquals("https://dziekanat.wsei.lublin.pl:20830/Files/a.pdf", AttachmentUrls.resolve(base, "Files/a.pdf"))
        assertEquals("https://dziekanat.wsei.lublin.pl:20830/Files/a.pdf", AttachmentUrls.resolve(base, "~/Files/a.pdf"))
        assertEquals(
            "https://dziekanat.wsei.lublin.pl:20830/api/b?id=1",
            AttachmentUrls.resolve("https://dziekanat.wsei.lublin.pl:20830/api/a?x=2", "b?id=1")
        )
    }

    @Test
    fun `resolves against a base without a trailing slash`() {
        assertEquals(
            "https://dziekanat.wsei.lublin.pl:20830/Files/a.pdf",
            AttachmentUrls.resolve("https://dziekanat.wsei.lublin.pl:20830", "Files/a.pdf")
        )
    }

    @Test
    fun `keeps absolute and protocol-relative URLs, encodes spaces`() {
        assertEquals("https://cdn.example.org/f.pdf", AttachmentUrls.resolve(base, "https://cdn.example.org/f.pdf"))
        assertEquals("https://cdn.example.org/f.pdf", AttachmentUrls.resolve(base, "//cdn.example.org/f.pdf"))
        assertEquals("https://dziekanat.wsei.lublin.pl:20830/Files/plan%20zaj.pdf", AttachmentUrls.resolve(base, "/Files/plan zaj.pdf"))
    }

    @Test
    fun `rejects non HTTP schemes and blanks`() {
        assertNull(AttachmentUrls.resolve(base, "javascript:alert(1)"))
        assertNull(AttachmentUrls.resolve(base, "file:///etc/passwd"))
        assertNull(AttachmentUrls.resolve(base, " "))
        assertNull(AttachmentUrls.resolve(base, null))
    }

    @Test
    fun `same host ignores the port`() {
        assertTrue(AttachmentUrls.isSameHost("https://dziekanat.wsei.lublin.pl/Files/a.pdf", "dziekanat.wsei.lublin.pl"))
        assertFalse(AttachmentUrls.isSameHost("https://evil.example/dziekanat.wsei.lublin.pl", "dziekanat.wsei.lublin.pl"))
    }

    @Test
    fun `tells links from Base64`() {
        assertTrue(AttachmentUrls.looksLikeUrl("https://x.pl/a.pdf"))
        assertTrue(AttachmentUrls.looksLikeUrl("/Files/Attachment/12"))
        assertTrue(AttachmentUrls.looksLikeUrl("Files/a.pdf"))
        assertTrue(AttachmentUrls.looksLikeUrl("~/Files/a.pdf"))
        assertFalse(AttachmentUrls.looksLikeUrl("JVBERi0xLjQK"))
        assertFalse(AttachmentUrls.looksLikeUrl("/9j/" + "A".repeat(2000)))
        assertFalse(AttachmentUrls.looksLikeUrl("plan.pdf"))
        assertFalse(AttachmentUrls.looksLikeUrl("data:application/pdf;base64,JVBERi0xLjQK"))
        assertFalse(AttachmentUrls.looksLikeUrl(""))
    }

    @Test
    fun `log description has no parameter values`() {
        val described = AttachmentUrls.describeForLog("$base" + "api/x?Id=123&IdLoggedPersonType=456")
        assertEquals("dziekanat.wsei.lublin.pl:20830/api/x?[Id,IdLoggedPersonType]", described)
        assertFalse(described.contains("123"))
    }
}
