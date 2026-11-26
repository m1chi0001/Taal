@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.BuildConfig
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdateManager
import pl.lublin.wsei.plan.data.update.UpdatePhase
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiChevron
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.update.MarkdownHighlights
import pl.lublin.wsei.plan.ui.update.MarkdownParser
import pl.lublin.wsei.plan.ui.update.UpdateChangelogSheet

private const val AUTHOR_LOGIN = "m1chi0001"
private const val LINK_REPOSITORY = "https://github.com/${UpdateManager.REPO}"

/**
 * "About": a compact header (icon, name, version), updates, links to the changelog and GitHub,
 * and a single line about the technology and licence. Uses the shared [MorePage] frame, so its
 * top app bar matches the Announcements and Settings panes.
 */
@Composable
internal fun AboutScreen(
    onBack: (() -> Unit)?,
    embedded: Boolean,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showChangelog by rememberSaveable { mutableStateOf(false) }

    MorePage(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        embedded = embedded
    ) { itemModifier ->
        item(key = "about_header", contentType = "header") {
            AboutHeader(modifier = itemModifier)
        }
        item(key = "about_update", contentType = "update") {
            UpdateCard(viewModel = viewModel, modifier = itemModifier)
        }
        item(key = "about_links", contentType = "links") {
            val colors = MaterialTheme.colorScheme
            WseiListGroup(
                modifier = itemModifier,
                entries = listOf(
                    WseiListEntry("changelog") { shape ->
                        WseiListItem(
                            title = stringResource(R.string.about_whats_new_title),
                            supporting = stringResource(R.string.about_whats_new_supporting),
                            icon = WseiIcons.AutoAwesome,
                            iconContainerColor = colors.tertiaryContainer,
                            iconContentColor = colors.onTertiaryContainer,
                            shape = shape,
                            onClick = { showChangelog = true },
                            trailing = { WseiChevron() }
                        )
                    },
                    WseiListEntry("repository") { shape ->
                        WseiListItem(
                            title = stringResource(R.string.about_github),
                            supporting = LINK_REPOSITORY.removePrefix("https://"),
                            icon = WseiIcons.Code,
                            shape = shape,
                            onClick = { openExternalLink(context, LINK_REPOSITORY) },
                            trailing = { WseiExternalLinkMark() }
                        )
                    }
                )
            )
        }
        item(key = "about_footer", contentType = "footer") {
            AboutFooter(modifier = itemModifier)
        }
    }

    if (showChangelog) {
        UpdateChangelogSheet(onDismiss = { showChangelog = false }, viewModel = viewModel)
    }
}

// -----------------------------------------------------------------------------------------
// Header and footer
// -----------------------------------------------------------------------------------------

/** App icon in a MaterialShapes cookie, name, version and a short tagline. */
@Composable
private fun AboutHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    WseiCard(
        modifier = modifier,
        shape = WseiHeroShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        contentPadding = PaddingValues(WseiSpacing.xl)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            // Adaptive launcher icons (mipmap) cannot be loaded as painters; the vector foreground
            // is drawn as a monochrome mark that follows the selected palette.
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    // The foreground keeps its artwork inside the adaptive-icon safe zone (~60%).
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.4f)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                WseiPill(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    emphasized = true
                )
            }
        }
        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer.copy(alpha = 0.85f)
        )
    }
}

/** Technology, author, licence and disclaimer — in small print. */
@Composable
private fun AboutFooter(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = WseiSpacing.s, vertical = WseiSpacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
    ) {
        Text(
            text = stringResource(R.string.about_credits_line),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.about_licence_line, AUTHOR_LOGIN),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.about_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

// -----------------------------------------------------------------------------------------
// Updates
// -----------------------------------------------------------------------------------------

/**
 * "Updates": a one-line status with a "Check for updates" button (forced refresh) and, when a
 * new version exists, its highlights, download and installation.
 */
@Composable
private fun UpdateCard(viewModel: UpdateViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.updateStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val release = state.release
    val releaseVersion = release?.version.orEmpty()
    // Re-checked on every resume: the user may have just granted "install unknown apps".
    var needsInstallPermission by remember { mutableStateOf(viewModel.needsInstallPermission()) }
    LifecycleResumeEffect(viewModel) {
        needsInstallPermission = viewModel.needsInstallPermission()
        onPauseOrDispose { }
    }
    val highlighted = state.phase == UpdatePhase.AVAILABLE ||
        state.phase == UpdatePhase.DOWNLOADING ||
        state.phase == UpdatePhase.READY
    val isChecking = state.phase == UpdatePhase.CHECKING || (state.phase == UpdatePhase.IDLE && status.isChecking)

    val statusText = when (state.phase) {
        UpdatePhase.IDLE -> stringResource(R.string.about_update_idle, viewModel.currentVersion)
        UpdatePhase.CHECKING -> stringResource(R.string.about_update_checking)
        UpdatePhase.UP_TO_DATE -> stringResource(R.string.about_update_up_to_date, viewModel.currentVersion)
        UpdatePhase.AVAILABLE -> stringResource(R.string.about_update_available, releaseVersion, viewModel.currentVersion)
        UpdatePhase.DOWNLOADING -> stringResource(
            R.string.about_update_downloading,
            releaseVersion,
            (state.progress * 100).toInt()
        )
        UpdatePhase.READY -> stringResource(R.string.about_update_ready, releaseVersion)
        UpdatePhase.ERROR -> state.message ?: stringResource(R.string.about_update_error)
    }
    val highlights = remember(release?.notes) {
        release?.notes?.let { MarkdownParser.highlights(it, max = 3) }.orEmpty()
    }

    WseiCard(
        modifier = modifier,
        containerColor = if (highlighted) colors.secondaryContainer else colors.surfaceContainer,
        contentColor = if (highlighted) colors.onSecondaryContainer else colors.onSurface,
        verticalSpacing = WseiSpacing.m
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            if (isChecking) {
                LoadingIndicator(modifier = Modifier.size(44.dp), color = colors.primary)
            } else {
                WseiIconBadge(
                    icon = WseiIcons.Download,
                    shape = if (highlighted) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
                    containerColor = if (highlighted) colors.primary else colors.secondaryContainer,
                    contentColor = if (highlighted) colors.onPrimary else colors.onSecondaryContainer,
                    size = 44.dp,
                    iconSize = 22.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.about_update_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        state.phase == UpdatePhase.ERROR -> colors.error
                        highlighted -> colors.onSecondaryContainer
                        else -> colors.onSurfaceVariant
                    },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Details appear with a single expand animation (no extra animateContentSize).
        AnimatedVisibility(
            visible = highlighted,
            enter = expandVertically(WseiMotion.size(), expandFrom = Alignment.Top) + fadeIn(WseiMotion.effects()),
            exit = shrinkVertically(WseiMotion.size(), shrinkTowards = Alignment.Top) + fadeOut(WseiMotion.fastEffects())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
                if (state.phase == UpdatePhase.DOWNLOADING) {
                    LinearWavyProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (highlights.isNotEmpty()) {
                    MarkdownHighlights(
                        items = highlights,
                        color = colors.onSecondaryContainer,
                        markerColor = colors.onSecondaryContainer
                    )
                }
                if (state.phase == UpdatePhase.READY && needsInstallPermission) {
                    Text(
                        text = stringResource(R.string.about_update_permission_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSecondaryContainer
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            when (state.phase) {
                UpdatePhase.AVAILABLE -> Button(onClick = viewModel::download, shape = CircleShape) {
                    Text(stringResource(R.string.update_prompt_download))
                }
                UpdatePhase.DOWNLOADING -> OutlinedButton(onClick = viewModel::cancelDownload, shape = CircleShape) {
                    Text(stringResource(R.string.about_update_cancel))
                }
                UpdatePhase.READY -> Button(
                    onClick = { viewModel.installIntent()?.let { runCatching { context.startActivity(it) } } },
                    shape = CircleShape
                ) {
                    Text(
                        stringResource(
                            if (needsInstallPermission) R.string.about_update_allow_and_install else R.string.about_update_install
                        )
                    )
                }
                UpdatePhase.CHECKING -> Unit
                else -> FilledTonalButton(
                    onClick = viewModel::checkForUpdate,
                    enabled = !isChecking,
                    shape = CircleShape
                ) {
                    Text(stringResource(R.string.about_update_check))
                }
            }
            if (state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.READY) {
                OutlinedButton(onClick = viewModel::checkForUpdate, shape = CircleShape) {
                    Text(stringResource(R.string.about_update_check_again))
                }
            }
        }
    }
}
