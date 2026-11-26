package pl.lublin.wsei.plan.data.attachments

/**
 * Lenient Base64 handling for attachment payloads.
 *
 * The dean's office API serializes C# `byte[]` as standard Base64, but proxies, older tenants and
 * hand-written handlers have been seen to add `data:<mime>;base64,` prefixes, line breaks, the
 * URL-safe alphabet (`-`/`_`) or to drop the `=` padding. `java.util.Base64` (API 26+) is used
 * instead of `android.util.Base64` so the logic is unit-testable on the JVM.
 */
object AttachmentBase64 {

    private val DATA_URI_PREFIX = Regex("^data:[^,]{0,200}?;base64,", RegexOption.IGNORE_CASE)
    private val WHITESPACE = Regex("\\s+")
    private val ESCAPED_NEWLINES = Regex("\\\\[rnt]")
    private val STANDARD_ALPHABET = Regex("^[A-Za-z0-9+/]*$")
    private val ANY_ALPHABET = Regex("^[A-Za-z0-9+/_\\-]*={0,2}$")

    /**
     * Returns canonical, padded standard Base64, or null when [raw] can't be Base64 at all
     * (illegal characters or an impossible length).
     */
    fun normalize(raw: String?): String? {
        if (raw == null) return null
        var text = raw.trim().removePrefix("\uFEFF")
        if (text.length >= 2 && text.first() == '"' && text.last() == '"') {
            text = text.substring(1, text.length - 1)
        }
        text = DATA_URI_PREFIX.replace(text, "")
        // Some handlers JSON-escape the line breaks of MIME Base64 twice ("\\r\\n").
        text = ESCAPED_NEWLINES.replace(text, "")
        text = WHITESPACE.replace(text, "")
        if (text.isEmpty()) return null
        if (!ANY_ALPHABET.matches(text)) return null
        text = text.trimEnd('=').replace('-', '+').replace('_', '/')
        if (!STANDARD_ALPHABET.matches(text)) return null
        return when (text.length % 4) {
            0 -> text
            2 -> "$text=="
            3 -> "$text="
            else -> null // a single dangling character can never be valid Base64
        }
    }

    /** Decodes [raw] leniently (see [normalize]); null when it isn't Base64 or decodes to nothing. */
    fun decode(raw: String?): ByteArray? {
        val normalized = normalize(raw) ?: return null
        return try {
            java.util.Base64.getDecoder().decode(normalized).takeIf { it.isNotEmpty() }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Heuristic used to tell a bare Base64 body (`text/plain` or a JSON string) from ordinary text:
     * long enough, only Base64 characters (after the lenient clean-up) and no spaces between words.
     */
    fun looksLikeBase64(text: String, minLength: Int = 8): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < minLength) return false
        if (DATA_URI_PREFIX.containsMatchIn(trimmed)) return true
        // Ordinary prose has spaces between words; MIME Base64 only has line breaks.
        if (trimmed.contains(' ')) return false
        return normalize(trimmed) != null
    }
}
