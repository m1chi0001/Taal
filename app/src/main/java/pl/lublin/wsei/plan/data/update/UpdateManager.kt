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
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.domain.update.VersionComparator

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
    @ApplicationContext private val context: Context,
    private val strings: LocalizedStrings
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
                        UpdateCheckResult.Failure(strings.get(R.string.update_error_no_release))
                    HttpURLConnection.HTTP_FORBIDDEN, 429 ->
                        UpdateCheckResult.Failure(strings.get(R.string.update_error_rate_limited))
                    else -> UpdateCheckResult.Failure(strings.get(R.string.update_error_http, code))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UpdateCheckResult.Failure(strings.get(R.string.update_error_no_connection))
        } catch (e: Exception) {
            UpdateCheckResult.Failure(strings.get(R.string.update_error_unreadable))
        }
    }

    private fun parseRelease(body: String): UpdateCheckResult {
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        if (tag.isBlank()) return UpdateCheckResult.Failure(strings.get(R.string.update_error_no_version))
        val remote = VersionComparator.normalize(tag)
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
            strings.get(R.string.update_error_no_apk, tag)
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
            if (!release.apkUrl.startsWith("https://")) throw UpdateException(strings.get(R.string.update_error_bad_url))
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "Taal-${release.version}.apk")
            val partial = File(dir, "Taal-${release.version}.apk.part")
            try {
                val connection = open(release.apkUrl, "application/octet-stream")
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw UpdateException(strings.get(R.string.update_error_download_http, connection.responseCode))
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
                        throw UpdateException(strings.get(R.string.update_error_incomplete))
                    }
                } finally {
                    connection.disconnect()
                }
                if (!partial.renameTo(target)) throw UpdateException(strings.get(R.string.update_error_save_failed))
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
                throw UpdateException(strings.get(R.string.update_error_interrupted))
            }
        }

    /** Odrzuca pliki, które nie są paczką Taala. Zgodność podpisu dodatkowo pilnuje sam system przy instalacji. */
    private fun verifyApk(file: File) {
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: throw UpdateException(strings.get(R.string.update_error_invalid_apk))
        if (info.packageName != context.packageName) {
            throw UpdateException(strings.get(R.string.update_error_foreign_apk))
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
            if (url.startsWith(GITHUB_API)) setRequestProperty("X-GitHub-Api-Version", GITHUB_API_VERSION)
        }

    companion object {
        const val REPO = "m1chi0001/Taal"
        const val RELEASES_PAGE = "https://github.com/$REPO/releases"
        private const val GITHUB_API = "https://api.github.com/"
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val LATEST_RELEASE_API = "${GITHUB_API}repos/$REPO/releases/latest"

        /**
         * `isNewer("0.5.1", "0.5")` → true. Delegates to [VersionComparator] (shared with
         * [GitHubUpdateRepository]); a remote version without any number is never newer.
         */
        fun isNewer(remote: String, local: String): Boolean =
            remote.any { it.isDigit() } && VersionComparator.compare(remote, local) > 0
    }
}
