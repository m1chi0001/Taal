package pl.lublin.wsei.plan.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.BuildConfig
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import pl.lublin.wsei.plan.domain.update.VersionComparator
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Dedicated DataStore file of the update engine (last check, dismissed version, cached changelog). */
private val Context.updatesDataStore: DataStore<Preferences> by preferencesDataStore(name = "taal_updates")

/**
 * [AppUpdateRepository] backed by the GitHub Releases API of [UpdateManager.REPO].
 *
 * Security: GitHub is queried with a plain [HttpURLConnection] — never with the dean's-office
 * Ktor client, whose cookie jar and re-auth interceptor would leak session data to a third party.
 *
 * - All published releases (drafts and pre-releases skipped) are kept newest first, so the full
 *   changelog is available; the list is cached in DataStore and shown offline.
 * - [refresh] with `force = false` is throttled to once per [THROTTLE_MILLIS]; a failed check keeps
 *   the cached data and reports [AppUpdateStatus.errorMessage] (it does not count as a check, so
 *   the next launch / background run retries).
 */
@Singleton
class GitHubUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val strings: LocalizedStrings
) : AppUpdateRepository {

    private val _status = MutableStateFlow(AppUpdateStatus(currentVersion = BuildConfig.VERSION_NAME))
    override val status: StateFlow<AppUpdateStatus> = _status.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private var cachedEtag: String? = null
    private var checkedInCurrentProcess = false

    /** Restores the cached state at startup, so the changelog is visible before (or without) a network check. */
    private val loadJob = scope.launch { restore() }

    override suspend fun refresh(force: Boolean) {
        loadJob.join()
        refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val last = _status.value.lastCheckedEpochMillis
            // Every fresh app launch checks GitHub (with a short debounce so Application + UI
            // startup share one call); subsequent foreground resumes throttle to 10 minutes.
            val cooldown = if (checkedInCurrentProcess) THROTTLE_MILLIS else STARTUP_DEBOUNCE_MILLIS
            if (!force && last != null && now - last in 0 until cooldown) return

            _status.update { it.copy(isChecking = true, errorMessage = null) }
            try {
                val ifNoneMatch = cachedEtag?.takeIf { !force && _status.value.releases.isNotEmpty() }
                applyResult(fetchReleases(ifNoneMatch), now)
            } finally {
                // Also when the caller (worker, ViewModel) is cancelled mid-request.
                _status.update { it.copy(isChecking = false) }
            }
        }
    }

    private suspend fun applyResult(result: FetchResult, now: Long) {
        when (result) {
            is FetchResult.Success -> {
                checkedInCurrentProcess = true
                val releases = result.releases
                if (!result.etag.isNullOrBlank()) cachedEtag = result.etag
                _status.update {
                    it.copy(
                        latest = releases.firstOrNull(),
                        releases = releases,
                        isChecking = false,
                        lastCheckedEpochMillis = now,
                        errorMessage = null
                    )
                }
                persist { prefs ->
                    prefs[KEY_LAST_CHECKED] = now
                    prefs[KEY_RELEASES] = GitHubReleaseParser.encodeCache(releases)
                    if (!result.etag.isNullOrBlank()) prefs[KEY_ETAG] = result.etag
                }
            }
            FetchResult.NotModified -> {
                checkedInCurrentProcess = true
                _status.update {
                    it.copy(
                        isChecking = false,
                        lastCheckedEpochMillis = now,
                        errorMessage = null
                    )
                }
                persist { prefs -> prefs[KEY_LAST_CHECKED] = now }
            }
            is FetchResult.Failure -> _status.update {
                it.copy(isChecking = false, errorMessage = result.message)
            }
        }
    }

    override suspend fun dismiss(version: String) {
        loadJob.join()
        val normalized = VersionComparator.normalize(version)
        _status.update { it.copy(dismissedVersion = normalized) }
        persist { it[KEY_DISMISSED] = normalized }
    }

    private suspend fun restore() {
        val prefs = runCatching {
            context.updatesDataStore.data.catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }.first()
        }.getOrElse { e ->
            if (e is CancellationException) throw e
            emptyPreferences()
        }
        cachedEtag = prefs[KEY_ETAG]
        val cached = prefs[KEY_RELEASES]?.let(GitHubReleaseParser::decodeCache).orEmpty()
        _status.update {
            // A refresh may only start after this load, so the cached values never overwrite fresh ones.
            it.copy(
                latest = cached.firstOrNull(),
                releases = cached,
                lastCheckedEpochMillis = prefs[KEY_LAST_CHECKED],
                dismissedVersion = prefs[KEY_DISMISSED]
            )
        }
    }

    private suspend fun persist(transform: (MutablePreferences) -> Unit) {
        try {
            context.updatesDataStore.edit { transform(it) }
        } catch (e: IOException) {
            // Not fatal: the in-memory state is still correct; the next check retries.
        }
    }

    private sealed interface FetchResult {
        data class Success(val releases: List<AppRelease>, val etag: String?) : FetchResult
        data object NotModified : FetchResult
        data class Failure(val message: String) : FetchResult
    }

    private suspend fun fetchReleases(ifNoneMatch: String?): FetchResult = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "Taal-Android/${BuildConfig.VERSION_NAME}")
                if (!ifNoneMatch.isNullOrBlank()) {
                    setRequestProperty("If-None-Match", ifNoneMatch)
                }
            }
            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val etag = connection.getHeaderField("ETag")?.trim()?.takeIf { it.isNotEmpty() }
                        FetchResult.Success(GitHubReleaseParser.parseReleases(body), etag)
                    }
                    HttpURLConnection.HTTP_NOT_MODIFIED -> FetchResult.NotModified
                    HttpURLConnection.HTTP_NOT_FOUND ->
                        FetchResult.Failure(strings.get(R.string.update_error_no_release))
                    HttpURLConnection.HTTP_FORBIDDEN, 429 ->
                        FetchResult.Failure(strings.get(R.string.update_error_rate_limited))
                    else -> FetchResult.Failure(strings.get(R.string.update_error_http, code))
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            FetchResult.Failure(strings.get(R.string.update_error_no_connection))
        } catch (e: Exception) {
            FetchResult.Failure(strings.get(R.string.update_error_unreadable))
        }
    }

    companion object {
        const val THROTTLE_MILLIS: Long = 10L * 60 * 1000
        private const val STARTUP_DEBOUNCE_MILLIS: Long = 30_000L
        private const val RELEASES_API = "https://api.github.com/repos/${UpdateManager.REPO}/releases?per_page=50"

        private val KEY_LAST_CHECKED = longPreferencesKey("last_checked_epoch_millis")
        private val KEY_DISMISSED = stringPreferencesKey("dismissed_version")
        private val KEY_RELEASES = stringPreferencesKey("cached_releases_json_v1")
        private val KEY_ETAG = stringPreferencesKey("cached_releases_etag_v1")
    }
}

/** Pure GitHub Releases JSON mapping (unit-testable without Android). */
object GitHubReleaseParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Serializable
    private data class GhAsset(
        val name: String = "",
        @SerialName("browser_download_url") val downloadUrl: String = "",
        val size: Long = -1L
    )

    @Serializable
    private data class GhRelease(
        @SerialName("tag_name") val tagName: String = "",
        val name: String? = null,
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        @SerialName("published_at") val publishedAt: String? = null,
        @SerialName("html_url") val htmlUrl: String? = null,
        val assets: List<GhAsset> = emptyList()
    )

    /** Storage form of [AppRelease] (the domain model is not serializable). */
    @Serializable
    private data class CachedRelease(
        val version: String,
        val tagName: String,
        val title: String,
        val notesMarkdown: String,
        val publishedAtIso: String? = null,
        val htmlUrl: String,
        val apkUrl: String? = null,
        val apkName: String? = null,
        val apkSizeBytes: Long? = null
    )

    /**
     * Maps a `GET /repos/{repo}/releases` body: drops drafts, pre-releases and untagged entries,
     * sorts newest first by [VersionComparator] (then by publish date) and keeps one entry per version.
     * @throws SerializationException / IllegalArgumentException on a malformed body.
     */
    fun parseReleases(body: String): List<AppRelease> =
        json.decodeFromString(ListSerializer(GhRelease.serializer()), body)
            .asSequence()
            .filter { !it.draft && !it.prerelease && it.tagName.isNotBlank() }
            .map(::toAppRelease)
            .sortedWith(NEWEST_FIRST)
            .distinctBy { it.version }
            .toList()

    private val NEWEST_FIRST: Comparator<AppRelease> =
        Comparator<AppRelease> { a, b -> VersionComparator.compare(b.version, a.version) }
            .thenByDescending { it.publishedAtIso.orEmpty() }

    private fun toAppRelease(r: GhRelease): AppRelease {
        val tag = r.tagName.trim()
        val apk = r.assets
            .filter { it.name.endsWith(".apk", ignoreCase = true) && it.downloadUrl.startsWith("https://") }
            // Prefer a release build over a debug one.
            .sortedByDescending { it.name.contains("release", ignoreCase = true) }
            .firstOrNull()
        return AppRelease(
            version = VersionComparator.normalize(tag),
            tagName = tag,
            title = r.name?.trim()?.takeIf { it.isNotEmpty() } ?: tag,
            notesMarkdown = r.body?.trim().orEmpty(),
            publishedAtIso = r.publishedAt?.takeIf { it.isNotBlank() },
            htmlUrl = r.htmlUrl?.takeIf { it.startsWith("https://") }
                ?: "${UpdateManager.RELEASES_PAGE}/tag/$tag",
            apkUrl = apk?.downloadUrl,
            apkName = apk?.name,
            apkSizeBytes = apk?.size?.takeIf { it > 0 }
        )
    }

    fun encodeCache(releases: List<AppRelease>): String =
        json.encodeToString(
            ListSerializer(CachedRelease.serializer()),
            releases.map {
                CachedRelease(
                    version = it.version,
                    tagName = it.tagName,
                    title = it.title,
                    notesMarkdown = it.notesMarkdown,
                    publishedAtIso = it.publishedAtIso,
                    htmlUrl = it.htmlUrl,
                    apkUrl = it.apkUrl,
                    apkName = it.apkName,
                    apkSizeBytes = it.apkSizeBytes
                )
            }
        )

    /** Decodes [encodeCache] output; a corrupt cache yields an empty list. */
    fun decodeCache(text: String): List<AppRelease> =
        try {
            json.decodeFromString(ListSerializer(CachedRelease.serializer()), text).map {
                AppRelease(
                    version = it.version,
                    tagName = it.tagName,
                    title = it.title,
                    notesMarkdown = it.notesMarkdown,
                    publishedAtIso = it.publishedAtIso,
                    htmlUrl = it.htmlUrl,
                    apkUrl = it.apkUrl,
                    apkName = it.apkName,
                    apkSizeBytes = it.apkSizeBytes
                )
            }
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
}
