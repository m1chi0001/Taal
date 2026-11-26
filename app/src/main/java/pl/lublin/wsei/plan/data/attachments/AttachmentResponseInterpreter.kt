package pl.lublin.wsei.plan.data.attachments

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import pl.lublin.wsei.plan.data.remote.api.AbpEnvelope
import pl.lublin.wsei.plan.data.remote.api.AprMalformedResponseException

/** What a download response turned out to contain. */
sealed interface AttachmentPayload {
    /** The file content. Names/MIME are hints from the response (may be null). */
    class Bytes(val bytes: ByteArray, val fileName: String?, val mimeType: String?) : AttachmentPayload

    /** The server pointed to another address (JSON `url`, ABP `targetUrl`, a JSON string link). */
    data class Link(val url: String, val fileName: String?) : AttachmentPayload

    /** Valid response without any content (`result: null`, `fileBytes: ""`, empty body). */
    data class Empty(val fileName: String?) : AttachmentPayload

    /** An HTML page with status 200: the login page ([isLoginPage]) or an error page. */
    data class Html(val isLoginPage: Boolean) : AttachmentPayload

    /** A content field was present but isn't decodable Base64. */
    data object CorruptBase64 : AttachmentPayload
}

/**
 * Turns an attachment download response into an [AttachmentPayload], accepting every shape seen
 * from APR System deployments:
 * - ABP envelope `{result:{id,name,fileBytes|bytes|file|content|data|base64…}}`,
 * - a bare JSON object or array without the envelope, `byte[]` serialized as a number array,
 * - a JSON string holding Base64 (or a link), a bare Base64 text body,
 * - the raw file (any binary `Content-Type`), named by `Content-Disposition`,
 * - HTML pages, which are reported (never treated as a file).
 *
 * ABP failures (`unAuthorizedRequest`, `success:false`) are thrown as the usual
 * [pl.lublin.wsei.plan.data.remote.api.AprApiException]s.
 */
object AttachmentResponseInterpreter {

    private const val MAX_DEPTH = 4
    private const val MIN_CORRUPT_LENGTH = 64

    private val NAME_KEYS = listOf(
        "filename", "name", "originalfilename", "originalname", "displayname", "attachmentname", "title"
    )
    private val MIME_KEYS = listOf("contenttype", "mimetype", "mime", "filetype", "type")
    private val CONTENT_KEYS = listOf(
        "filebytes", "bytes", "filecontents", "filecontent", "filedata", "attachmentbytes", "base64",
        "content", "data", "file", "attachment", "document", "body", "value", "result", "item"
    )
    private val URL_KEYS = listOf("downloadurl", "fileurl", "url", "link", "href", "uri", "path", "filepath", "location")

    fun interpret(
        json: Json,
        contentType: String?,
        contentDisposition: String?,
        bytes: ByteArray
    ): AttachmentPayload {
        val headerName = ContentDispositionParser.fileName(contentDisposition)
        return when (AttachmentResponseClassifier.classify(contentType, contentDisposition, bytes)) {
            AttachmentBodyKind.EMPTY -> AttachmentPayload.Empty(headerName)
            AttachmentBodyKind.HTML -> AttachmentPayload.Html(AttachmentResponseClassifier.looksLikeLoginPage(bytes))
            AttachmentBodyKind.BINARY -> AttachmentPayload.Bytes(bytes, headerName, contentType)
            AttachmentBodyKind.BASE64_TEXT ->
                AttachmentBase64.decode(String(bytes, Charsets.US_ASCII))
                    ?.let { AttachmentPayload.Bytes(it, headerName, null) }
                    ?: AttachmentPayload.CorruptBase64
            AttachmentBodyKind.JSON -> {
                val element = try {
                    json.parseToJsonElement(String(bytes, Charsets.UTF_8).trim().removePrefix("\uFEFF"))
                } catch (e: SerializationException) {
                    throw AprMalformedResponseException("Attachment response is not valid JSON.", e)
                }
                fromJson(json, element).withFallbackName(headerName)
            }
        }
    }

    /** Interprets an already parsed JSON body (ABP envelope or bare). */
    fun fromJson(json: Json, element: JsonElement): AttachmentPayload {
        val target = AbpEnvelope.targetUrl(element)
        val unwrapped = AbpEnvelope.unwrapElement(element) // throws on unAuthorizedRequest / success:false
        val payload = extract(json, unwrapped, 0)
        if (payload is AttachmentPayload.Empty && target != null && AttachmentUrls.looksLikeUrl(target)) {
            return AttachmentPayload.Link(target, payload.fileName)
        }
        return payload
    }

    private fun extract(json: Json, element: JsonElement, depth: Int): AttachmentPayload {
        if (depth > MAX_DEPTH) return AttachmentPayload.Empty(null)
        return when (element) {
            is JsonNull -> AttachmentPayload.Empty(null)
            is JsonPrimitive -> fromString(json, element, depth)
            is JsonArray -> fromArray(json, element, depth)
            is JsonObject -> fromObject(json, element, depth)
        }
    }

    private fun fromString(json: Json, primitive: JsonPrimitive, depth: Int): AttachmentPayload {
        if (!primitive.isString) return AttachmentPayload.Empty(null)
        val text = primitive.content.trim()
        if (text.isEmpty()) return AttachmentPayload.Empty(null)
        // Double-encoded JSON ("{\"fileBytes\":…}").
        if ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))) {
            runCatching { json.parseToJsonElement(text) }.getOrNull()?.let { inner ->
                return extract(json, AbpEnvelope.unwrapElement(inner), depth + 1)
            }
        }
        if (AttachmentUrls.looksLikeUrl(text)) return AttachmentPayload.Link(text, null)
        if (!AttachmentBase64.looksLikeBase64(text)) {
            // Short prose ("Brak pliku", "plan.pdf") is not content; long non-Base64 text is broken content.
            return if (text.length < MIN_CORRUPT_LENGTH) AttachmentPayload.Empty(null) else AttachmentPayload.CorruptBase64
        }
        val bytes = AttachmentBase64.decode(text) ?: return AttachmentPayload.CorruptBase64
        return AttachmentPayload.Bytes(bytes, null, null)
    }

    private fun fromArray(json: Json, array: JsonArray, depth: Int): AttachmentPayload {
        if (array.isEmpty()) return AttachmentPayload.Empty(null)
        // C# byte[] serialized by a non-default serializer: [37,80,68,70,…] (or signed bytes).
        if (array.all { it is JsonPrimitive && !it.isString && it.intOrNull != null }) {
            val values = array.map { (it as JsonPrimitive).intOrNull!! }
            if (values.all { it in -128..255 }) {
                return AttachmentPayload.Bytes(ByteArray(values.size) { values[it].toByte() }, null, null)
            }
            return AttachmentPayload.Empty(null)
        }
        var fallback: AttachmentPayload = AttachmentPayload.Empty(null)
        for (item in array) {
            when (val payload = extract(json, item, depth + 1)) {
                is AttachmentPayload.Bytes, is AttachmentPayload.Link -> return payload
                AttachmentPayload.CorruptBase64 -> fallback = payload
                else -> Unit
            }
        }
        return fallback
    }

    private fun fromObject(json: Json, obj: JsonObject, depth: Int): AttachmentPayload {
        val byKey = obj.entries.associate { (k, v) -> k.lowercase() to v }
        val name = NAME_KEYS.firstNotNullOfOrNull { key -> byKey[key].stringValue() }
        val mime = MIME_KEYS.firstNotNullOfOrNull { key -> byKey[key].stringValue()?.takeIf { '/' in it } }

        var corrupt = false
        var nestedName: String? = null
        for (key in CONTENT_KEYS) {
            val value = byKey[key] ?: continue
            if (value is JsonNull) continue
            // A nested object's own name beats the wrapper's (e.g. {"name":"Dziekanat","file":{…}}).
            when (val payload = extract(json, value, depth + 1)) {
                is AttachmentPayload.Bytes -> return AttachmentPayload.Bytes(
                    payload.bytes,
                    payload.fileName ?: name,
                    payload.mimeType ?: mime
                )
                is AttachmentPayload.Link -> return AttachmentPayload.Link(payload.url, payload.fileName ?: name)
                AttachmentPayload.CorruptBase64 -> corrupt = true
                is AttachmentPayload.Empty -> nestedName = nestedName ?: payload.fileName
                is AttachmentPayload.Html -> Unit
            }
        }
        for (key in URL_KEYS) {
            val url = byKey[key].stringValue() ?: continue
            if (AttachmentUrls.looksLikeUrl(url)) return AttachmentPayload.Link(url, name)
        }
        return if (corrupt) AttachmentPayload.CorruptBase64 else AttachmentPayload.Empty(name ?: nestedName)
    }

    private fun JsonElement?.stringValue(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun AttachmentPayload.withFallbackName(name: String?): AttachmentPayload {
        if (name == null) return this
        return when (this) {
            is AttachmentPayload.Bytes -> if (fileName == null) AttachmentPayload.Bytes(bytes, name, mimeType) else this
            is AttachmentPayload.Link -> if (fileName == null) copy(fileName = name) else this
            is AttachmentPayload.Empty -> if (fileName == null) copy(fileName = name) else this
            else -> this
        }
    }
}
