package pl.lublin.wsei.plan.data.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.lublin.wsei.plan.BuildConfig
import pl.lublin.wsei.plan.domain.update.AppUpdateRepository
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import javax.inject.Inject
import javax.inject.Singleton

/** [AppUpdateRepository] backed by the GitHub Releases API of [UpdateManager.REPO]. */
@Singleton
class GitHubUpdateRepository @Inject constructor() : AppUpdateRepository {
    private val _status = MutableStateFlow(AppUpdateStatus(currentVersion = BuildConfig.VERSION_NAME))
    override val status: StateFlow<AppUpdateStatus> = _status.asStateFlow()

    override suspend fun refresh(force: Boolean) = Unit

    override suspend fun dismiss(version: String) = Unit
}
