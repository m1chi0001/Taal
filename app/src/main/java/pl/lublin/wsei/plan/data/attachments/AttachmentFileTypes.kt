package pl.lublin.wsei.plan.data.attachments

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** A file type: canonical extension (without the dot) and MIME type. */
data class AttachmentFileType(val extension: String, val mimeType: String)

/**
 * File type detection for attachments: magic bytes first (the dean's office often serves files
 * with a missing or wrong extension), then a static extension → MIME table. Pure JVM, so the
 * Android-only `MimeTypeMap` stays an optional last resort in the opener.
 */
object AttachmentFileTypes {

    const val OCTET_STREAM = "application/octet-stream"

    val PDF = AttachmentFileType("pdf", "application/pdf")
    val PNG = AttachmentFileType("png", "image/png")
    val JPEG = AttachmentFileType("jpg", "image/jpeg")
    val GIF = AttachmentFileType("gif", "image/gif")
    val WEBP = AttachmentFileType("webp", "image/webp")
    val BMP = AttachmentFileType("bmp", "image/bmp")
    val TIFF = AttachmentFileType("tif", "image/tiff")
    val RTF = AttachmentFileType("rtf", "application/rtf")
    val ZIP = AttachmentFileType("zip", "application/zip")
    val RAR = AttachmentFileType("rar", "application/vnd.rar")
    val SEVEN_ZIP = AttachmentFileType("7z", "application/x-7z-compressed")
    val DOCX = AttachmentFileType("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    val XLSX = AttachmentFileType("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    val PPTX = AttachmentFileType("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation")
    val ODT = AttachmentFileType("odt", "application/vnd.oasis.opendocument.text")
    val ODS = AttachmentFileType("ods", "application/vnd.oasis.opendocument.spreadsheet")
    val ODP = AttachmentFileType("odp", "application/vnd.oasis.opendocument.presentation")
    val DOC = AttachmentFileType("doc", "application/msword")
    val XLS = AttachmentFileType("xls", "application/vnd.ms-excel")
    val PPT = AttachmentFileType("ppt", "application/vnd.ms-powerpoint")
    val TXT = AttachmentFileType("txt", "text/plain")

    /** Containers whose exact flavour is better told by a matching extension than by sniffing. */
    private val ZIP_FAMILY = setOf("zip", "docx", "docm", "xlsx", "xlsm", "pptx", "ppsx", "odt", "ods", "odp", "epub", "jar", "apk")
    private val OLE_FAMILY = setOf("doc", "dot", "xls", "xlt", "ppt", "pps", "msg", "vsd", "pub")
    private val TEXT_FAMILY = setOf("txt", "csv", "tsv", "log", "md", "xml", "json", "html", "htm", "ics", "vcf", "rtf")

    private val MIME_BY_EXTENSION: Map<String, String> = mapOf(
        "pdf" to PDF.mimeType,
        "png" to PNG.mimeType,
        "jpg" to JPEG.mimeType,
        "jpeg" to JPEG.mimeType,
        "jpe" to JPEG.mimeType,
        "gif" to GIF.mimeType,
        "webp" to WEBP.mimeType,
        "bmp" to BMP.mimeType,
        "tif" to TIFF.mimeType,
        "tiff" to TIFF.mimeType,
        "heic" to "image/heic",
        "svg" to "image/svg+xml",
        "rtf" to RTF.mimeType,
        "zip" to ZIP.mimeType,
        "rar" to RAR.mimeType,
        "7z" to SEVEN_ZIP.mimeType,
        "doc" to DOC.mimeType,
        "dot" to DOC.mimeType,
        "docx" to DOCX.mimeType,
        "docm" to "application/vnd.ms-word.document.macroEnabled.12",
        "xls" to XLS.mimeType,
        "xlt" to XLS.mimeType,
        "xlsx" to XLSX.mimeType,
        "xlsm" to "application/vnd.ms-excel.sheet.macroEnabled.12",
        "ppt" to PPT.mimeType,
        "pps" to PPT.mimeType,
        "pptx" to PPTX.mimeType,
        "ppsx" to "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        "odt" to ODT.mimeType,
        "ods" to ODS.mimeType,
        "odp" to ODP.mimeType,
        "txt" to TXT.mimeType,
        "log" to TXT.mimeType,
        "md" to "text/markdown",
        "csv" to "text/csv",
        "tsv" to "text/tab-separated-values",
        "xml" to "text/xml",
        "json" to "application/json",
        "html" to "text/html",
        "htm" to "text/html",
        "ics" to "text/calendar",
        "vcf" to "text/x-vcard",
        "epub" to "application/epub+zip",
        "mp3" to "audio/mpeg",
        "mp4" to "video/mp4",
        "msg" to "application/vnd.ms-outlook"
    )

    private val EXTENSION_BY_MIME: Map<String, String> = buildMap {
        // First extension listed for a MIME type wins (e.g. image/jpeg → jpg).
        MIME_BY_EXTENSION.forEach { (ext, mime) -> putIfAbsent(mime, ext) }
        put("application/x-pdf", "pdf")
        put("application/vnd.ms-word", "doc")
        put("application/x-zip-compressed", "zip")
        put("image/jpg", "jpg")
        put("image/pjpeg", "jpg")
        put("text/rtf", "rtf")
    }

    fun mimeForExtension(extension: String?): String? =
        extension?.lowercase()?.trim('.')?.let { MIME_BY_EXTENSION[it] }

    /** Canonical extension for a MIME type (parameters such as `; charset=` are ignored). */
    fun extensionForMime(mimeType: String?): String? {
        val bare = mimeType?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return EXTENSION_BY_MIME[bare]
    }

    /** True for MIME types that carry no information about the content. */
    fun isGenericMime(mimeType: String?): Boolean {
        val bare = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return bare.isEmpty() || bare == OCTET_STREAM || bare == "binary/octet-stream" ||
            bare == "application/force-download" || bare == "application/download" ||
            bare == "application/unknown" || bare == "*/*" || bare == "application/x-download"
    }

    /**
     * Detects the type from magic bytes. ZIP and OLE containers are told apart further by their
     * member names (`word/`, `xl/`, `ppt/`, ODF `mimetype`, OLE `WordDocument`/`Workbook` streams).
     * Plain text is reported only when the bytes are valid UTF-8 without binary control characters.
     */
    fun sniff(bytes: ByteArray): AttachmentFileType? {
        fun at(offset: Int, vararg sig: Int): Boolean =
            bytes.size >= offset + sig.size && sig.indices.all { (bytes[offset + it].toInt() and 0xFF) == sig[it] }

        val pdfOffset = indexOf(bytes, "%PDF-".toByteArray(Charsets.US_ASCII), limit = 1024)
        return when {
            pdfOffset == 0 -> PDF
            at(0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> PNG
            at(0, 0xFF, 0xD8, 0xFF) -> JPEG
            at(0, 0x47, 0x49, 0x46, 0x38) -> GIF // GIF8
            at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50) -> WEBP // RIFF....WEBP
            at(0, 0x42, 0x4D) && bytes.size > 26 && at(6, 0x00, 0x00, 0x00, 0x00) -> BMP
            at(0, 0x49, 0x49, 0x2A, 0x00) || at(0, 0x4D, 0x4D, 0x00, 0x2A) -> TIFF
            at(0, 0x7B, 0x5C, 0x72, 0x74, 0x66) -> RTF // {\rtf
            at(0, 0x50, 0x4B, 0x03, 0x04) || at(0, 0x50, 0x4B, 0x05, 0x06) -> sniffZip(bytes)
            at(0, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1) -> sniffOle(bytes)
            at(0, 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07) -> RAR // Rar!
            at(0, 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C) -> SEVEN_ZIP
            // Some generators prepend a few junk bytes (BOM, whitespace) before %PDF-.
            pdfOffset in 1..1023 -> PDF
            isPlainText(bytes) -> TXT
            else -> null
        }
    }

    /** True when the sniffed type is a container whose flavour [extension] can legitimately refine. */
    fun isCompatible(sniffed: AttachmentFileType, extension: String): Boolean {
        val ext = extension.lowercase()
        if (ext == sniffed.extension) return true
        return when (sniffed) {
            ZIP, DOCX, XLSX, PPTX, ODT, ODS, ODP -> ext in ZIP_FAMILY
            DOC, XLS, PPT -> ext in OLE_FAMILY
            TXT -> ext in TEXT_FAMILY || MIME_BY_EXTENSION[ext]?.startsWith("text/") == true
            JPEG -> ext == "jpeg" || ext == "jpe"
            TIFF -> ext == "tiff"
            else -> false
        }
    }

    private fun sniffZip(bytes: ByteArray): AttachmentFileType {
        // ODF: the first entry is an uncompressed "mimetype" file holding the MIME type.
        val head = String(bytes, 0, minOf(bytes.size, 256), Charsets.ISO_8859_1)
        if (head.contains("mimetype")) {
            when {
                head.contains("application/vnd.oasis.opendocument.text") -> return ODT
                head.contains("application/vnd.oasis.opendocument.spreadsheet") -> return ODS
                head.contains("application/vnd.oasis.opendocument.presentation") -> return ODP
            }
        }
        // OOXML: member names appear in local headers and in the central directory.
        return when {
            contains(bytes, "word/") -> DOCX
            contains(bytes, "xl/") -> XLSX
            contains(bytes, "ppt/") -> PPTX
            else -> ZIP
        }
    }

    private fun sniffOle(bytes: ByteArray): AttachmentFileType = when {
        containsUtf16(bytes, "WordDocument") -> DOC
        containsUtf16(bytes, "Workbook") || containsUtf16(bytes, "Book") -> XLS
        containsUtf16(bytes, "PowerPoint Document") -> PPT
        else -> DOC
    }

    private fun isPlainText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sample = if (bytes.size > 4096) bytes.copyOf(4096) else bytes
        var control = 0
        for (b in sample) {
            val v = b.toInt() and 0xFF
            if (v == 0) return false
            if (v < 0x20 && v != 0x09 && v != 0x0A && v != 0x0D && v != 0x0C) control++
        }
        if (control > sample.size / 100) return false
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(sample))
            true
        } catch (e: CharacterCodingException) {
            // A cut-off multi-byte sequence at the end of the sample is still text.
            sample.size == 4096 && runCatching {
                decoder.reset().decode(ByteBuffer.wrap(sample, 0, sample.size - 3))
            }.isSuccess
        }
    }

    private fun contains(bytes: ByteArray, needle: String): Boolean =
        indexOf(bytes, needle.toByteArray(Charsets.US_ASCII)) >= 0

    private fun containsUtf16(bytes: ByteArray, needle: String): Boolean =
        indexOf(bytes, needle.toByteArray(Charsets.UTF_16LE)) >= 0

    internal fun indexOf(bytes: ByteArray, needle: ByteArray, limit: Int = bytes.size): Int {
        if (needle.isEmpty()) return 0
        val last = minOf(bytes.size, limit + needle.size) - needle.size
        var i = 0
        outer@ while (i <= last) {
            for (j in needle.indices) {
                if (bytes[i + j] != needle[j]) {
                    i++
                    continue@outer
                }
            }
            return i
        }
        return -1
    }
}
