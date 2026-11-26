package pl.lublin.wsei.plan.data.attachments

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpenResult
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.attachments.DownloadedAttachment
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AttachmentOpener] that downloads the file through [ScheduleRepository] (active session: bearer
 * token + session cookies are added by the shared OkHttp client), caches it in
 * `cacheDir/attachments/<attachment key>/<name>` and opens it via `FileProvider`
 * (`${packageName}.updates`, see `res/xml/file_paths.xml`).
 *
 * Opening tries, in order: `ACTION_VIEW` with the detected MIME type, an app chooser over every
 * viewer (wildcard type, for files whose type no app claims), and the share sheet, so the student can
 * at least save the file. A file downloaded earlier (same attachment, non-empty, younger than
 * [MAX_CACHE_AGE_MILLIS]) is reopened without touching the network.
 */
@Singleton
class AndroidAttachmentOpener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val strings: LocalizedStrings,
    private val responses: AttachmentResponseHandler
) : AttachmentOpener {

    override suspend fun downloadAndOpen(attachment: AnnouncementAttachment): Result<AttachmentOpenResult> = try {
        val prepared = withContext(Dispatchers.IO) { findCached(attachment) } ?: run {
            // Inline Base64 from the list is a shortcut only: if it's broken, download properly.
            val downloaded = attachment.fileBytesBase64?.takeIf { it.isNotBlank() }
                ?.let { base64 -> runCatching { responses.fromInlineBase64(attachment, base64) }.getOrNull() }
                ?: scheduleRepository.downloadAnnouncementAttachment(attachment).getOrThrow()
            withContext(Dispatchers.IO) { save(attachment, downloaded) }
        }
        Result.success(withContext(Dispatchers.Main) { open(prepared) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------------------------------------------------------------------------------------
    // Cache
    // ---------------------------------------------------------------------------------------------

    private fun rootDir(): File = File(context.cacheDir, CACHE_DIR)

    /** One directory per attachment, so a reopened file keeps its name and nothing collides. */
    private fun cacheKey(attachment: AnnouncementAttachment): String {
        if (attachment.id > 0) return "id_${attachment.id}"
        val seed = "${attachment.announcementId}|${attachment.fileName}|${attachment.downloadUrl.orEmpty()}|" +
            attachment.alternativeFileId.orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        return "h_" + digest.take(10).joinToString("") { "%02x".format(it) }
    }

    private fun findCached(attachment: AnnouncementAttachment): PreparedFile? {
        val dir = File(rootDir(), cacheKey(attachment))
        val files = dir.listFiles { f -> f.isFile && !f.name.endsWith(PARTIAL_SUFFIX) }.orEmpty()
        val file = files.singleOrNull()?.takeIf { it.length() > 0 } ?: return null
        if (file.lastModified() < System.currentTimeMillis() - MAX_CACHE_AGE_MILLIS) return null
        return PreparedFile(file, mimeTypeFor(file.name, readHeader(file)))
    }

    private fun save(attachment: AnnouncementAttachment, downloaded: DownloadedAttachment): PreparedFile {
        if (downloaded.bytes.isEmpty()) throw responses.empty()
        val root = rootDir()
        try {
            root.mkdirs()
            cleanUpOldFiles(root)
            val dir = File(root, cacheKey(attachment))
            dir.deleteRecursively()
            if (!dir.mkdirs() && !dir.isDirectory) throw IOException("Cannot create the attachment directory.")
            val name = withExtension(
                AttachmentFileNames.sanitize(downloaded.fileName, AttachmentResponseHandler.fallbackBaseName(attachment)),
                downloaded.mimeType
            )
            val partial = File(dir, name + PARTIAL_SUFFIX)
            partial.writeBytes(downloaded.bytes)
            val file = File(dir, name)
            if (!partial.renameTo(file)) {
                partial.delete()
                throw IOException("Cannot finalize the attachment file.")
            }
            val mime = downloaded.mimeType.takeUnless { AttachmentFileTypes.isGenericMime(it) }
                ?: mimeTypeFor(name, downloaded.bytes)
            return PreparedFile(file, mime)
        } catch (e: IOException) {
            throw responses.saveFailed(e)
        } catch (e: SecurityException) {
            throw responses.saveFailed(e)
        }
    }

    /** Removes cached attachments older than [MAX_CACHE_AGE_MILLIS]. */
    private fun cleanUpOldFiles(dir: File) {
        val threshold = System.currentTimeMillis() - MAX_CACHE_AGE_MILLIS
        dir.listFiles()?.forEach { entry ->
            val newest = if (entry.isDirectory) {
                entry.listFiles()?.maxOfOrNull { it.lastModified() } ?: entry.lastModified()
            } else {
                entry.lastModified()
            }
            if (newest < threshold) entry.deleteRecursively()
        }
    }

    private fun readHeader(file: File): ByteArray = runCatching {
        file.inputStream().use { input ->
            val buffer = ByteArray(HEADER_BYTES)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }
    }.getOrDefault(ByteArray(0))

    // ---------------------------------------------------------------------------------------------
    // Opening
    // ---------------------------------------------------------------------------------------------

    private fun open(prepared: PreparedFile): AttachmentOpenResult {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.updates", prepared.file)
        } catch (e: IllegalArgumentException) {
            throw responses.openFailed(e) // path not covered by res/xml/file_paths.xml
        }
        val name = prepared.file.name

        // 1. The default viewer for the detected type. No resolveActivity() pre-check: starting
        //    the activity is the only reliable test.
        if (tryStart(viewIntent(uri, prepared.mimeType, name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))) {
            return AttachmentOpenResult.Opened(name)
        }
        // 2. Any app that views files (the type may be wrong, or no app claims it explicitly).
        val viewAny = viewIntent(uri, ANY_TYPE, name)
        if (hasHandler(viewAny) &&
            tryStart(chooser(viewAny, strings.get(R.string.data_attachment_open_with, name)))
        ) {
            return AttachmentOpenResult.Opened(name)
        }
        // 3. Share sheet: lets the student save the file to Files/Drive or send it to a viewer.
        val send = Intent(Intent.ACTION_SEND).apply {
            type = prepared.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (hasHandler(send) && tryStart(chooser(send, strings.get(R.string.data_attachment_share, name)))) {
            return AttachmentOpenResult.Opened(name)
        }
        return AttachmentOpenResult.NoViewerApp(name, prepared.mimeType)
    }

    /** ClipData carries the URI grant through choosers and intent forwarding. */
    private fun viewIntent(uri: Uri, mimeType: String, name: String): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            clipData = ClipData.newUri(context.contentResolver, name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun chooser(target: Intent, title: String): Intent =
        Intent.createChooser(target, title).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

    private fun tryStart(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        throw responses.openFailed(e)
    }

    /** Needs the `<queries>` entries in the manifest to see other apps on Android 11+. */
    @Suppress("DEPRECATION")
    private fun hasHandler(intent: Intent): Boolean =
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

    // ---------------------------------------------------------------------------------------------
    // MIME
    // ---------------------------------------------------------------------------------------------

    /** Extension table first, then Android's `MimeTypeMap`, then magic bytes. */
    private fun mimeTypeFor(fileName: String, header: ByteArray): String {
        val ext = AttachmentFileNames.extensionOf(fileName)
        return AttachmentFileTypes.mimeForExtension(ext)
            ?: ext?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: AttachmentFileTypes.sniff(header)?.mimeType
            ?: AttachmentFileTypes.OCTET_STREAM
    }

    /** Appends an extension matching [mime] when the name has none (viewers often rely on it). */
    private fun withExtension(name: String, mime: String): String {
        if (AttachmentFileNames.extensionOf(name) != null || AttachmentFileTypes.isGenericMime(mime)) return name
        val ext = AttachmentFileTypes.extensionForMime(mime)
            ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime.substringBefore(';').trim())
            ?: return name
        return "$name.$ext"
    }

    private class PreparedFile(val file: File, val mimeType: String)

    private companion object {
        const val CACHE_DIR = "attachments"
        const val PARTIAL_SUFFIX = ".part"
        const val MAX_CACHE_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val HEADER_BYTES = 64 * 1024
        const val ANY_TYPE = "*/*"
    }
}
