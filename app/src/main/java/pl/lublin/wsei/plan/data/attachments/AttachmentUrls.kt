package pl.lublin.wsei.plan.data.attachments

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * URL helpers for attachment links and redirects: resolving relative `Location` headers and
 * list-entry paths against the dean's office base URL, and deciding which hosts may receive the
 * session credentials.
 */
object AttachmentUrls {

    private val BASE64_ONLY = Regex("^[A-Za-z0-9+/=\\s]+$")

    /**
     * Resolves [reference] (absolute, protocol-relative, root-relative, relative, ASP.NET `~/…`)
     * against [base]. Returns null for non-HTTP schemes (`javascript:`, `mailto:`, `file:`) or garbage.
     */
    fun resolve(base: String, reference: String?): String? {
        val ref = reference?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val cleaned = when {
            ref.startsWith("~/") -> ref.substring(1)
            else -> ref
        }.replace('\\', '/')
        val scheme = SCHEME.find(cleaned)?.groupValues?.get(1)?.lowercase()
        if (scheme != null && scheme != "http" && scheme != "https") return null
        val baseUrl = base.toHttpUrlOrNull() ?: return null
        return baseUrl.resolve(cleaned)?.toString()
    }

    /** True when [url] points at [baseHost] (port ignored: the API and the web portal share a host). */
    fun isSameHost(url: String?, baseHost: String): Boolean {
        val host = url?.toHttpUrlOrNull()?.host ?: return false
        return host.equals(baseHost, ignoreCase = true)
    }

    fun hostOf(url: String?): String? = url?.toHttpUrlOrNull()?.host

    /**
     * True when a string field from the API looks like a link rather than Base64 content:
     * an http(s) URL, a protocol-relative URL, or a path containing characters Base64 never has.
     */
    fun looksLikeUrl(value: String?): Boolean {
        val v = value?.trim()?.takeIf { it.isNotEmpty() && it.length <= 4096 } ?: return false
        if (v.startsWith("http://", ignoreCase = true) || v.startsWith("https://", ignoreCase = true)) return true
        if (v.any { it.isWhitespace() }) return false
        if (v.startsWith("data:", ignoreCase = true)) return false // inline content, not a link
        if (v.startsWith("~/")) return true
        // "Files/a.pdf", "/Download.ashx?id=1": characters Base64 never contains.
        if (!BASE64_ONLY.matches(v)) return v.contains('/')
        // "/Files/Attachment/12" is Base64-safe too, but real file content is far longer
        // (the only common Base64 file that starts with '/' is JPEG, "/9j/…", always > 1 KB).
        return v.startsWith("/") && v.length < 1024
    }

    /** Path + parameter names only, for diagnostics: never values (ids) or the host's credentials. */
    fun describeForLog(url: String?): String {
        val parsed: HttpUrl = url?.toHttpUrlOrNull() ?: return "<invalid url>"
        val params = parsed.queryParameterNames.joinToString(",")
        return parsed.host + ":" + parsed.port + parsed.encodedPath + if (params.isEmpty()) "" else "?[$params]"
    }

    private val SCHEME = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")
}
