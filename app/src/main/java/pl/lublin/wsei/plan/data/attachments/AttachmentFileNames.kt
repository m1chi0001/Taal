package pl.lublin.wsei.plan.data.attachments

/** Final on-disk name and MIME type of a downloaded attachment. */
data class ResolvedAttachmentFile(val fileName: String, val mimeType: String)

/**
 * File naming for downloaded attachments: sanitizing server-provided names and making sure the
 * name ends with an extension that matches the content, because viewer apps pick a handler by
 * extension as often as by MIME type.
 */
object AttachmentFileNames {

    const val DEFAULT_BASE_NAME = "zalacznik"
    private const val MAX_NAME_LENGTH = 120
    private const val MAX_EXTENSION_LENGTH = 10

    private val ILLEGAL_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]")
    private val REPEATED_SPACES = Regex("\\s{2,}")
    private val EXTENSION = Regex("^[A-Za-z0-9]{1,$MAX_EXTENSION_LENGTH}$")

    /**
     * Strips any path, characters that are illegal on Android/FAT file systems, bidi overrides
     * (which can disguise "fdp.exe" as "exe.pdf") and leading/trailing dots; caps the length while
     * keeping the extension. Returns [fallback] for names that end up empty.
     */
    fun sanitize(raw: String?, fallback: String = DEFAULT_BASE_NAME): String {
        val cleaned = raw.orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(ILLEGAL_CHARS, "_")
            .replace(REPEATED_SPACES, " ")
            .trim()
            .trim('.', ' ')
        if (cleaned.isEmpty() || cleaned.all { it == '_' }) return fallback
        if (cleaned.length <= MAX_NAME_LENGTH) return cleaned
        val ext = extensionOf(cleaned)
        return if (ext == null) {
            cleaned.take(MAX_NAME_LENGTH).trimEnd('.', ' ')
        } else {
            cleaned.substringBeforeLast('.').take(MAX_NAME_LENGTH - ext.length - 1).trimEnd('.', ' ') + "." + ext
        }
    }

    /**
     * Extension of [name] (lower-case, without the dot) when it looks like a real one: 1–10
     * alphanumerics with at least one letter, so "Komunikat 12.2024" has none but "a.7z" has.
     */
    fun extensionOf(name: String?): String? {
        val ext = name?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() } ?: return null
        if (!EXTENSION.matches(ext) || ext.none { it.isLetter() }) return null
        if (name.length == ext.length + 1) return null // ".pdf" alone is a hidden name, not an extension
        return ext.lowercase()
    }

    /**
     * True for names that say nothing about the file: handler/action names ("GetAttachmentBytes",
     * "download.ashx"), placeholders ("file", "blob") or bare numbers/GUIDs.
     */
    fun isGenericName(name: String?): Boolean {
        val base = name?.trim()?.let { n -> extensionOf(n)?.let { n.dropLast(it.length + 1) } ?: n }
            ?.lowercase()?.trim() ?: return true
        if (base.isEmpty()) return true
        if (base in GENERIC_BASE_NAMES) return true
        if (base.all { it.isDigit() }) return true
        return GUID.matches(base)
    }

    /**
     * Orders name candidates by trust: the response's name (Content-Disposition / payload) unless it
     * is generic, then the name from the announcement list, then the URL's last segment.
     */
    fun candidates(responseName: String?, listName: String?, urlName: String?): List<String?> =
        if (!isGenericName(responseName)) {
            listOf(responseName, listName, urlName)
        } else {
            listOf(listName, urlName, responseName)
        }

    private val GENERIC_BASE_NAMES = setOf(
        "download", "file", "attachment", "getattachmentbytes", "getattachment", "getfile", "document",
        "plik", "zalacznik", "załącznik", "blob", "content", "data", "default", "index", "handler", "untitled"
    )
    private val GUID = Regex("^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$")

    /** Last path segment of a URL (without query/fragment), percent-decoded; null when it has none. */
    fun nameFromUrl(url: String?): String? {
        val path = url?.substringBefore('#')?.substringBefore('?')?.trimEnd('/') ?: return null
        val segment = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
        val decoded = runCatching { java.net.URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8") }
            .getOrDefault(segment)
        return decoded.takeIf { extensionOf(it) != null }
    }

    /**
     * Picks the final file name and MIME type.
     *
     * @param nameCandidates names in order of trust (Content-Disposition, API payload, list entry,
     *   URL); the first non-blank one is used, but a later one may contribute a missing extension.
     * @param bytes the content (or its first few KB) for magic-byte detection.
     * @param declaredMime `Content-Type` or the payload's MIME field; used only when it is specific.
     * @param fallbackBaseName base name when no candidate is usable (e.g. `zalacznik_123`).
     */
    fun resolve(
        nameCandidates: List<String?>,
        bytes: ByteArray,
        declaredMime: String? = null,
        fallbackBaseName: String = DEFAULT_BASE_NAME
    ): ResolvedAttachmentFile {
        val names = nameCandidates.mapNotNull { it?.takeIf { n -> n.isNotBlank() } }
        val primary = sanitize(names.firstOrNull(), fallbackBaseName)
        val sniffed = AttachmentFileTypes.sniff(bytes)
        val declared = declaredMime?.substringBefore(';')?.trim()?.lowercase()
            ?.takeIf { !AttachmentFileTypes.isGenericMime(it) && !it.startsWith("text/html") }

        val ownExt = extensionOf(primary)
        // A later candidate (e.g. the list entry name) may know the extension the header lacks.
        val borrowedExt = if (ownExt == null) names.drop(1).firstNotNullOfOrNull { extensionOf(it) } else null
        val nameExt = ownExt ?: borrowedExt

        val finalExt: String?
        val mime: String?
        when {
            // Strong signature that contradicts the name (e.g. "plan.pdf" holding a PNG): trust the bytes.
            sniffed != null && sniffed != AttachmentFileTypes.TXT &&
                (nameExt == null || !AttachmentFileTypes.isCompatible(sniffed, nameExt)) -> {
                finalExt = sniffed.extension
                mime = sniffed.mimeType
            }
            nameExt != null -> {
                finalExt = nameExt
                mime = AttachmentFileTypes.mimeForExtension(nameExt)
                    ?: sniffed?.takeIf { AttachmentFileTypes.isCompatible(it, nameExt) }?.mimeType
                    ?: declared
            }
            declared != null && AttachmentFileTypes.extensionForMime(declared) != null -> {
                finalExt = AttachmentFileTypes.extensionForMime(declared)
                mime = declared
            }
            sniffed != null -> { // plain text without any other hint
                finalExt = sniffed.extension
                mime = sniffed.mimeType
            }
            else -> {
                finalExt = null
                mime = declared
            }
        }

        val fileName = when {
            finalExt == null -> primary
            ownExt == finalExt -> primary
            ownExt != null && sniffed != null && finalExt == sniffed.extension ->
                primary.substringBeforeLast('.') + "." + finalExt
            else -> "$primary.$finalExt"
        }
        return ResolvedAttachmentFile(
            fileName = sanitize(fileName, fallbackBaseName),
            mimeType = mime ?: AttachmentFileTypes.OCTET_STREAM
        )
    }
}
