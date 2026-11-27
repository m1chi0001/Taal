package pl.lublin.wsei.plan.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.domain.update.VersionComparator
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** GitHub release metadata needed to download and verify an installable APK asset. */
data class ReleaseInfo(
    val tag: String,
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val pageUrl: String
)

/** APK download or verification failure with a localized user-facing message. */
class UpdateException(message: String) : Exception(message)

/**
 * Downloads, verifies and launches installation of signed release APKs from [REPO].
 *
 * Uses a plain [HttpURLConnection] rather than the Wirtualny Dziekanat Ktor client so dean's-office
 * session cookies and auto-reauth interceptors are never exposed to third-party hosts.
 */
@Singleton
class UpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val strings: LocalizedStrings
) {

    val currentVersion: String
        get() = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "0" }

    /** Downloads the APK to `cacheDir/updates`, reporting progress in `0..1`, and verifies its package ID. */
    suspend fun downloadApk(release: ReleaseInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            if (!release.apkUrl.startsWith("https://")) {
                throw UpdateException(strings.get(R.string.update_error_bad_url))
            }
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, "Taal-${release.version}.apk")
            val partial = File(dir, "Taal-${release.version}.apk.part")
            try {
                val connection = open(release.apkUrl, "application/octet-stream")
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw UpdateException(
                            strings.get(R.string.update_error_download_http, connection.responseCode)
                        )
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
                if (!partial.renameTo(target)) {
                    throw UpdateException(strings.get(R.string.update_error_save_failed))
                }
                verifyApk(target)
                target
            } catch (e: CancellationException) {
                partial.delete()
                throw e
            } catch (e: UpdateException) {
                partial.delete()
                target.delete()
                throw e
            } catch (_: IOException) {
                partial.delete()
                target.delete()
                throw UpdateException(strings.get(R.string.update_error_interrupted))
            }
        }

    /** Rejects archives whose packageName differs from this app; Android also enforces signature match at install. */
    private fun verifyApk(file: File) {
        val info = context.packageManager.getPackageArchiveInfo(file.path, 0)
            ?: throw UpdateException(strings.get(R.string.update_error_invalid_apk))
        if (info.packageName != context.packageName) {
            throw UpdateException(strings.get(R.string.update_error_foreign_apk))
        }
    }

    /** Since Android 8.0, self-updating requires the per-app "Install unknown apps" permission. */
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

        /**
         * `isNewer("0.5.1", "0.5")` → true. Delegates to [VersionComparator]; a remote tag without
         * any digit is never considered newer.
         */
        fun isNewer(remote: String, local: String): Boolean =
            remote.any { it.isDigit() } && VersionComparator.compare(remote, local) > 0
    }
}
