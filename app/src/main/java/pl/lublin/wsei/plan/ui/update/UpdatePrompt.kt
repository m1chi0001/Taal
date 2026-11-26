@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.update

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdatePhase
import pl.lublin.wsei.plan.data.update.UpdateState
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.data.update.isFor
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.domain.update.VersionComparator
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.more.openExternalLink
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/**
 * Automatic "update available" dialog wired to [viewModel]. Appears when the repository reports
 * [pl.lublin.wsei.plan.domain.update.AppUpdateStatus.shouldPrompt], the user has not snoozed this
 * version for the current session, and [allowed] is true (hidden on "O aplikacji", which already
 * shows its own update card, and while the full changelog sheet is open).
 */
@Composable
fun UpdatePromptHost(
    viewModel: UpdateViewModel,
    allowed: Boolean,
    onShowChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status by viewModel.updateStatus.collectAsStateWithLifecycle()
    val download by viewModel.state.collectAsStateWithLifecycle()
    val snoozedVersion by viewModel.snoozedVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Re-checked on every resume: the user may have just granted "install unknown apps".
    var needsInstallPermission by remember { mutableStateOf(viewModel.needsInstallPermission()) }
    LifecycleResumeEffect(viewModel) {
        needsInstallPermission = viewModel.needsInstallPermission()
        onPauseOrDispose { }
    }

    val latest = status.latest ?: return
    val normalizedLatest = VersionComparator.normalize(latest.version)
    val isDownloadingLatest = download.isFor(latest.version) && download.phase == UpdatePhase.DOWNLOADING
    val shouldShow = allowed && status.shouldPrompt && snoozedVersion != normalizedLatest

    if (!shouldShow) return

    Dialog(
        onDismissRequest = {
            if (!isDownloadingLatest) {
                viewModel.snoozePromptForSession(latest.version)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloadingLatest,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        UpdatePromptCard(
            release = latest,
            currentVersion = status.currentVersion,
            download = download,
            needsInstallPermission = needsInstallPermission,
            onDownload = {
                if (latest.apkUrl.isNullOrBlank()) {
                    openExternalLink(context, latest.htmlUrl)
                } else {
                    viewModel.download(latest)
                }
            },
            onCancelDownload = viewModel::cancelDownload,
            onInstall = { viewModel.installIntent()?.let { runCatching { context.startActivity(it) } } },
            onShowChanges = onShowChanges,
            onSnooze = { viewModel.snoozePromptForSession(latest.version) },
            onDismiss = { viewModel.dismissPrompt(latest.version) },
            modifier = modifier
        )
    }
}

/**
 * Material 3 Expressive update card: header with version pills, scrollable full Markdown release
 * notes (never truncated at 3 bullets or 2 lines), live download progress, and action buttons.
 */
@Composable
fun UpdatePromptCard(
    release: AppRelease,
    currentVersion: String,
    download: UpdateState,
    needsInstallPermission: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onInstall: () -> Unit,
    onShowChanges: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit = onDismiss,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val ownsDownload = download.isFor(release.version)
    val phase = if (ownsDownload) download.phase else UpdatePhase.AVAILABLE
    val hasApk = !release.apkUrl.isNullOrBlank()
    val releaseTitle = remember(release.title, release.version, release.tagName) {
        release.title.trim().takeUnless { raw ->
            raw.isBlank() ||
                raw == release.tagName ||
                VersionComparator.normalize(raw.removePrefix("Taal").trim()) == release.version
        }
    }

    Surface(
        shape = WseiHeroShape,
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        modifier = modifier
            .padding(horizontal = WseiSpacing.l, vertical = WseiSpacing.l)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .heightIn(max = 560.dp)
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(WseiMotion.size())
                .padding(WseiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                WseiIconBadge(
                    icon = UpdateIcons.NewReleases,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    size = 48.dp,
                    iconSize = 24.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.update_prompt_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        WseiPill(
                            text = stringResource(R.string.changelog_version_badge, release.version),
                            containerColor = colors.primaryContainer,
                            contentColor = colors.onPrimaryContainer,
                            emphasized = true
                        )
                        Text(
                            text = stringResource(R.string.update_prompt_installed, currentVersion),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = UpdateIcons.Close,
                        contentDescription = stringResource(R.string.update_prompt_dismiss),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (releaseTitle != null) {
                Text(
                    text = releaseTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }

            if (release.notesMarkdown.isNotBlank()) {
                val notesScroll = rememberScrollState()
                Surface(
                    shape = WseiInsetShape,
                    color = colors.surfaceContainer,
                    contentColor = colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(notesScroll)
                            .padding(WseiSpacing.l)
                    ) {
                        MarkdownText(
                            markdown = release.notesMarkdown,
                            containerColor = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            when (phase) {
                UpdatePhase.DOWNLOADING -> Column(
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
                ) {
                    Text(
                        text = stringResource(R.string.update_prompt_downloading, (download.progress * 100).toInt()),
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearWavyProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                UpdatePhase.ERROR -> Surface(
                    shape = WseiInsetShape,
                    color = colors.errorContainer,
                    contentColor = colors.onErrorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = download.message ?: stringResource(R.string.about_update_error),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(WseiSpacing.m)
                    )
                }
                UpdatePhase.READY -> if (needsInstallPermission) {
                    Text(
                        text = stringResource(R.string.about_update_permission_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                else -> Unit
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                when (phase) {
                    UpdatePhase.DOWNLOADING -> {
                        TextButton(
                            onClick = onShowChanges,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.update_prompt_changes))
                        }
                        OutlinedButton(
                            onClick = onCancelDownload,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.about_update_cancel))
                        }
                    }
                    else -> {
                        TextButton(
                            onClick = onSnooze,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.update_prompt_later))
                        }
                        TextButton(
                            onClick = onShowChanges,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(stringResource(R.string.update_prompt_changes))
                        }
                        if (phase == UpdatePhase.READY) {
                            Button(
                                onClick = onInstall,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(
                                    stringResource(
                                        if (needsInstallPermission) {
                                            R.string.about_update_allow_and_install
                                        } else {
                                            R.string.about_update_install
                                        }
                                    )
                                )
                            }
                        } else {
                            Button(
                                onClick = onDownload,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(
                                    stringResource(
                                        if (hasApk) {
                                            R.string.update_prompt_download
                                        } else {
                                            R.string.update_prompt_open_release
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
