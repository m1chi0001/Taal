package pl.lublin.wsei.plan.data.attachments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentDispositionParserTest {

    @Test
    fun `prefers RFC 5987 filename star`() {
        val header = "attachment; filename=\"Regulamin.pdf\"; filename*=UTF-8''Regulamin%20zaj%C4%99%C4%87.pdf"
        assertEquals("Regulamin zajęć.pdf", ContentDispositionParser.fileName(header))
    }

    @Test
    fun `reads quoted and unquoted filename`() {
        assertEquals("plan zajęć.docx", ContentDispositionParser.fileName("attachment; filename=\"plan zajęć.docx\""))
        assertEquals("plan.xlsx", ContentDispositionParser.fileName("inline;filename=plan.xlsx"))
        assertEquals("a\"b.pdf", ContentDispositionParser.fileName("attachment; filename=\"a\\\"b.pdf\""))
    }

    @Test
    fun `semicolon inside quotes does not split parameters`() {
        assertEquals("a; b.pdf", ContentDispositionParser.fileName("attachment; filename=\"a; b.pdf\"; size=10"))
    }

    @Test
    fun `decodes percent-encoded plain filename and Latin-1 mojibake`() {
        assertEquals("Załącznik.pdf", ContentDispositionParser.fileName("attachment; filename=Za%C5%82%C4%85cznik.pdf"))
        val mojibake = String("Zał.pdf".toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)
        assertEquals("Zał.pdf", ContentDispositionParser.fileName("attachment; filename=\"$mojibake\""))
    }

    @Test
    fun `keeps a literal plus in extended values`() {
        assertEquals("C++ notes.pdf", ContentDispositionParser.fileName("attachment; filename*=UTF-8''C++%20notes.pdf"))
    }

    @Test
    fun `returns null without a filename`() {
        assertNull(ContentDispositionParser.fileName(null))
        assertNull(ContentDispositionParser.fileName("attachment"))
        assertNull(ContentDispositionParser.fileName("inline; size=12"))
    }
}
