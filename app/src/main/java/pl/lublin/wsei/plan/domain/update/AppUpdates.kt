package pl.lublin.wsei.plan.domain.update

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.StateFlow

/** One GitHub release of Taal. [version] has no leading "v" ("0.7.6"). */
@Immutable
data class AppRelease(
    val version: String,
    val tagName: String,
    val title: String,
    /** Release notes as GitHub Markdown. */
    val notesMarkdown: String,
    val publishedAtIso: String?,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkName: String?,
    val apkSizeBytes: Long?
)

@Immutable
data class AppUpdateStatus(
    val currentVersion: String,
    /** Newest release, if any is known. */
    val latest: AppRelease? = null,
    /** All releases, newest first (full changelog). */
    val releases: List<AppRelease> = emptyList(),
    val isChecking: Boolean = false,
    val lastCheckedEpochMillis: Long? = null,
    /** Version the user dismissed the update prompt for. */
    val dismissedVersion: String? = null,
    val errorMessage: String? = null
) {
    /** A newer release than the installed one exists. */
    val isUpdateAvailable: Boolean get() = latest != null && latest.version.isNotBlank() && newerThanCurrent

    /** Show the non-intrusive prompt: update available and not dismissed for this version. */
    val shouldPrompt: Boolean get() = isUpdateAvailable && dismissedVersion != latest?.version

    private val newerThanCurrent: Boolean
        get() = latest?.let { VersionComparator.compare(it.version, currentVersion) > 0 } ?: false
}

/** Semantic-ish version comparison: strips a leading "v", compares numeric parts. */
object VersionComparator {
    fun normalize(version: String): String = version.trim().removePrefix("v").removePrefix("V")

    fun compare(a: String, b: String): Int {
        fun parts(v: String) = Regex("\\d+").findAll(normalize(v)).map { it.value.toInt() }.toList()
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}

/**
 * GitHub Releases backed update source: latest version + full changelog, throttled background
 * checks, persisted last-check time and dismissed version.
 */
interface AppUpdateRepository {
    val status: StateFlow<AppUpdateStatus>

    /** Checks GitHub; when [force] is false it is skipped if the last check is recent (throttle). */
    suspend fun refresh(force: Boolean)

    /** Hides the prompt for [version] until a newer one appears. */
    suspend fun dismiss(version: String)
}
