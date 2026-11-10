package pl.lublin.wsei.plan.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Wydanie z GitHuba, które da się zainstalować (ma załączony plik APK). */
data class ReleaseInfo(
    val tag: String,
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val pageUrl: String
)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Available(val release: ReleaseInfo) : UpdateCheckResult
    data class Failure(val message: String) : UpdateCheckResult
}

/** Błąd pobierania/weryfikacji aktualizacji z komunikatem gotowym do pokazania użytkownikowi. */
class UpdateException(message: String) : Exception(message)

/**
 * Aktualizacje „z poziomu aplikacji”: sprawdza najnowsze wydanie w GitHubie
 * ([REPO]), pobiera załączony plik APK i uruchamia systemowy instalator.
 *
 * Używa zwykłego `HttpURLConnection`, a nie klienta Ktor od Wirtualnego Dziekanatu —
 * ten ma ciasteczka sesji i interceptor re-autoryzacji, które nie powinny dotykać GitHuba.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val currentVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "0" }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val connection = open(LATEST_RELEASE_API, "application/vnd.github+json")
            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        parseRelease(body)
                    }
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        UpdateCheckResult.Failure("Na GitHubie nie ma jeszcze opublikowanego wydania.")
                    HttpURLConnection.HTTP_FORBIDDEN, 429 ->
                        UpdateCheckResult.Failure("GitHub tymczasowo ogranicza zapytania. Spróbuj ponownie za kilka minut.")
                    else -> UpdateCheckResult.Failure("GitHub odpowiedział kodem HTTP $code.")
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UpdateCheckResult.Failure("Brak połączenia z GitHubem. Sprawdź internet i spróbuj ponownie.")
        } catch (e: Exception) {
            UpdateCheckResult.Failure("Nie udało się odczytać informacji o wydaniu.")
        }
    }

    private fun parseRelease(body: String): UpdateCheckResult {
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        if (tag.isBlank()) return UpdateCheckResult.Failure("Wydanie na GitHubie nie ma numeru wersji.")
        val remote = tag.removePrefix("v").removePrefix("V")
        if (!isNewer(remote, currentVersion)) return UpdateCheckResult.UpToDate(currentVersion)

        val assets = json.optJSONArray("assets")
        var chosen: JSONObject? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
                // Preferuj plik release, nie debug.
                if (chosen == null || (asset.optString("name").contains("release", true) &&
                        !chosen.optString("name").contains("release", true))
                ) chosen = asset
            }
        }
        val apk = chosen ?: return UpdateCheckResult.Failure(
            "Wydanie $tag nie ma załączonego pliku APK. Dodaj go do wydania na GitHubie."
        )
        return UpdateCheckResult.Available(
            ReleaseInfo(
                tag = tag,
                version = remote,
                notes = json.optString("body").trim(),
                apkUrl = apk.optString("browser_download_url"),
                apkName = apk.optString("name"),
                apkSize = apk.optLong("size", -1L),
                pageUrl = json.optString("html_url", RELEASES_PAGE)
            )
        )
    }

    /** Pobiera APK do `cacheDir/updates`, raportując postęp 0..1, i sprawdza, że to paczka tej aplikacji. */
    suspend fun downloadApk(release: ReleaseInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            if (!release.apkUrl.startsWith("https://")) throw UpdateException("Nieprawidłowy adres pobierania.")
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "Taal-${release.version}.apk")
            val partial = File(dir, "Taal-${release.version}.apk.part")
            try {
                val connection = open(release.apkUrl, "application/octet-stream")
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw UpdateException("Pobieranie nie powiodło się (HTTP ${connection.responseCode}).")
                    }
                    val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
                    var done = 0L
                    connection.inputStream.use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                    if (release.apkSize > 0 && done != release.apkSize) {
                        throw UpdateException("Pobrany plik jest niekompletny. Spróbuj ponownie.")
                    }
                } finally {
                    connection.disconnect()
                }
                if (!partial.renameTo(target)) throw UpdateException("Nie udało się zapisać pobranego pliku.")
                verifyApk(target)
                target
            } catch (e: CancellationException) {
                partial.delete()
                throw e
            } catch (e: UpdateException) {
                partial.delete(); target.delete()
                throw e
            } catch (e: IOException) {
                partial.delete(); target.delete()
                throw UpdateException("Przerwano pobieranie. Sprawdź połączenie i spróbuj ponownie.")
            }
        }

    /** Odrzuca pliki, które nie są paczką Taala. Zgodność podpisu dodatkowo pilnuje sam system przy instalacji. */
    private fun verifyApk(file: File) {
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: throw UpdateException("Pobrany plik nie jest poprawnym APK.")
        if (info.packageName != context.packageName) {
            throw UpdateException("Pobrany plik pochodzi z innej aplikacji.")
        }
    }

    /** Od Androida 8 instalowanie z własnej aplikacji wymaga zgody „Instaluj nieznane aplikacje”. */
    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun open(url: String, accept: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "Taal-Android/$currentVersion")
        }

    companion object {
        const val REPO = "m1chi0001/Taal"
        const val RELEASES_PAGE = "https://github.com/$REPO/releases"
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/$REPO/releases/latest"

        /** `isNewer("0.5.1", "0.5")` → true; porównuje liczbowo kolejne człony wersji. */
        fun isNewer(remote: String, local: String): Boolean {
            fun parts(v: String) = Regex("\\d+").findAll(v).map { it.value.toInt() }.toList()
            val a = parts(remote)
            val b = parts(local)
            if (a.isEmpty()) return false
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
