package pl.lublin.wsei.plan.data.update

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import pl.lublin.wsei.plan.domain.update.VersionComparator

enum class UpdatePhase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, ERROR }

data class UpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val release: ReleaseInfo? = null,
    val progress: Float = 0f,
    val message: String? = null,
    val apk: File? = null
)

/**
 * Update UI state shared by the in-app prompt, "O aplikacji" and the changelog.
 *
 * Two sources work together:
 * - [AppUpdateRepository] ([updateStatus]) knows the latest release, the full changelog and the
 *   dismissed version (also refreshed in the background),
 * - [UpdateManager] ([state]) downloads the APK and builds the installer intent.
 *
 * Scoped to the activity, so the prompt and the About page share one download.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager,
    private val repository: AppUpdateRepository,
    private val strings: LocalizedStrings
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** Latest release, changelog and prompt state from the repository. */
    val updateStatus: StateFlow<AppUpdateStatus> = repository.status

    val currentVersion: String get() = manager.currentVersion

    private var job: Job? = null
    private var autoCheckStarted = false
    private var refreshJob: Job? = null

    init {
        // A release found by the background/auto check makes "O aplikacji" offer the download
        // right away, without a second manual check.
        viewModelScope.launch {
            repository.status.collect { status ->
                val info = status.latest?.takeIf { status.isUpdateAvailable }?.toReleaseInfo() ?: return@collect
                _state.update {
                    if (it.phase == UpdatePhase.IDLE || it.phase == UpdatePhase.UP_TO_DATE) {
                        UpdateState(phase = UpdatePhase.AVAILABLE, release = info)
                    } else {
                        it
                    }
                }
            }
        }
    }

    /** Throttled background check, run once per process when the app shell first appears. */
    fun autoCheckOnce() {
        if (autoCheckStarted) return
        autoCheckStarted = true
        refreshRepository(force = false)
    }

    /** Loads the changelog when the sheet opens and nothing is known yet. */
    fun loadChangelogIfMissing() {
        val status = repository.status.value
        if (status.releases.isEmpty() && !status.isChecking) refreshRepository(force = true)
    }

    /** Re-fetches the changelog (retry button in the sheet). */
    fun reloadChangelog() = refreshRepository(force = true)

    /** Hides the prompt for [version] until a newer release appears. */
    fun dismissPrompt(version: String) {
        viewModelScope.launch {
            try {
                repository.dismiss(version)
            } catch (e: CancellationException) {
                throw e
            } catch (ignored: Exception) {
                // A failed write only means the prompt shows again next time.
            }
        }
    }

    private fun refreshRepository(force: Boolean) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh(force)
            } catch (e: CancellationException) {
                throw e
            } catch (ignored: Exception) {
                // The repository reports its own errors through status.errorMessage.
            }
        }
    }

    /** Manual check ("Sprawdź aktualizacje"): forces a repository refresh and the APK lookup. */
    fun checkForUpdate() {
        if (_state.value.phase == UpdatePhase.CHECKING || _state.value.phase == UpdatePhase.DOWNLOADING) return
        refreshJob?.cancel()
        refreshRepository(force = true)
        _state.value = UpdateState(phase = UpdatePhase.CHECKING)
        job = viewModelScope.launch {
            _state.value = when (val result = manager.checkForUpdate()) {
                is UpdateCheckResult.UpToDate -> UpdateState(phase = UpdatePhase.UP_TO_DATE)
                is UpdateCheckResult.Available -> UpdateState(phase = UpdatePhase.AVAILABLE, release = result.release)
                is UpdateCheckResult.Failure -> UpdateState(phase = UpdatePhase.ERROR, message = result.message)
            }
        }
    }

    /**
     * Downloads [release] (from the prompt) without a second GitHub lookup. Does nothing when
     * the release has no APK — the UI opens its page instead.
     */
    fun download(release: AppRelease) {
        val info = release.toReleaseInfo() ?: return
        val current = _state.value
        if (current.phase == UpdatePhase.DOWNLOADING) return
        if (current.phase == UpdatePhase.READY && current.release?.version == info.version) return
        _state.value = UpdateState(phase = UpdatePhase.AVAILABLE, release = info)
        download()
    }

    fun download() {
        val release = _state.value.release ?: return
        if (_state.value.phase == UpdatePhase.DOWNLOADING) return
        _state.update { it.copy(phase = UpdatePhase.DOWNLOADING, progress = 0f, message = null) }
        job = viewModelScope.launch {
            try {
                val apk = manager.downloadApk(release) { p ->
                    // Late progress callbacks after a cancel must not overwrite the reset state.
                    _state.update { if (it.phase == UpdatePhase.DOWNLOADING) it.copy(progress = p) else it }
                }
                _state.update { it.copy(phase = UpdatePhase.READY, apk = apk, progress = 1f) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: UpdateException) {
                _state.update { it.copy(phase = UpdatePhase.ERROR, message = e.message) }
            } catch (e: Exception) {
                // Anything unexpected (e.g. a storage or security error) must not crash the app.
                _state.update {
                    it.copy(phase = UpdatePhase.ERROR, message = strings.get(R.string.update_error_download_generic))
                }
            }
        }
    }

    fun cancelDownload() {
        if (_state.value.phase != UpdatePhase.DOWNLOADING) return
        job?.cancel()
        _state.update { it.copy(phase = UpdatePhase.AVAILABLE, progress = 0f) }
    }

    /** Intent do uruchomienia przez UI: zgoda na nieznane źródła albo instalator pakietów. */
    fun installIntent(): Intent? {
        val apk = _state.value.apk ?: return null
        return if (manager.canInstallPackages()) manager.installIntent(apk) else manager.unknownSourcesSettingsIntent()
    }

    fun needsInstallPermission(): Boolean = !manager.canInstallPackages()
}

/** `true` when this download state belongs to [version] (progress or ready APK of that release). */
fun UpdateState.isFor(version: String): Boolean =
    release?.let { VersionComparator.compare(it.version, version) == 0 } == true

/** Maps a changelog release to the downloadable form used by [UpdateManager]; null without an APK. */
private fun AppRelease.toReleaseInfo(): ReleaseInfo? {
    val url = apkUrl?.takeIf { it.isNotBlank() } ?: return null
    return ReleaseInfo(
        tag = tagName,
        version = version,
        notes = notesMarkdown,
        apkUrl = url,
        apkName = apkName ?: "Taal-$version.apk",
        apkSize = apkSizeBytes ?: 0L,
        pageUrl = htmlUrl
    )
}
