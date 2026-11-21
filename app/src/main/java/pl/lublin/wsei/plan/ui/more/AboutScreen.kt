@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
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
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiCardHeader
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

private const val AUTHOR_LOGIN = "m1chi0001"
private const val LINK_AUTHOR = "https://github.com/$AUTHOR_LOGIN"
private const val LINK_REPOSITORY = "https://github.com/${UpdateManager.REPO}"
private const val LINK_RELEASES = UpdateManager.RELEASES_PAGE

/** Minimum width of the About page before its cards are laid out in two columns. */
private val AboutTwoColumnMinWidth = 560.dp

/**
 * Libraries the app is actually built with (app/build.gradle.kts and gradle/libs.versions.toml).
 * Names of libraries are not translated.
 */
private val TechStack = listOf(
    "Kotlin",
    "Jetpack Compose",
    "Material 3 Expressive",
    "Coroutines & Flow",
    "kotlinx.serialization",
    "Ktor",
    "Room",
    "DataStore",
    "Hilt",
    "WorkManager",
    "Jsoup"
)

/**
 * „O aplikacji”: ekspresyjny nagłówek z ikoną, wersją i skrótami do GitHuba, karty projektu,
 * technologii, licencji i aktualizacji. Od [AboutTwoColumnMinWidth] karty układają się
 * w dwie kolumny (okna średnie, szeroki panel szczegółów).
 */
@Composable
internal fun AboutScreen(onBack: (() -> Unit)?, embedded: Boolean) {
    val context = LocalContext.current
    val openLink: (String) -> Unit = { url -> openExternalLink(context, url) }
    WseiScreenFrame(
        title = stringResource(R.string.about_title),
        onBack = onBack,
        showProfileShortcut = !embedded && onBack == null
    ) { scrollModifier ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = if (maxWidth >= AboutTwoColumnMinWidth) 2 else 1
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier),
                contentPadding = wseiScreenContentPadding(),
                verticalItemSpacing = WseiSpacing.betweenCards,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
            ) {
                item(key = "about_hero", contentType = "hero", span = StaggeredGridItemSpan.FullLine) {
                    AboutHeroCard(
                        onOpenRepository = { openLink(LINK_REPOSITORY) },
                        onOpenReleases = { openLink(LINK_RELEASES) }
                    )
                }
                item(key = "about_update", contentType = "update") {
                    UpdateCard()
                }
                item(key = "about_project", contentType = "project") {
                    AboutProjectCard(onOpenLink = openLink)
                }
                item(key = "about_tech", contentType = "tech") {
                    AboutTechStackCard()
                }
                item(key = "about_licence", contentType = "licence") {
                    AboutLicenceCard()
                }
                item(key = "about_disclaimer", contentType = "disclaimer", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        text = stringResource(R.string.about_disclaimer),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = WseiSpacing.xs)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Karty
// -----------------------------------------------------------------------------------------

/** Nagłówek: ikona aplikacji w kształcie MaterialShapes, nazwa, opis, wersja i skróty. */
@Composable
private fun AboutHeroCard(onOpenRepository: () -> Unit, onOpenReleases: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    WseiCard(
        shape = WseiHeroShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        contentPadding = PaddingValues(horizontal = WseiSpacing.xxl, vertical = WseiSpacing.xxl),
        verticalSpacing = WseiSpacing.l
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            // Adaptive launcher icons (mipmap) cannot be loaded as painters; the vector foreground
            // is drawn as a monochrome mark that follows the selected palette.
            Box(
                modifier = Modifier
                    .size(120.dp)
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
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colors.onPrimaryContainer.copy(alpha = 0.85f)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                WseiPill(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    icon = WseiIcons.Info,
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    emphasized = true
                )
                WseiPill(
                    text = stringResource(R.string.about_min_android),
                    containerColor = colors.onPrimaryContainer.copy(alpha = 0.1f),
                    contentColor = colors.onPrimaryContainer
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            Button(onClick = onOpenRepository, shape = CircleShape) {
                Icon(imageVector = WseiIcons.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.about_source_code),
                    modifier = Modifier.padding(start = WseiSpacing.s)
                )
            }
            FilledTonalButton(onClick = onOpenReleases, shape = CircleShape) {
                Icon(imageVector = WseiIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.about_releases),
                    modifier = Modifier.padding(start = WseiSpacing.s)
                )
            }
        }
    }
}

private class AboutLink(
    val key: String,
    val title: String,
    val supporting: String,
    val icon: ImageVector,
    val url: String
)

/** Projekt: autor, repozytorium i wydania na GitHubie — każdy wiersz otwiera przeglądarkę. */
@Composable
private fun AboutProjectCard(onOpenLink: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val links = listOf(
        AboutLink(
            key = "author",
            title = stringResource(R.string.about_author),
            supporting = stringResource(R.string.about_author_supporting, AUTHOR_LOGIN),
            icon = WseiIcons.Person,
            url = LINK_AUTHOR
        ),
        AboutLink(
            key = "repository",
            title = stringResource(R.string.about_source_code),
            supporting = LINK_REPOSITORY.removePrefix("https://"),
            icon = WseiIcons.Code,
            url = LINK_REPOSITORY
        ),
        AboutLink(
            key = "releases",
            title = stringResource(R.string.about_releases),
            supporting = stringResource(R.string.about_releases_supporting),
            icon = WseiIcons.Download,
            url = LINK_RELEASES
        )
    )
    WseiCard(containerColor = colors.surfaceContainer, verticalSpacing = WseiSpacing.l) {
        WseiCardHeader(
            title = stringResource(R.string.about_project_title),
            icon = WseiIcons.IntegrationInstructions,
            iconContainerColor = colors.tertiaryContainer,
            iconContentColor = colors.onTertiaryContainer
        )
        WseiListGroup(
            entries = links.map { link ->
                WseiListEntry(link.key) { shape ->
                    WseiListItem(
                        title = link.title,
                        supporting = link.supporting,
                        icon = link.icon,
                        shape = shape,
                        containerColor = colors.surfaceContainerHigh,
                        onClick = { onOpenLink(link.url) },
                        trailing = { WseiExternalLinkMark() }
                    )
                }
            }
        )
    }
}

@Composable
private fun AboutTechStackCard() {
    val colors = MaterialTheme.colorScheme
    WseiCard(containerColor = colors.surfaceContainer, verticalSpacing = WseiSpacing.l) {
        WseiCardHeader(
            title = stringResource(R.string.about_tech_title),
            icon = WseiIcons.Memory,
            iconContainerColor = colors.secondaryContainer,
            iconContentColor = colors.onSecondaryContainer
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            TechStack.forEachIndexed { index, name ->
                // The first three define the look of the app and are emphasised.
                val emphasized = index < 3
                WseiPill(
                    text = name,
                    containerColor = if (emphasized) colors.secondaryContainer else colors.surfaceContainerHighest,
                    contentColor = if (emphasized) colors.onSecondaryContainer else colors.onSurface,
                    emphasized = emphasized
                )
            }
        }
        Text(
            text = stringResource(R.string.about_tech_architecture),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AboutLicenceCard() {
    val colors = MaterialTheme.colorScheme
    WseiCard(containerColor = colors.surfaceContainer, verticalSpacing = WseiSpacing.l) {
        WseiCardHeader(
            title = stringResource(R.string.about_licence_title),
            icon = WseiIcons.Balance,
            iconContainerColor = colors.primaryContainer,
            iconContentColor = colors.onPrimaryContainer
        )
        WseiInset(containerColor = colors.surfaceContainerHigh) {
            WseiInfoRow(stringResource(R.string.about_licence_app), stringResource(R.string.about_licence_mit))
            WseiInfoRow(stringResource(R.string.about_licence_copyright), "© 2026 $AUTHOR_LOGIN")
            WseiInfoRow(stringResource(R.string.about_licence_font), "Google Sans Flex (OFL)")
            WseiInfoRow(stringResource(R.string.about_licence_data), stringResource(R.string.about_licence_data_value))
        }
    }
}

// -----------------------------------------------------------------------------------------
// Aktualizacje
// -----------------------------------------------------------------------------------------

/** Karta „Aktualizacje”: sprawdza wydania w GitHubie, pobiera APK i uruchamia instalator. */
@Composable
private fun UpdateCard(
    modifier: Modifier = Modifier,
    viewModel: UpdateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
    val highlighted = state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.READY

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

    WseiCard(
        modifier = modifier.animateContentSize(WseiMotion.size()),
        containerColor = if (highlighted) colors.secondaryContainer else colors.surfaceContainer,
        contentColor = if (highlighted) colors.onSecondaryContainer else colors.onSurface,
        verticalSpacing = WseiSpacing.m
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            if (state.phase == UpdatePhase.CHECKING) {
                LoadingIndicator(
                    modifier = Modifier.size(44.dp),
                    color = colors.primary
                )
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
                    }
                )
            }
        }

        if (state.phase == UpdatePhase.DOWNLOADING) {
            LinearWavyProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        val notes = release?.notes?.takeIf { it.isNotBlank() }
        if (notes != null && highlighted) {
            Text(
                text = stringResource(R.string.about_update_whats_new),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
            Text(
                text = notes.replace(Regex("[#*`]+"), "").trim(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSecondaryContainer,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            when (state.phase) {
                UpdatePhase.AVAILABLE -> Button(onClick = viewModel::download) {
                    Text(stringResource(R.string.about_update_download))
                }
                UpdatePhase.DOWNLOADING -> OutlinedButton(onClick = viewModel::cancelDownload) {
                    Text(stringResource(R.string.about_update_cancel))
                }
                UpdatePhase.READY -> Button(onClick = {
                    viewModel.installIntent()?.let { runCatching { context.startActivity(it) } }
                }) {
                    Text(
                        stringResource(
                            if (needsInstallPermission) R.string.about_update_allow_and_install else R.string.about_update_install
                        )
                    )
                }
                UpdatePhase.CHECKING -> Unit
                else -> FilledTonalButton(onClick = viewModel::checkForUpdate) {
                    Text(stringResource(R.string.about_update_check))
                }
            }
            if (highlighted) {
                OutlinedButton(onClick = viewModel::checkForUpdate) {
                    Text(stringResource(R.string.about_update_check_again))
                }
            }
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
