package pl.lublin.wsei.plan.domain.attachments

import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment

/** Outcome of opening an announcement attachment. */
sealed interface AttachmentOpenResult {
    /** The file was handed to a viewer app (directly, through the app chooser or the share sheet). */
    data class Opened(val fileName: String) : AttachmentOpenResult

    /** Downloaded and cached, but no installed app can open this MIME type. */
    data class NoViewerApp(val fileName: String, val mimeType: String) : AttachmentOpenResult
}

/**
 * A downloaded attachment, ready to be written to disk.
 *
 * @property fileName sanitized name with an extension matching the content when it was detectable.
 * @property mimeType best known MIME type (`application/octet-stream` only when nothing is known).
 */
class DownloadedAttachment(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String
)

/**
 * A failed attachment download whose [message] is already localized for the student.
 * Session expiry and network failures are reported with their own exception types
 * (`AprSessionExpiredException`, `IOException`), which the UI maps to localized texts as well.
 */
class AttachmentDownloadException(
    val reason: Reason,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    enum class Reason {
        /** HTTP 404/400/410, or every endpoint variant failed without a better explanation. */
        NOT_FOUND,

        /** The server answered 200 with an HTML page (error page) instead of the file. */
        HTML_PAGE,

        /** The response had no content. */
        EMPTY,

        /** The content was present but not decodable. */
        CORRUPT,

        /** The file is larger than the app downloads in memory. */
        TOO_LARGE,

        /** The file couldn't be written to the cache. */
        SAVE_FAILED,

        /** The file was saved but couldn't be handed to another app. */
        OPEN_FAILED
    }
}

/**
 * Downloads an announcement attachment with the active dean's office session, stores it in
 * `cacheDir/attachments`, resolves its MIME type and opens it in the system default viewer
 * through `FileProvider` + `Intent.ACTION_VIEW` (falling back to the app chooser and the share sheet).
 */
interface AttachmentOpener {
    suspend fun downloadAndOpen(attachment: AnnouncementAttachment): Result<AttachmentOpenResult>
}
