package pl.lublin.wsei.plan.data.attachments

/** What an attachment download response body actually contains. */
enum class AttachmentBodyKind {
    /** No bytes (or only whitespace). */
    EMPTY,

    /** JSON: ABP envelope, bare DTO, array, or a JSON string. */
    JSON,

    /** A bare Base64 text body (no JSON quotes). */
    BASE64_TEXT,

    /** An HTML page (login form, error page) — never a valid attachment. */
    HTML,

    /** The file itself. */
    BINARY
}

/**
 * Classifies a download response from its `Content-Type`, `Content-Disposition` and first bytes.
 * Magic bytes win over headers: servers often send PDFs as `text/html` or JSON as `text/plain`.
 */
object AttachmentResponseClassifier {

    private const val SAMPLE_SIZE = 2048
    private val HTML_START = Regex(
        "^(<!doctype\\s+html|<html|<head|<body|<meta|<title|<script|<form|<div|<table|<!--|<link|<style|<noscript|<center|<h1|<p>|<br)",
        RegexOption.IGNORE_CASE
    )
    private val HTML_ANYWHERE = Regex("<(html|body|form|head)[\\s>]", RegexOption.IGNORE_CASE)
    private val LOGIN_MARKERS = Regex(
        "(type\\s*=\\s*[\"']?password|<form[^>]{0,300}(login|logowanie|signin|account|konto)|" +
            "(zaloguj|logowanie|sign in|log in)\\s*</(title|h1|h2|button))",
        RegexOption.IGNORE_CASE
    )

    fun classify(contentType: String?, contentDisposition: String?, bytes: ByteArray): AttachmentBodyKind {
        if (bytes.isEmpty()) return AttachmentBodyKind.EMPTY
        val sniffed = AttachmentFileTypes.sniff(bytes)
        if (sniffed != null && sniffed != AttachmentFileTypes.TXT) return AttachmentBodyKind.BINARY

        val type = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        val text = textSample(bytes)
        if (text.isBlank()) return AttachmentBodyKind.EMPTY
        val first = text.first()

        if (looksLikeHtml(text)) {
            // An .html/.xml attachment explicitly sent as a download is still a file, not a page.
            val isDownload = contentDisposition?.contains("attachment", ignoreCase = true) == true &&
                ContentDispositionParser.fileName(contentDisposition)
                    ?.let { AttachmentFileNames.extensionOf(it) }
                    .let { it == "html" || it == "htm" || it == "xml" }
            return if (isDownload) AttachmentBodyKind.BINARY else AttachmentBodyKind.HTML
        }
        val jsonType = type.contains("json")
        if (first == '{' || first == '[' || first == '"') {
            // A .json/.csv attachment served as a download stays a file.
            if (!jsonType && contentDisposition?.contains("attachment", ignoreCase = true) == true &&
                ContentDispositionParser.fileName(contentDisposition) != null
            ) {
                return AttachmentBodyKind.BINARY
            }
            return AttachmentBodyKind.JSON
        }
        if (bytes.size >= 8 && isWholeBodyBase64(bytes)) return AttachmentBodyKind.BASE64_TEXT
        if (type == "text/html") return AttachmentBodyKind.HTML
        return AttachmentBodyKind.BINARY
    }

    fun looksLikeHtml(text: String): Boolean {
        val trimmed = text.trimStart()
        if (HTML_START.containsMatchIn(trimmed)) return true
        if (trimmed.startsWith("<?xml", ignoreCase = true)) return HTML_ANYWHERE.containsMatchIn(trimmed)
        return false
    }

    /** True when an HTML body is a login form (expired session) rather than an error page. */
    fun looksLikeLoginPage(bytes: ByteArray): Boolean =
        LOGIN_MARKERS.containsMatchIn(String(bytes, 0, minOf(bytes.size, 64 * 1024), Charsets.UTF_8))

    fun textSample(bytes: ByteArray): String {
        val sample = String(bytes, 0, minOf(bytes.size, SAMPLE_SIZE), Charsets.UTF_8)
        return sample.removePrefix("\uFEFF").trimStart()
    }

    private fun isWholeBodyBase64(bytes: ByteArray): Boolean {
        if (bytes.size <= LARGE_BODY) return AttachmentBase64.looksLikeBase64(String(bytes, Charsets.US_ASCII))
        // Large bodies: the alphabet of a prefix is enough (the decoder validates the rest).
        for (i in 0 until LARGE_BODY) {
            val c = (bytes[i].toInt() and 0xFF).toChar()
            val ok = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
                c == '+' || c == '/' || c == '=' || c == '-' || c == '_' || c == '\r' || c == '\n'
            if (!ok) return false
        }
        return true
    }

    private const val LARGE_BODY = 256 * 1024
}
