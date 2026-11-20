package pl.lublin.wsei.plan.data.attachments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpenResult
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AttachmentOpener] that downloads the file through [ScheduleRepository] (active session: bearer
 * token + session cookies are added by the shared OkHttp client), caches it in
 * `cacheDir/attachments` and opens it via `FileProvider` (`${packageName}.updates`, see
 * `res/xml/file_paths.xml`) + `Intent.ACTION_VIEW`.
 *
 * The MIME type is detected from the file signature first and the extension second, because the
 * dean's office often serves files with a missing or wrong extension.
 */
@Singleton
class AndroidAttachmentOpener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) : AttachmentOpener {

    override suspend fun downloadAndOpen(attachment: AnnouncementAttachment): Result<AttachmentOpenResult> = try {
        val downloaded = if (!attachment.fileBytesBase64.isNullOrBlank()) {
            attachment
        } else {
            scheduleRepository.downloadAnnouncementAttachment(attachment.id).getOrThrow()
        }
        val prepared = withContext(Dispatchers.IO) {
            val bytes = decode(downloaded.fileBytesBase64)
            val rawName = downloaded.fileName.ifBlank { attachment.fileName }
            val mime = resolveMimeType(bytes, rawName)
            val name = withExtension(sanitizeFileName(rawName), mime)
            val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
            cleanUpOldFiles(dir)
            val file = File(dir, name)
            file.writeBytes(bytes)
            PreparedFile(file, mime)
        }
        Result.success(withContext(Dispatchers.Main) { open(prepared) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun open(prepared: PreparedFile): AttachmentOpenResult {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", prepared.file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, prepared.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // No resolveActivity() pre-check: without <queries> it returns null on Android 11+ even when
        // a viewer is installed. Starting the activity is the only reliable test.
        return try {
            context.startActivity(intent)
            AttachmentOpenResult.Opened(prepared.file.name)
        } catch (e: ActivityNotFoundException) {
            AttachmentOpenResult.NoViewerApp(prepared.file.name, prepared.mimeType)
        }
    }

    private fun decode(base64: String?): ByteArray {
        val encoded = base64?.substringAfter("base64,")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException(context.getString(R.string.data_attachment_empty))
        val bytes = try {
            Base64.decode(encoded, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException(context.getString(R.string.data_attachment_corrupt), e)
        }
        if (bytes.isEmpty()) throw IllegalStateException(context.getString(R.string.data_attachment_empty))
        return bytes
    }

    /** Removes cached attachments older than [MAX_CACHE_AGE_MILLIS]. */
    private fun cleanUpOldFiles(dir: File) {
        val threshold = System.currentTimeMillis() - MAX_CACHE_AGE_MILLIS
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < threshold) file.deleteRecursively()
        }
    }

    private class PreparedFile(val file: File, val mimeType: String)

    internal companion object {
        private const val CACHE_DIR = "attachments"
        private const val MAX_CACHE_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val OCTET_STREAM = "application/octet-stream"
        private const val MAX_NAME_LENGTH = 120

        /** Strips path separators and characters that are illegal in file names; keeps the extension. */
        fun sanitizeFileName(raw: String): String {
            val cleaned = raw.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                .trim()
                .trim('.')
                .ifBlank { "zalacznik" }
            if (cleaned.length <= MAX_NAME_LENGTH) return cleaned
            val ext = cleaned.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 10 }
            return if (ext == null) {
                cleaned.take(MAX_NAME_LENGTH)
            } else {
                cleaned.substringBeforeLast('.').take(MAX_NAME_LENGTH - ext.length - 1) + "." + ext
            }
        }

        /** Appends an extension matching [mime] when the name has none (viewers often rely on it). */
        fun withExtension(name: String, mime: String): String {
            if (name.substringAfterLast('.', "").isNotEmpty() || mime == OCTET_STREAM) return name
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: return name
            return "$name.$ext"
        }

        fun resolveMimeType(bytes: ByteArray, fileName: String): String {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            val byExtension = extension.takeIf { it.isNotEmpty() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            return when (val signature = sniff(bytes)) {
                // Container formats: the extension tells which document it is (docx, xlsx, odt, doc, xls…).
                Signature.ZIP -> byExtension ?: "application/zip"
                Signature.OLE -> byExtension ?: "application/msword"
                null -> byExtension ?: OCTET_STREAM
                else -> signature.mime
            }
        }

        private enum class Signature(val mime: String) {
            PDF("application/pdf"),
            PNG("image/png"),
            JPEG("image/jpeg"),
            GIF("image/gif"),
            WEBP("image/webp"),
            RTF("application/rtf"),
            ZIP("application/zip"),
            OLE("application/x-ole-storage")
        }

        private fun sniff(b: ByteArray): Signature? {
            fun startsWith(vararg sig: Int, offset: Int = 0): Boolean =
                b.size >= offset + sig.size && sig.indices.all { (b[offset + it].toInt() and 0xFF) == sig[it] }
            return when {
                startsWith(0x25, 0x50, 0x44, 0x46) -> Signature.PDF // %PDF
                startsWith(0x89, 0x50, 0x4E, 0x47) -> Signature.PNG
                startsWith(0xFF, 0xD8, 0xFF) -> Signature.JPEG
                startsWith(0x47, 0x49, 0x46, 0x38) -> Signature.GIF // GIF8
                startsWith(0x52, 0x49, 0x46, 0x46) && startsWith(0x57, 0x45, 0x42, 0x50, offset = 8) -> Signature.WEBP
                startsWith(0x7B, 0x5C, 0x72, 0x74, 0x66) -> Signature.RTF // {\rtf
                startsWith(0x50, 0x4B, 0x03, 0x04) -> Signature.ZIP
                startsWith(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1) -> Signature.OLE
                else -> null
            }
        }
    }
}
