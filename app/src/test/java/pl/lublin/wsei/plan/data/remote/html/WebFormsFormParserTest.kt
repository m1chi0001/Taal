package pl.lublin.wsei.plan.data.remote.html

import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Buffer
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebFormsFormParserTest {

    private val loginHtml = """
        <html><body>
        <form method="post" action="./Logowanie.aspx?ReturnUrl=%2f" id="aspnetForm">
          <input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="dDwtMTA4MTc=" />
          <input type="hidden" name="__VIEWSTATEGENERATOR" value="C2EE9ABB" />
          <input type="hidden" name="__EVENTVALIDATION" value="/wEWBALx" />
          <input name="ctl00${'$'}Main${'$'}txtLogin" type="text" id="ctl00_Main_txtLogin" />
          <input name="ctl00${'$'}Main${'$'}txtHaslo" type="password" id="ctl00_Main_txtHaslo" />
          <input type="checkbox" name="ctl00${'$'}Main${'$'}chkPamietaj" />
          <input type="submit" name="ctl00${'$'}Main${'$'}btnZaloguj" value="Zaloguj" />
          <input type="submit" name="ctl00${'$'}Main${'$'}btnPrzypomnij" value="Przypomnij hasło" />
        </form></body></html>
    """.trimIndent()

    @Test
    fun `keeps ASP NET hidden fields and fills credentials`() {
        val pageUrl = "https://wd.example.invalid/Konto/Logowanie.aspx".toHttpUrl()
        val form = WebFormsFormParser.parseLoginForm(Jsoup.parse(loginHtml), pageUrl)
        assertNotNull(form)
        form!!
        assertTrue(form.hasViewState)
        assertEquals("https://wd.example.invalid/Konto/Logowanie.aspx?ReturnUrl=%2f", form.actionUrl.toString())
        assertEquals("ctl00\$Main\$txtLogin", form.usernameField)
        assertEquals("ctl00\$Main\$txtHaslo", form.passwordField)
        assertEquals("ctl00\$Main\$btnZaloguj" to "Zaloguj", form.submitButton)

        val body = form.toLoginBody("12345", "tajne")
        val posted = (0 until body.size).associate { body.name(it) to body.value(it) }
        assertEquals("dDwtMTA4MTc=", posted["__VIEWSTATE"])
        assertEquals("C2EE9ABB", posted["__VIEWSTATEGENERATOR"])
        assertEquals("/wEWBALx", posted["__EVENTVALIDATION"])
        assertEquals("", posted["__EVENTTARGET"])
        assertEquals("12345", posted["ctl00\$Main\$txtLogin"])
        assertEquals("tajne", posted["ctl00\$Main\$txtHaslo"])
        assertEquals("Zaloguj", posted["ctl00\$Main\$btnZaloguj"])
        // Only the clicked button and no unchecked checkbox may be posted (EVENTVALIDATION).
        assertTrue("ctl00\$Main\$btnPrzypomnij" !in posted)
        assertTrue("ctl00\$Main\$chkPamietaj" !in posted)
        assertTrue(Buffer().also { body.writeTo(it) }.readUtf8().contains("__VIEWSTATE="))
    }

    @Test
    fun `detects LinkButton postback login`() {
        val html = """
            <form action="Default.aspx" method="post">
              <input type="hidden" name="__VIEWSTATE" value="x" />
              <input type="text" name="tbUser" /><input type="password" name="tbPass" />
              <a id="lbZaloguj" href="javascript:__doPostBack('lbZaloguj','')">Zaloguj się</a>
            </form>
        """.trimIndent()
        val form = WebFormsFormParser.parseLoginForm(Jsoup.parse(html), "https://wd.example.invalid/".toHttpUrl())!!
        assertEquals("lbZaloguj" to "", form.postBackTarget)
        val body = form.toLoginBody("a", "b")
        val posted = (0 until body.size).associate { body.name(it) to body.value(it) }
        assertEquals("lbZaloguj", posted["__EVENTTARGET"])
    }
}
