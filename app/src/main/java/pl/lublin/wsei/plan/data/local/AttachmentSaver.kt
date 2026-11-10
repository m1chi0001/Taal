package pl.lublin.wsei.plan.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import java.io.File

/** Zapisany załącznik: [uri] do otwarcia, [location] do pokazania użytkownikowi. */
data class SavedAttachment(val fileName: String, val uri: Uri, val location: String)

/**
 * Zapisuje pobrane załączniki ogłoszeń na urządzeniu: w folderze „Pobrane/Taal” (Android 10+ przez MediaStore,
 * bez dodatkowych uprawnień), a na starszych wersjach w katalogu plików aplikacji.
 */
object AttachmentSaver {

    fun save(context: Context, attachment: AnnouncementAttachment): Result<SavedAttachment> = runCatching {
        val encoded = attachment.fileBytesBase64
            ?.substringAfter("base64,")
            ?.takeIf { it.isNotBlank() }
            ?: error("Załącznik jest pusty.")
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        if (bytes.isEmpty()) error("Załącznik jest pusty.")

        val name = sanitize(attachment.fileName)
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Taal")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Nie udało się utworzyć pliku w folderze Pobrane.")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: error("Nie udało się zapisać pliku.")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            SavedAttachment(name, uri, "Pobrane/Taal")
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Taal").apply { mkdirs() }
            val file = uniqueFile(dir, name)
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
            SavedAttachment(file.name, uri, "Android/data/${context.packageName}/files/Download/Taal")
        }
    }

    /** Usuwa separatory ścieżek i znaki niedozwolone w nazwach plików; zachowuje rozszerzenie. */
    internal fun sanitize(raw: String): String {
        val cleaned = raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .trim('.')
        return cleaned.ifBlank { "zalacznik" }
    }

    private fun uniqueFile(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($i)$ext")
            i++
        }
        return candidate
    }
}
