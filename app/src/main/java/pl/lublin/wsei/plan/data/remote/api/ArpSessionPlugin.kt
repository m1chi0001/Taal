package pl.lublin.wsei.plan.data.remote.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

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
 * Thread-safe Session & Token Manager (`PaTokenManager` equivalent) for
 * APR System Wirtualny Dziekanat (`https://dziekanat.wsei.lublin.pl:20830/`).
 */
@Singleton
class ArpSessionCookieJar @Inject constructor() : CookieJar {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

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

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
        synchronized(hostCookies) {
            for (newCookie in cookies) {
                hostCookies.removeAll { it.name == newCookie.name }
                hostCookies.add(newCookie)
                if (newCookie.name.equals("ASP.NET_SessionId", ignoreCase = true)) {
                    aspNetSessionId = newCookie.value
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val hostCookies = cookieStore[url.host] ?: return emptyList()
        val now = System.currentTimeMillis()
        synchronized(hostCookies) {
            hostCookies.removeAll { it.expiresAt < now }
            return hostCookies.toList()
        }
    }

    fun exportCookieHeader(host: String = "dziekanat.wsei.lublin.pl"): Map<String, String> {
        val hostCookies = cookieStore[host].orEmpty()
        return hostCookies.associate { it.name to it.value }
    }

    fun clearSession() {
        cookieStore.clear()
        bearerToken = null
        refreshToken = null
        savedLogin = null
        savedPassword = null
        aspNetSessionId = null
    }
}

/**
 * OkHttp Interceptor replicating `MobilnyStudent.ApiPa.Http.Handlers.HttpAuthorizationHandler`
 * and `ApiClient.AppendUrlSuffix`:
 * - Attaches `MS-API-KEY` header (`AprMsApiKeyGenerator.generateMsApiKey()`)
 * - Attaches `Authorization: Bearer {AccessToken}`
 * - Appends `TwoLetterISOLanguageName=pl&ClientVersion=latest` to Wirtualny Dziekanat API calls
 * - Automatically refreshes the token via `/api/MobileAuth/Authenticate` on HTTP 401
 */
class ArpAutoReauthInterceptor(
    private val cookieJar: ArpSessionCookieJar,
    private val tokenRefreshProvider: suspend () -> Boolean
) : Interceptor {

    private val reauthMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlWithSuffix = originalRequest.url.newBuilder().apply {
            if (originalRequest.url.queryParameter("TwoLetterISOLanguageName") == null) {
                addQueryParameter("TwoLetterISOLanguageName", "pl")
            }
            if (originalRequest.url.queryParameter("ClientVersion") == null) {
                addQueryParameter("ClientVersion", "latest")
            }
        }.build()

        val authenticatedRequest = originalRequest.newBuilder()
            .url(urlWithSuffix)
            .header("Accept", "application/json")
            .header(AprMsApiKeyGenerator.HEADER_NAME, AprMsApiKeyGenerator.generateMsApiKey())
            .apply {
                cookieJar.bearerToken?.let { token ->
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        val initialResponse = chain.proceed(authenticatedRequest)

        if (initialResponse.code == 401) {
            initialResponse.close()

            val refreshed = kotlinx.coroutines.runBlocking {
                reauthMutex.withLock {
                    tokenRefreshProvider()
                }
            }

            if (refreshed) {
                val retriedRequest = originalRequest.newBuilder()
                    .url(urlWithSuffix)
                    .header("Accept", "application/json")
                    .header(AprMsApiKeyGenerator.HEADER_NAME, AprMsApiKeyGenerator.generateMsApiKey())
                    .apply {
                        cookieJar.bearerToken?.let { token ->
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .build()
                return chain.proceed(retriedRequest)
            }
        }

        return initialResponse
    }
}
