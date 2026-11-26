package pl.lublin.wsei.plan.data.attachments

import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.data.remote.api.AprApiException
import pl.lublin.wsei.plan.data.remote.api.AprHttpException
import pl.lublin.wsei.plan.data.remote.api.AprServerException
import pl.lublin.wsei.plan.domain.attachments.AttachmentDownloadException
import pl.lublin.wsei.plan.domain.attachments.AttachmentDownloadException.Reason
import pl.lublin.wsei.plan.domain.attachments.DownloadedAttachment
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a successful (2xx) download response into a [DownloadedAttachment] or a link to follow,
 * and builds the localized [AttachmentDownloadException]s. Shared by the REST API path
 * ([pl.lublin.wsei.plan.data.remote.api.ArpApiDataSource]) and the web portal fallback.
 */
@Singleton
class AttachmentResponseHandler @Inject constructor(
    private val json: Json,
    private val strings: LocalizedStrings
) {

    sealed interface Outcome {
        class File(val attachment: DownloadedAttachment) : Outcome
        class FollowLink(val url: String) : Outcome
    }

    /**
     * @param sourceUrl the URL that produced the body; relative links are resolved against it.
     * @throws AttachmentDownloadException for HTML pages, empty or undecodable content.
     * @throws AprApiException for ABP failures inside a JSON envelope.
     */
    fun handle(
        attachment: AnnouncementAttachment,
        sourceUrl: String,
        contentType: String?,
        contentDisposition: String?,
        bytes: ByteArray
    ): Outcome {
        if (bytes.size > MAX_ATTACHMENT_BYTES) throw tooLarge()
        return when (val payload = AttachmentResponseInterpreter.interpret(json, contentType, contentDisposition, bytes)) {
            is AttachmentPayload.Bytes -> Outcome.File(toDownloaded(attachment, payload, sourceUrl))
            is AttachmentPayload.Link -> Outcome.FollowLink(
                AttachmentUrls.resolve(sourceUrl, payload.url) ?: throw notFound()
            )
            is AttachmentPayload.Html -> throw htmlPage(payload.isLoginPage)
            is AttachmentPayload.Empty -> throw empty()
            AttachmentPayload.CorruptBase64 -> throw corrupt()
        }
    }

    /** Decodes the Base64 that arrived inline in the announcement list (or the demo account). */
    fun fromInlineBase64(attachment: AnnouncementAttachment, base64: String): DownloadedAttachment {
        val bytes = AttachmentBase64.decode(base64) ?: throw corrupt()
        return toDownloaded(attachment, AttachmentPayload.Bytes(bytes, null, attachment.mimeType), null)
    }

    private fun toDownloaded(
        attachment: AnnouncementAttachment,
        payload: AttachmentPayload.Bytes,
        sourceUrl: String?
    ): DownloadedAttachment {
        if (payload.bytes.isEmpty()) throw empty()
        if (payload.bytes.size > MAX_ATTACHMENT_BYTES) throw tooLarge()
        val resolved = AttachmentFileNames.resolve(
            nameCandidates = AttachmentFileNames.candidates(
                responseName = payload.fileName,
                listName = attachment.fileName,
                urlName = AttachmentFileNames.nameFromUrl(sourceUrl)
            ),
            bytes = payload.bytes,
            declaredMime = payload.mimeType ?: attachment.mimeType,
            fallbackBaseName = fallbackBaseName(attachment)
        )
        return DownloadedAttachment(resolved.fileName, payload.bytes, resolved.mimeType)
    }

    fun notFound(cause: Throwable? = null) =
        AttachmentDownloadException(Reason.NOT_FOUND, strings.get(R.string.error_attachment_not_found), cause)

    fun htmlPage(isLoginPage: Boolean) = AttachmentDownloadException(
        Reason.HTML_PAGE,
        strings.get(if (isLoginPage) R.string.error_attachment_login_page else R.string.error_attachment_html_page)
    )

    fun empty() = AttachmentDownloadException(Reason.EMPTY, strings.get(R.string.data_attachment_empty))

    fun corrupt(cause: Throwable? = null) =
        AttachmentDownloadException(Reason.CORRUPT, strings.get(R.string.data_attachment_corrupt), cause)

    fun tooLarge() = AttachmentDownloadException(Reason.TOO_LARGE, strings.get(R.string.error_attachment_too_large))

    fun saveFailed(cause: Throwable?) =
        AttachmentDownloadException(Reason.SAVE_FAILED, strings.get(R.string.error_attachment_save_failed), cause)

    fun openFailed(cause: Throwable?) =
        AttachmentDownloadException(Reason.OPEN_FAILED, strings.get(R.string.error_attachment_open_failed), cause)

    companion object {
        /** Attachments are held in memory (Base64 JSON needs that anyway); dean's office files are small. */
        const val MAX_ATTACHMENT_BYTES: Long = 64L * 1024 * 1024

        fun fallbackBaseName(attachment: AnnouncementAttachment): String =
            if (attachment.id > 0) "${AttachmentFileNames.DEFAULT_BASE_NAME}_${attachment.id}" else AttachmentFileNames.DEFAULT_BASE_NAME

        /**
         * Of two failures from different endpoint variants, keeps the one that tells the student more:
         * an HTML/login page or a server message beats corrupt/empty content, which beats "not found"
         * (the expected answer of a variant with wrong parameter names).
         */
        fun moreInformative(current: Throwable?, candidate: Throwable): Throwable {
            if (current == null) return candidate
            return if (rank(candidate) > rank(current)) candidate else current
        }

        private fun rank(error: Throwable): Int = when (error) {
            is AttachmentDownloadException -> when (error.reason) {
                Reason.HTML_PAGE -> 6
                Reason.TOO_LARGE -> 5
                Reason.CORRUPT -> 3
                Reason.EMPTY -> 2
                Reason.NOT_FOUND -> 0
                Reason.SAVE_FAILED, Reason.OPEN_FAILED -> 1
            }
            is AprServerException -> if (error.message.isNullOrBlank()) 1 else 4
            is AprHttpException -> 1
            else -> 1
        }
    }
}
