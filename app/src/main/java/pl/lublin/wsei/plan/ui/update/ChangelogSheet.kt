@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdateManager
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.update.AppRelease
import pl.lublin.wsei.plan.domain.update.AppUpdateStatus
import pl.lublin.wsei.plan.domain.update.VersionComparator
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiExpandableCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.more.openExternalLink
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import pl.lublin.wsei.plan.l10n.appLocale

/**
 * "Historia zmian" sheet wired to the shared [UpdateViewModel]. Opened from the update prompt,
 * "O aplikacji" and the settings. Loads the changelog when nothing is cached yet.
 */
@Composable
fun UpdateChangelogSheet(
    onDismiss: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val status by viewModel.updateStatus.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.loadChangelogIfMissing() }
    ChangelogSheet(
        status = status,
        onDismiss = onDismiss,
        onRetry = viewModel::reloadChangelog
    )
}

/** All releases, newest first: version badge, title, date and rendered notes. */
@Composable
fun ChangelogSheet(
    status: AppUpdateStatus,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val locale = appLocale()
    // The newest release is open by default; the rest stay compact until tapped.
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = colors.surfaceContainerLow
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = WseiSpacing.l, end = WseiSpacing.l, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            item(key = "changelog_header", contentType = "header") {
                ChangelogHeader(modifier = Modifier.padding(bottom = WseiSpacing.s))
            }
            val releases = status.releases
            if (releases.isEmpty()) {
                item(key = "changelog_empty", contentType = "empty") {
                    ChangelogEmpty(
                        isLoading = status.isChecking,
                        onRetry = onRetry,
                        onOpenGitHub = { openExternalLink(context, UpdateManager.RELEASES_PAGE) }
                    )
                }
            } else {
                itemsIndexed(
                    items = releases,
                    key = { index, release -> "release_${index}_${release.tagName}" },
                    contentType = { _, _ -> "release" }
                ) { index, release ->
                    val id = release.tagName.ifBlank { release.version }
                    ReleaseCard(
                        release = release,
                        isLatest = index == 0,
                        isInstalled = VersionComparator.compare(release.version, status.currentVersion) == 0,
                        dateLabel = remember(release.publishedAtIso, locale) {
                            formatReleaseDate(release.publishedAtIso, locale)
                        },
                        expanded = expanded[id] ?: (index == 0),
                        onToggle = { expanded[id] = !(expanded[id] ?: (index == 0)) },
                        onOpenRelease = { openExternalLink(context, release.htmlUrl) }
                    )
                }
                item(key = "changelog_footer", contentType = "footer") {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { openExternalLink(context, UpdateManager.RELEASES_PAGE) }) {
                            Text(stringResource(R.string.changelog_open_github))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangelogHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
    ) {
        WseiIconBadge(
            icon = UpdateIcons.History,
            shape = MaterialShapes.Cookie9Sided.toShape(),
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            size = 48.dp,
            iconSize = 24.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.changelog_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.changelog_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChangelogEmpty(isLoading: Boolean, onRetry: () -> Unit, onOpenGitHub: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    WseiCard(containerColor = colors.surfaceContainer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WseiSpacing.s),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.size(48.dp), color = colors.primary)
                Text(
                    text = stringResource(R.string.changelog_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(R.string.changelog_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
                ) {
                    FilledTonalButton(onClick = onRetry, shape = CircleShape) {
                        Text(stringResource(R.string.changelog_retry))
                    }
                    OutlinedButton(onClick = onOpenGitHub, shape = CircleShape) {
                        Text(stringResource(R.string.changelog_open_github))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseCard(
    release: AppRelease,
    isLatest: Boolean,
    isInstalled: Boolean,
    dateLabel: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenRelease: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val title = release.title.trim().takeUnless { isJustVersion(it, release) }
    WseiExpandableCard(
        expanded = expanded,
        onToggle = onToggle,
        containerColor = if (isLatest) colors.surfaceContainerHigh else colors.surfaceContainer,
        header = {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    WseiPill(
                        text = stringResource(R.string.changelog_version_badge, release.version),
                        containerColor = if (isLatest) colors.primary else colors.secondaryContainer,
                        contentColor = if (isLatest) colors.onPrimary else colors.onSecondaryContainer,
                        emphasized = true
                    )
                    if (isLatest) {
                        WseiPill(
                            text = stringResource(R.string.changelog_latest),
                            containerColor = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer
                        )
                    }
                    if (isInstalled) {
                        WseiPill(
                            text = stringResource(R.string.changelog_installed),
                            containerColor = colors.surfaceContainerHighest,
                            contentColor = colors.onSurface
                        )
                    }
                }
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (dateLabel != null) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        body = {
            if (release.notesMarkdown.isBlank()) {
                Text(
                    text = stringResource(R.string.changelog_no_notes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            } else {
                MarkdownText(markdown = release.notesMarkdown, modifier = Modifier.fillMaxWidth())
            }
            TextButton(
                onClick = onOpenRelease,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.changelog_open_release))
            }
        }
    )
}

/** Titles like "v0.7.6" or "Taal 0.7.6" add nothing next to the version badge. */
private fun isJustVersion(title: String, release: AppRelease): Boolean {
    if (title.isBlank()) return true
    val normalized = VersionComparator.normalize(title.removePrefix("Taal").trim())
    return normalized == release.version || title == release.tagName
}

/** "2026-11-20T12:34:56Z" → localized medium date ("20 lis 2026"); null when unparsable. */
private fun formatReleaseDate(iso: String?, locale: Locale): String? {
    if (iso.isNullOrBlank()) return null
    val date = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
        ?: return null
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }.getOrNull()
}
