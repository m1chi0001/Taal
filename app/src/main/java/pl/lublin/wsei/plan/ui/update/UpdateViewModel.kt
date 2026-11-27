package pl.lublin.wsei.plan.ui.update

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
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.data.update.ReleaseInfo
import pl.lublin.wsei.plan.data.update.UpdateException
import pl.lublin.wsei.plan.data.update.UpdateManager
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import pl.lublin.wsei.plan.domain.update.VersionComparator
import java.io.File
import javax.inject.Inject

enum class UpdatePhase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, ERROR }

data class UpdateState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val release: ReleaseInfo? = null,
    val progress: Float = 0f,
    val message: String? = null,
    val apk: File? = null
)

/**
 * Update UI state shared by the in-app prompt, "O aplikacji" and the changelog sheet.
 *
 * Two collaborators work together:
 * - [AppUpdateRepository] ([updateStatus]) fetches, parses and caches GitHub releases,
 * - [UpdateManager] ([state]) downloads the verified APK and builds the package installer intent.
 *
 * Scoped to the activity so the prompt and the About screen share a single download state.
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

    /** Version snoozed ("Później") for the current app session only. */
    private val _snoozedVersion = MutableStateFlow<String?>(null)
    val snoozedVersion: StateFlow<String?> = _snoozedVersion.asStateFlow()

    val currentVersion: String get() = manager.currentVersion

    private var job: Job? = null
    private var refreshJob: Job? = null

    init {
        // Syncs "O aplikacji" with the automatic/background repository state right away, so the
        // card shows AVAILABLE or UP_TO_DATE without requiring a second manual check.
        viewModelScope.launch {
            repository.status.collect { status ->
                val latest = status.latest
                if (status.isUpdateAvailable && latest != null) {
                    val info = latest.toReleaseInfo()
                    _state.update { current ->
                        if (current.phase == UpdatePhase.IDLE || current.phase == UpdatePhase.UP_TO_DATE) {
                            UpdateState(phase = UpdatePhase.AVAILABLE, release = info)
                        } else {
                            current
                        }
                    }
                } else if (!status.isChecking && status.lastCheckedEpochMillis != null) {
                    _state.update { current ->
                        if (current.phase == UpdatePhase.IDLE) {
                            UpdateState(phase = UpdatePhase.UP_TO_DATE)
                        } else {
                            current
                        }
                    }
                }
            }
        }
    }

    /** Automatic check triggered on app launch and foreground resume (throttled in repository). */
    fun autoCheckOnForeground() {
        refreshRepository(force = false)
    }

    /** Loads the changelog when the sheet opens and nothing is cached yet. */
    fun loadChangelogIfMissing() {
        val status = repository.status.value
        if (status.releases.isEmpty() && !status.isChecking) refreshRepository(force = true)
    }

    /** Re-fetches the changelog (retry button in the sheet). */
    fun reloadChangelog() = refreshRepository(force = true)

    /** Hides the automatic dialog for the rest of this app session ("Później"). */
    fun snoozePromptForSession(version: String) {
        _snoozedVersion.value = VersionComparator.normalize(version)
    }

    /** Hides the prompt for [version] permanently until a newer release appears. */
    fun dismissPrompt(version: String) {
        val normalized = VersionComparator.normalize(version)
        _snoozedVersion.value = normalized
        viewModelScope.launch {
            try {
                repository.dismiss(normalized)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
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
            } catch (_: Exception) {
                // The repository reports its own errors through status.errorMessage.
            }
        }
    }

    /** Manual check ("Sprawdź aktualizacje"): performs a single forced repository refresh. */
    fun checkForUpdate() {
        if (_state.value.phase == UpdatePhase.CHECKING || _state.value.phase == UpdatePhase.DOWNLOADING) return
        refreshJob?.cancel()
        _state.value = UpdateState(phase = UpdatePhase.CHECKING)
        job = viewModelScope.launch {
            try {
                repository.refresh(force = true)
                val status = repository.status.value
                val latest = status.latest
                _state.value = when {
                    status.errorMessage != null ->
                        UpdateState(phase = UpdatePhase.ERROR, message = status.errorMessage)
                    !status.isUpdateAvailable || latest == null ->
                        UpdateState(phase = UpdatePhase.UP_TO_DATE)
                    latest.version.isBlank() ->
                        UpdateState(
                            phase = UpdatePhase.ERROR,
                            message = strings.get(R.string.update_error_no_version)
                        )
                    latest.apkUrl.isNullOrBlank() ->
                        UpdateState(
                            phase = UpdatePhase.ERROR,
                            message = strings.get(R.string.update_error_no_apk, latest.tagName)
                        )
                    else ->
                        UpdateState(phase = UpdatePhase.AVAILABLE, release = latest.toReleaseInfo())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = UpdateState(
                    phase = UpdatePhase.ERROR,
                    message = strings.get(R.string.update_error_unreadable)
                )
            }
        }
    }

    /**
     * Downloads [release] (from the prompt) without a second GitHub lookup. Does nothing when
     * the release has no APK — the UI opens its page instead.
     */
    fun download(release: AppRelease) {
        if (release.apkUrl.isNullOrBlank()) return
        val info = release.toReleaseInfo()
        val current = _state.value
        if (current.phase == UpdatePhase.DOWNLOADING) return
        if (current.phase == UpdatePhase.READY && current.release?.version == info.version) return
        _state.value = UpdateState(phase = UpdatePhase.AVAILABLE, release = info)
        download()
    }

    fun download() {
        val release = _state.value.release ?: return
        if (release.apkUrl.isBlank() || _state.value.phase == UpdatePhase.DOWNLOADING) return
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
            } catch (_: Exception) {
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

    /** Intent for the UI to launch: unknown-sources permission screen or package installer. */
    fun installIntent(): Intent? {
        val apk = _state.value.apk ?: return null
        return if (manager.canInstallPackages()) manager.installIntent(apk) else manager.unknownSourcesSettingsIntent()
    }

    fun needsInstallPermission(): Boolean = !manager.canInstallPackages()
}

/** `true` when this download state belongs to [version] (progress or ready APK of that release). */
fun UpdateState.isFor(version: String): Boolean =
    release?.let { VersionComparator.compare(it.version, version) == 0 } == true

/** Maps a changelog release to the form used by [UpdateManager] (empty `apkUrl` when no APK asset is attached). */
private fun AppRelease.toReleaseInfo(): ReleaseInfo = ReleaseInfo(
    tag = tagName,
    version = version,
    notes = notesMarkdown,
    apkUrl = apkUrl?.trim().orEmpty(),
    apkName = apkName ?: "Taal-$version.apk",
    apkSize = apkSizeBytes ?: 0L,
    pageUrl = htmlUrl
)
