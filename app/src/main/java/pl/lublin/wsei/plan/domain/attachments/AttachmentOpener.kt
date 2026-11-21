package pl.lublin.wsei.plan.domain.attachments

import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment

/** Outcome of opening an announcement attachment. */
sealed interface AttachmentOpenResult {
    /** The file was handed to a viewer app. */
    data class Opened(val fileName: String) : AttachmentOpenResult

    /** Downloaded and cached, but no installed app can open this MIME type. */
    data class NoViewerApp(val fileName: String, val mimeType: String) : AttachmentOpenResult
}

/**
 * Downloads an announcement attachment with the active dean's office session, stores it in
 * `cacheDir/attachments`, resolves its MIME type and opens it in the system default viewer
 * through `FileProvider` + `Intent.ACTION_VIEW`.
 */
interface AttachmentOpener {
    suspend fun downloadAndOpen(attachment: AnnouncementAttachment): Result<AttachmentOpenResult>
}
