package pl.lublin.wsei.plan.data.update

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String get() = manager.currentVersion

    private var job: Job? = null

    fun checkForUpdate() {
        if (_state.value.phase == UpdatePhase.CHECKING || _state.value.phase == UpdatePhase.DOWNLOADING) return
        _state.value = UpdateState(phase = UpdatePhase.CHECKING)
        job = viewModelScope.launch {
            _state.value = when (val result = manager.checkForUpdate()) {
                is UpdateCheckResult.UpToDate -> UpdateState(phase = UpdatePhase.UP_TO_DATE)
                is UpdateCheckResult.Available -> UpdateState(phase = UpdatePhase.AVAILABLE, release = result.release)
                is UpdateCheckResult.Failure -> UpdateState(phase = UpdatePhase.ERROR, message = result.message)
            }
        }
    }

    fun download() {
        val release = _state.value.release ?: return
        if (_state.value.phase == UpdatePhase.DOWNLOADING) return
        _state.update { it.copy(phase = UpdatePhase.DOWNLOADING, progress = 0f, message = null) }
        job = viewModelScope.launch {
            try {
                val apk = manager.downloadApk(release) { p -> _state.update { it.copy(progress = p) } }
                _state.update { it.copy(phase = UpdatePhase.READY, apk = apk, progress = 1f) }
            } catch (e: UpdateException) {
                _state.update { it.copy(phase = UpdatePhase.ERROR, message = e.message) }
            }
        }
    }

    fun cancelDownload() {
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
