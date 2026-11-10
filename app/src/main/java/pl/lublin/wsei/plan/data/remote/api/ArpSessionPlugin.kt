package pl.lublin.wsei.plan.data.remote.api

import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock

/**
 * Generates the daily dynamic `MS-API-KEY` header required by APR System's
 * Wirtualny Dziekanat (`MobilnyStudent.ApiPa` / `APRSystem.MobilnyStudent.Web.Core.Shared.Const.ConstantShared`).
 *
 * Extracted from `ConstantShared.GetMsApiKey()` in `com.APRSystem.MobilnyStudent.Mobile`:
 * Given `s = DateTime.UtcNow.Date.ToString("yyyyMMdd")`:
 * - `part1 = "${s[7]}${s[2]}${s[3]}"`
 * - `part2 = "${s[1]}${s[6]}"`
 * - `part3 = "${s[0]}${s[5]}${s[4]}"`
 * - `plain = "${part1}aM@zoN^5kA${part2}d7*Un6La${part3}"`
 * - `MS-API-KEY = MD5(plain)` (32-char uppercase hex)
 */
object AprMsApiKeyGenerator {
    const val HEADER_NAME = "MS-API-KEY"
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun generateMsApiKey(utcDate: LocalDate = LocalDate.now(ZoneOffset.UTC)): String {
        val s = utcDate.format(DATE_FORMATTER) // 8 chars: yyyyMMdd
        val part1 = "${s[7]}${s[2]}${s[3]}"
        val part2 = "${s[1]}${s[6]}"
        val part3 = "${s[0]}${s[5]}${s[4]}"
        val plain = "${part1}aM@zoN^5kA${part2}d7*Un6La${part3}"
        return md5Uppercase(plain)
    }

    fun generateExternalAuthKey(utcDate: LocalDate = LocalDate.now(ZoneOffset.UTC)): String {
        val s = utcDate.format(DATE_FORMATTER)
        val part1 = "${s[6]}${s[3]}${s[4]}"
        val part2 = "${s[2]}${s[7]}"
        val part3 = "${s[0]}${s[5]}${s[1]}"
        val plain = "${part1}pU5tvN1@${part2}s4hAr@${part3}"
        return md5Uppercase(plain)
    }

    private fun md5Uppercase(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }
}

/**
 * Session state for APR System Wirtualny Dziekanat (`https://dziekanat.wsei.lublin.pl:20830/`):
 * bearer token, in-memory credentials for silent re-authentication, and an RFC 6265-aware cookie jar
 * (`ASP.NET_SessionId`, `.AspNetCore.*`, load-balancer affinity cookies).
 *
 * Fixes vs. the previous version:
 * - cookies are matched with [Cookie.matches] (domain/path/secure) instead of only by exact host,
 * - cookies that the server expires (`Max-Age=0`) are removed instead of being kept and re-sent,
 * - a cookie is replaced only when name + domain + path are equal (two paths can share a name).
 */
@Singleton
class ArpSessionCookieJar @Inject constructor() : CookieJar {

    private val lock = Any()
    private val cookies = mutableListOf<Cookie>()

    @Volatile
    var bearerToken: String? = null

    @Volatile
    var refreshToken: String? = null

    @Volatile
    var savedLogin: String? = null

    @Volatile
    var savedPassword: String? = null

    @Volatile
    var aspNetSessionId: String? = null
        private set

    val hasCredentials: Boolean
        get() = !savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            for (incoming in cookies) {
                this.cookies.removeAll {
                    it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path
                }
                if (incoming.expiresAt > now) {
                    this.cookies += incoming
                }
                if (incoming.name.equals("ASP.NET_SessionId", ignoreCase = true)) {
                    aspNetSessionId = incoming.value.takeIf { incoming.expiresAt > now }
                }
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

    fun exportCookieHeader(host: String = "dziekanat.wsei.lublin.pl"): Map<String, String> =
        synchronized(lock) {
            cookies.filter { host == it.domain || host.endsWith(".${it.domain}") }
                .associate { it.name to it.value }
        }

    fun clearCookies() {
        synchronized(lock) { cookies.clear() }
        aspNetSessionId = null
    }

    fun clearSession() {
        clearCookies()
        bearerToken = null
        refreshToken = null
        savedLogin = null
        savedPassword = null
    }
}

/**
 * Silent re-authentication (`PaTokenManager` equivalent): replays `POST /api/MobileAuth/Authenticate`
 * with the in-memory credentials and stores the new bearer token.
 *
 * Runs on its **own** OkHttp client without [ArpAutoReauthInterceptor], so a 401 during refresh can't
 * re-enter the interceptor (the old `Mutex` + `runBlocking` design deadlocked in that case) and it
 * doesn't need a coroutine bridge. A [ReentrantLock] serialises concurrent refreshes; requests that
 * failed with an already-replaced token just retry with the new one (no refresh storm when the six
 * dashboard modules hit 401 at the same time).
 */
@Singleton
class ArpTokenRefresher @Inject constructor(
    private val cookieJar: ArpSessionCookieJar,
    private val json: Json
) {
    /** Called (on an OkHttp thread) after a successful silent re-login. */
    @Volatile
    var onTokenRefreshed: ((MobileAuthResponseDto) -> Unit)? = null

    private val lock = ReentrantLock()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * @param staleToken the token the failed request was sent with.
     * @return true when a token different from [staleToken] is available afterwards.
     */
    fun refreshBlocking(staleToken: String?): Boolean {
        lock.withLock { return refreshLocked(staleToken) }
    }

    private fun refreshLocked(staleToken: String?): Boolean {
        val current = cookieJar.bearerToken
        if (current != null && current != staleToken) return true // another thread already refreshed

        val login = cookieJar.savedLogin?.takeIf { it.isNotBlank() } ?: return false
        val password = cookieJar.savedPassword?.takeIf { it.isNotEmpty() } ?: return false

        val requestJson = json.encodeToString(
            MobileAuthRequestDto.serializer(),
            MobileAuthRequestDto(userNameOrEmailAddress = login, password = password)
        )
        val request = Request.Builder()
            .url(ArpAutoReauthInterceptor.withApiSuffix(AUTH_URL.toHttpUrl()))
            .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header(AprMsApiKeyGenerator.HEADER_NAME, AprMsApiKeyGenerator.generateMsApiKey())
            .build()

        val auth = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val text = response.body?.string().orEmpty()
                AbpEnvelope.decode(json, text, MobileAuthResponseDto.serializer())
            }
        }.getOrNull() ?: return false

        val token = auth.accessToken?.takeIf { it.isNotBlank() } ?: return false
        if (auth.requiresTwoFactorVerification || auth.shouldResetPassword) return false
        cookieJar.bearerToken = token
        runCatching { onTokenRefreshed?.invoke(auth) }
        return true
    }

    companion object {
        const val AUTH_PATH = "/api/MobileAuth/Authenticate"
        val AUTH_URL = "${ArpApiDataSource.WSEI_WD_BASE_URL}$AUTH_PATH"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/**
 * OkHttp Interceptor replicating `MobilnyStudent.ApiPa.Http.Handlers.HttpAuthorizationHandler`
 * and `ApiClient.AppendUrlSuffix`:
 * - attaches `MS-API-KEY`, `Authorization: Bearer …`, `Accept: application/json` and
 *   `X-Requested-With: XMLHttpRequest` (the latter makes ABP answer 401 JSON instead of a 302 to the
 *   MVC login page),
 * - appends `TwoLetterISOLanguageName=pl&ClientVersion=latest`,
 * - detects an expired session (HTTP 401 or a 3xx to a login page — redirects are not followed for the
 *   API), re-authenticates once via [ArpTokenRefresher] and replays the request with the new token,
 * - if re-authentication is impossible, returns a clean 401 so the caller raises
 *   [AprSessionExpiredException] instead of trying to parse HTML.
 */
class ArpAutoReauthInterceptor(
    private val cookieJar: ArpSessionCookieJar,
    private val tokenRefresher: ArpTokenRefresher
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = withApiSuffix(original.url)
        val isAuthCall = original.url.encodedPath.endsWith(ArpTokenRefresher.AUTH_PATH, ignoreCase = true) ||
            original.header(HEADER_SKIP_REAUTH) != null

        val tokenUsed = cookieJar.bearerToken
        val firstResponse = chain.proceed(decorate(original, url, tokenUsed, isAuthCall))
        if (isAuthCall || !isSessionExpired(firstResponse)) return firstResponse

        firstResponse.close()
        val refreshed = tokenRefresher.refreshBlocking(staleToken = tokenUsed)
        if (!refreshed) {
            return Response.Builder()
                .request(original)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Session expired and silent re-authentication failed")
                .body("".toResponseBody(null))
                .build()
        }
        return chain.proceed(decorate(original, url, cookieJar.bearerToken, isAuthCall = false))
    }

    private fun decorate(original: Request, url: HttpUrl, token: String?, isAuthCall: Boolean): Request =
        original.newBuilder()
            .url(url)
            .removeHeader(HEADER_SKIP_REAUTH)
            .header("Accept", "application/json")
            .header("X-Requested-With", "XMLHttpRequest")
            .header(AprMsApiKeyGenerator.HEADER_NAME, AprMsApiKeyGenerator.generateMsApiKey())
            .apply {
                if (!isAuthCall && !token.isNullOrBlank()) header("Authorization", "Bearer $token")
                else removeHeader("Authorization")
            }
            .build()

    private fun isSessionExpired(response: Response): Boolean = when {
        response.code == 401 -> true
        response.code in 300..399 -> AbpEnvelope.isLoginRedirect(response.header("Location"))
        else -> false
    }

    companion object {
        /** Add to a request to skip the re-auth loop (used by the login call itself). */
        const val HEADER_SKIP_REAUTH = "X-Taal-Skip-Reauth"

        fun withApiSuffix(url: HttpUrl): HttpUrl = url.newBuilder().apply {
            if (url.queryParameter("TwoLetterISOLanguageName") == null) {
                addQueryParameter("TwoLetterISOLanguageName", "pl")
            }
            if (url.queryParameter("ClientVersion") == null) {
                addQueryParameter("ClientVersion", "latest")
            }
        }.build()
    }
}
