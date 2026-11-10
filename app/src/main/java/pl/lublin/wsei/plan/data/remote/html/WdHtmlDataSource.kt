package pl.lublin.wsei.plan.data.remote.html

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import pl.lublin.wsei.plan.data.remote.api.ArpSessionCookieJar
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.repository.TimetableDataSource
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Web portal addresses. **Not verified against the live server** (it isn't reachable from the build
 * environment) — open the web Wirtualny Dziekanat in a browser, check the login URL and the timetable
 * page, and adjust here. Discovery also follows a "Plan zajęć" link after login, so only [baseUrl]
 * must be right for the common case.
 */
data class WdHtmlEndpoints(
    val baseUrl: String = "https://dziekanat.wsei.lublin.pl/",
    /** Page that shows (or redirects to) the login form. */
    val loginPath: String = "",
    /** Known timetable paths tried before link discovery. Empty = discover from navigation. */
    val timetablePaths: List<String> = emptyList(),
    val maxWeekPages: Int = 12
)

/**
 * Fallback [TimetableDataSource]: logs in through the ASP.NET web form (WebForms `__VIEWSTATE` /
 * `__EVENTVALIDATION` or MVC `__RequestVerificationToken`) and parses the timetable DOM with Jsoup.
 *
 * Uses its own OkHttp client and cookie jar (`ASP.NET_SessionId`, `.ASPXAUTH` / `.AspNetCore.Cookies`),
 * independent of the REST API session. Credentials are taken from [ArpSessionCookieJar] (in memory,
 * set at login or restored from the encrypted vault).
 */
@Singleton
class WdHtmlDataSource @Inject constructor(
    private val apiSession: ArpSessionCookieJar
) : TimetableDataSource {

    var endpoints: WdHtmlEndpoints = WdHtmlEndpoints()

    override val sourceOrigin: DataSourceOrigin = DataSourceOrigin.HTML_FALLBACK

    private val cookieJar = InMemoryCookieJar()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "pl-PL,pl;q=0.9")
                        .build()
                )
            }
            .build()
    }

    @Volatile
    private var timetableUrl: HttpUrl? = null

    private data class Page(val url: HttpUrl, val document: Document)

    override suspend fun authenticate(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String?
    ): Result<StudentSession> = resultOf {
        login(usernameOrAlbum.trim(), password)
        StudentSession(
            userId = 0L,
            personId = 0,
            studId = 0,
            login = usernameOrAlbum.trim(),
            firstName = "",
            lastName = "",
            albumNumber = usernameOrAlbum.trim(),
            fieldOfStudy = "",
            accessToken = "",
            isDebugAccount = false
        )
    }

    override suspend fun fetchSchedule(
        session: StudentSession,
        from: LocalDate,
        to: LocalDate,
        scheduleType: ScheduleFilterType
    ): Result<List<ClassEvent>> = resultOf {
        val login = apiSession.savedLogin ?: session.login
        val password = apiSession.savedPassword
            ?: throw IllegalStateException("Brak zapisanych danych logowania dla trybu awaryjnego.")
        val javaFrom = java.time.LocalDate.of(from.year, from.monthNumber, from.dayOfMonth)
        val javaTo = java.time.LocalDate.of(to.year, to.monthNumber, to.dayOfMonth)

        var page = openTimetablePage()
        if (page == null || isLoginPage(page.document)) {
            login(login, password)
            page = openTimetablePage()
                ?: throw IOException("Nie znaleziono strony planu zajęć w Wirtualnym Dziekanacie WWW.")
        }

        page = applyDateFilter(page, javaFrom, javaTo) ?: page

        val collected = linkedMapOf<String, ClassEvent>()
        var current: Page = page
        for (pageIndex in 0 until endpoints.maxWeekPages) {
            val events = TimetableHtmlParser.parse(current.document.outerHtml(), current.url.toString())
            events.forEach { collected.putIfAbsent(it.id, it) }
            val latest = events.maxOfOrNull { java.time.LocalDate.parse(it.dateIso) }
            if (latest != null && !latest.isBefore(javaTo)) break
            current = nextWeek(current) ?: break
        }
        collected.values
            .filter { java.time.LocalDate.parse(it.dateIso).let { d -> !d.isBefore(javaFrom) && !d.isAfter(javaTo) } }
            .sortedBy { it.timeSlot.startEpochMillis }
    }

    fun clearSession() {
        cookieJar.clear()
        timetableUrl = null
    }

    // ---------------------------------------------------------------------------------------------

    private suspend fun login(login: String, password: String) {
        cookieJar.clear()
        val base = endpoints.baseUrl.toHttpUrl()
        val entry = base.resolve(endpoints.loginPath) ?: base
        val loginPage = get(entry)
        val form = WebFormsFormParser.parseLoginForm(loginPage.document, loginPage.url)
            ?: throw IOException("Nie znaleziono formularza logowania na ${loginPage.url}.")
        if (form.usernameField == null || form.passwordField == null) {
            throw IOException("Formularz logowania ma nieznaną strukturę (brak pola loginu lub hasła).")
        }
        val result = post(form.actionUrl, form.toLoginBody(login, password))
        if (isLoginPage(result.document)) {
            val message = WebFormsFormParser.extractLoginError(result.document)
                ?: "Logowanie do Wirtualnego Dziekanatu WWW nie powiodło się."
            throw IllegalStateException(message)
        }
        timetableUrl = discoverTimetableUrl(result)
    }

    private suspend fun openTimetablePage(): Page? {
        val base = endpoints.baseUrl.toHttpUrl()
        val candidates = buildList {
            timetableUrl?.let { add(it) }
            endpoints.timetablePaths.mapNotNullTo(this) { base.resolve(it) }
        }.distinct()
        for (url in candidates) {
            val page = get(url)
            if (isLoginPage(page.document)) return page
            if (hasTimetableTable(page)) {
                timetableUrl = page.url
                return page
            }
        }
        if (cookieJar.isEmpty()) return null
        val home = get(base)
        if (isLoginPage(home.document)) return home
        val discovered = discoverTimetableUrl(home) ?: return null
        return get(discovered).also { timetableUrl = it.url }
    }

    private fun discoverTimetableUrl(page: Page): HttpUrl? {
        val link = page.document.select("a[href]").firstOrNull {
            TIMETABLE_LINK.containsMatchIn(it.text()) && !it.attr("href").startsWith("javascript", ignoreCase = true)
        } ?: return null
        return page.url.resolve(link.attr("href"))
    }

    /** Fills "data od / data do" inputs when the page has them, then posts the filter. */
    private suspend fun applyDateFilter(page: Page, from: java.time.LocalDate, to: java.time.LocalDate): Page? {
        val form = WebFormsFormParser.parseMainForm(page.document, page.url) ?: return null
        val fromField = form.fields.keys.firstOrNull { DATE_FROM_FIELD.containsMatchIn(it) } ?: return null
        val toField = form.fields.keys.firstOrNull { DATE_TO_FIELD.containsMatchIn(it) && it != fromField } ?: return null
        val format = if (form.fields[fromField].orEmpty().contains('-')) ISO_DATE else PL_DATE
        val button = page.document.select("input[type=submit][name], button[name]")
            .firstOrNull { FILTER_BUTTON.containsMatchIn(it.attr("value") + it.text()) }
        val values = LinkedHashMap(form.fields).apply {
            put(fromField, from.format(format))
            put(toField, to.format(format))
            button?.let { put(it.attr("name"), it.attr("value").ifEmpty { it.text() }) }
        }
        val body = FormBody.Builder().apply { values.forEach { (k, v) -> add(k, v) } }.build()
        return runCatching { post(form.actionUrl, body) }.getOrNull()?.takeIf { hasTimetableTable(it) }
    }

    /** Follows a "następny tydzień" link or WebForms LinkButton postback. */
    private suspend fun nextWeek(page: Page): Page? {
        val control = page.document.select("a, input[type=submit], button")
            .firstOrNull { NEXT_WEEK.containsMatchIn(it.text() + " " + it.attr("value") + " " + it.attr("title")) }
            ?: return null
        val href = control.attr("href")
        val postBack = POSTBACK.find(href + " " + control.attr("onclick"))
        return when {
            postBack != null -> {
                val form = WebFormsFormParser.parseMainForm(page.document, page.url) ?: return null
                post(form.actionUrl, form.toPostBackBody(postBack.groupValues[1], postBack.groupValues[2]))
            }
            href.isNotBlank() && !href.startsWith("#") && !href.startsWith("javascript", true) ->
                page.url.resolve(href)?.let { get(it) }
            control.hasAttr("name") -> {
                val form = WebFormsFormParser.parseMainForm(page.document, page.url) ?: return null
                val values = LinkedHashMap(form.fields).apply { put(control.attr("name"), control.attr("value")) }
                post(form.actionUrl, FormBody.Builder().apply { values.forEach { (k, v) -> add(k, v) } }.build())
            }
            else -> null
        }?.takeIf { hasTimetableTable(it) }
    }

    private fun hasTimetableTable(page: Page): Boolean =
        TimetableHtmlParser.parse(page.document.outerHtml(), page.url.toString()).isNotEmpty() ||
            page.document.select("th, td").any { it.text().trim().equals("Przedmiot", ignoreCase = true) }

    private fun isLoginPage(document: Document): Boolean =
        document.selectFirst("input[type=password]") != null

    private suspend fun get(url: HttpUrl): Page = execute(Request.Builder().url(url).get().build())

    private suspend fun post(url: HttpUrl, body: FormBody): Page =
        execute(Request.Builder().url(url).post(body).build())

    private suspend fun execute(request: Request): Page = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} z ${request.url}")
            val finalUrl = response.request.url
            val html = response.body?.string().orEmpty()
            Page(finalUrl, Jsoup.parse(html, finalUrl.toString()))
        }
    }

    private inline fun <T> resultOf(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36 Taal"
        private val TIMETABLE_LINK = Regex("(?i)(plan zaj|mój plan|moj plan|rozkład zaj|rozklad zaj|harmonogram)")
        private val NEXT_WEEK = Regex("(?i)(następn|nastepn|kolejn|>>|»|next)")
        private val FILTER_BUTTON = Regex("(?i)(pokaż|pokaz|filtruj|szukaj|wyświetl|wyswietl|zastosuj)")
        private val DATE_FROM_FIELD = Regex("(?i)(data_?od|dataod|datefrom|date_from|\\bod\\b|txtOd|dtOd)")
        private val DATE_TO_FIELD = Regex("(?i)(data_?do|datado|dateto|date_to|\\bdo\\b|txtDo|dtDo)")
        private val POSTBACK = Regex("""__doPostBack\(\s*['"]([^'"]*)['"]\s*,\s*['"]([^'"]*)['"]\s*\)""")
        private val PL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

/** Minimal RFC 6265 cookie jar for the web portal session. */
class InMemoryCookieJar : CookieJar {
    private val lock = Any()
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            for (c in cookies) {
                this.cookies.removeAll { it.name == c.name && it.domain == c.domain && it.path == c.path }
                if (c.expiresAt > now) this.cookies += c
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            cookies.removeAll { it.expiresAt <= now }
            return cookies.filter { it.matches(url) }
        }
    }

    fun isEmpty(): Boolean = synchronized(lock) { cookies.isEmpty() }

    fun clear() = synchronized(lock) { cookies.clear() }
}
