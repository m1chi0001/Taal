package pl.lublin.wsei.plan.data.attachments

import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Extracts the file name from a `Content-Disposition` header (RFC 6266).
 *
 * Supports `filename*=UTF-8''%C5%BC%C3%B3%C5%82w.pdf` (RFC 5987, preferred), quoted and unquoted
 * `filename=`, backslash escapes inside quotes, percent-encoded plain `filename=` (ASP.NET
 * `FileResult` encodes non-ASCII names that way) and UTF-8 names that arrived as Latin-1 mojibake.
 */
object ContentDispositionParser {

    /** Returns the decoded file name, or null when the header has none. */
    fun fileName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val params = parseParameters(header)
        val extended = params["filename*"]?.let(::decodeExtendedValue)?.takeIf { it.isNotBlank() }
        if (extended != null) return extended.trim()
        val plain = params["filename"]?.takeIf { it.isNotBlank() } ?: return null
        return decodePlainValue(plain).trim().takeIf { it.isNotEmpty() }
    }

    /** Parses `type; key=value; key="quoted; value"` into a lower-cased key → raw value map. */
    internal fun parseParameters(header: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var i = header.indexOf(';').takeIf { it >= 0 } ?: return result
        i++
        val n = header.length
        while (i < n) {
            while (i < n && (header[i] == ' ' || header[i] == '\t' || header[i] == ';')) i++
            val keyStart = i
            while (i < n && header[i] != '=' && header[i] != ';') i++
            val key = header.substring(keyStart, i).trim().lowercase()
            if (i >= n || header[i] == ';') {
                continue
            }
            i++ // '='
            while (i < n && (header[i] == ' ' || header[i] == '\t')) i++
            val value: String
            if (i < n && header[i] == '"') {
                i++
                val sb = StringBuilder()
                while (i < n && header[i] != '"') {
                    if (header[i] == '\\' && i + 1 < n) i++
                    sb.append(header[i])
                    i++
                }
                i++ // closing quote
                value = sb.toString()
                while (i < n && header[i] != ';') i++
            } else {
                val valueStart = i
                while (i < n && header[i] != ';') i++
                value = header.substring(valueStart, i).trim()
            }
            if (key.isNotEmpty() && key !in result) result[key] = value
        }
        return result
    }

    /** `charset'lang'percent-encoded` (RFC 5987); tolerates a missing charset/lang part. */
    private fun decodeExtendedValue(raw: String): String? {
        val value = raw.trim().trim('"')
        val parts = value.split('\'', limit = 3)
        val (charsetName, encoded) = if (parts.size == 3) parts[0] to parts[2] else "UTF-8" to value
        val charset = runCatching { Charset.forName(charsetName.ifBlank { "UTF-8" }) }.getOrNull()
            ?: Charsets.UTF_8
        return percentDecode(encoded, charset)
    }

    private fun decodePlainValue(raw: String): String {
        val decoded = if (PERCENT_ESCAPE.containsMatchIn(raw)) {
            percentDecode(raw, Charsets.UTF_8) ?: raw
        } else {
            raw
        }
        return repairLatin1Mojibake(decoded)
    }

    private fun percentDecode(value: String, charset: Charset): String? = try {
        // URLDecoder would turn '+' into a space; a literal '+' is legal in RFC 5987 values.
        URLDecoder.decode(value.replace("+", "%2B"), charset.name())
    } catch (e: IllegalArgumentException) {
        null
    }

    /**
     * HTTP header values are decoded as ISO-8859-1, so a raw UTF-8 name ("Zał.pdf") arrives as
     * "ZaÅ\u0082.pdf". If re-encoding as Latin-1 gives valid UTF-8, that's the real name.
     */
    internal fun repairLatin1Mojibake(value: String): String {
        if (value.none { it.code in 0x80..0xFF }) return value
        if (value.any { it.code > 0xFF }) return value
        val bytes = value.toByteArray(Charsets.ISO_8859_1)
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            value
        }
    }

    private val PERCENT_ESCAPE = Regex("%[0-9A-Fa-f]{2}")
}
