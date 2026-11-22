@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package pl.lublin.wsei.plan.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdatePhase
import pl.lublin.wsei.plan.data.update.UpdateState
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.data.update.isFor
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.more.openExternalLink
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/**
 * Non-intrusive "update available" prompt wired to [viewModel]. Springs in when the repository
 * says [pl.lublin.wsei.plan.domain.update.AppUpdateStatus.shouldPrompt] and [allowed] is true
 * (e.g. hidden on the About page, which has its own update card). Dismissing hides it for this
 * version only.
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
    val context = LocalContext.current
    // Re-checked on every resume: the user may have just granted "install unknown apps".
    var needsInstallPermission by remember { mutableStateOf(viewModel.needsInstallPermission()) }
    LifecycleResumeEffect(viewModel) {
        needsInstallPermission = viewModel.needsInstallPermission()
        onPauseOrDispose { }
    }

    // The last release stays in memory for the exit animation after "dismiss".
    val latest = status.latest
    val lastRelease = remember { arrayOfNulls<AppRelease>(1) }
    if (latest != null) lastRelease[0] = latest

    AnimatedVisibility(
        visible = allowed && status.shouldPrompt && latest != null,
        modifier = modifier,
        enter = fadeIn(WseiMotion.effects()) +
            slideInVertically(WseiMotion.spatial()) { it / 3 } +
            scaleIn(WseiMotion.spatial(), initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)),
        exit = fadeOut(WseiMotion.fastEffects()) +
            slideOutVertically(WseiMotion.size()) { it / 3 } +
            scaleOut(WseiMotion.size(), targetScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 1f))
    ) {
        val release = lastRelease[0] ?: return@AnimatedVisibility
        UpdatePromptCard(
            release = release,
            currentVersion = status.currentVersion,
            download = download,
            needsInstallPermission = needsInstallPermission,
            onDownload = {
                if (release.apkUrl.isNullOrBlank()) {
                    openExternalLink(context, release.htmlUrl)
                } else {
                    viewModel.download(release)
                }
            },
            onCancelDownload = viewModel::cancelDownload,
            onInstall = { viewModel.installIntent()?.let { runCatching { context.startActivity(it) } } },
            onShowChanges = onShowChanges,
            onDismiss = { viewModel.dismissPrompt(release.version) }
        )
    }
}

/** The banner itself: version badge, 2–3 highlights and the actions. */
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
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val highlights = remember(release.notesMarkdown) { MarkdownParser.highlights(release.notesMarkdown, max = 3) }
    val ownsDownload = download.isFor(release.version)
    val phase = if (ownsDownload) download.phase else UpdatePhase.AVAILABLE
    val hasApk = !release.apkUrl.isNullOrBlank()

    Surface(
        shape = WseiCardShape,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shadowElevation = 6.dp,
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(bottom = WseiSpacing.m)
    ) {
        // One size animation for the whole card (progress / install rows swapping in).
        Column(
            modifier = Modifier
                .animateContentSize(WseiMotion.size())
                .padding(start = WseiSpacing.l, end = WseiSpacing.xs, top = WseiSpacing.m, bottom = WseiSpacing.m),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                WseiIconBadge(
                    icon = UpdateIcons.NewReleases,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    size = 44.dp,
                    iconSize = 22.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                ) {
                    Text(
                        text = stringResource(R.string.update_prompt_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
                    ) {
                        WseiPill(
                            text = stringResource(R.string.changelog_version_badge, release.version),
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                            emphasized = true
                        )
                        Text(
                            text = stringResource(R.string.update_prompt_installed, currentVersion),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = UpdateIcons.Close,
                        contentDescription = stringResource(R.string.update_prompt_dismiss),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (highlights.isNotEmpty() && phase != UpdatePhase.DOWNLOADING) {
                MarkdownHighlights(
                    items = highlights,
                    color = colors.onPrimaryContainer,
                    markerColor = colors.primary,
                    modifier = Modifier.padding(end = WseiSpacing.m)
                )
            }

            when (phase) {
                UpdatePhase.DOWNLOADING -> Column(
                    modifier = Modifier.padding(end = WseiSpacing.m),
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
                UpdatePhase.ERROR -> Text(
                    text = download.message ?: stringResource(R.string.about_update_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                    modifier = Modifier.padding(end = WseiSpacing.m)
                )
                else -> Unit
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                when (phase) {
                    UpdatePhase.DOWNLOADING -> TextButton(onClick = onCancelDownload) {
                        Text(stringResource(R.string.about_update_cancel))
                    }
                    UpdatePhase.READY -> Button(onClick = onInstall, shape = CircleShape) {
                        Text(
                            stringResource(
                                if (needsInstallPermission) R.string.about_update_allow_and_install else R.string.about_update_install
                            )
                        )
                    }
                    else -> Button(onClick = onDownload, shape = CircleShape) {
                        Text(
                            text = stringResource(
                                if (hasApk) R.string.update_prompt_download else R.string.update_prompt_open_release
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TextButton(onClick = onShowChanges) {
                    Text(
                        text = stringResource(R.string.update_prompt_changes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
