@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.lublin.wsei.plan.data.update.UpdatePhase
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiBottomClearance
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiChevron
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiInfoRow
import pl.lublin.wsei.plan.ui.components.WseiInset
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiSwitch
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.navigation.MoreSubView
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing

/** Wersja aplikacji z manifestu — bez ręcznego wpisywania w kodzie. */
@Composable
private fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
}

private const val LINK_VIRTUAL_DEAN = "https://dziekanat.wsei.lublin.pl/"
private const val LINK_ELEARNING = "https://dl.wsei.pl/"

/** Minimum width of a hub tile before the grid drops a column. */
private val HubTileMinWidth = 220.dp

/** Strzałka oznaczająca, że pozycja otwiera zewnętrzną stronę w przeglądarce. */
@Composable
private fun WseiExternalLinkMark() {
    Icon(
        imageVector = WseiIcons.ArrowForward,
        contentDescription = "Otwiera w przeglądarce",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/**
 * Zakładka „Więcej”: hub oraz podstrony Ogłoszenia, Ustawienia i O aplikacji.
 *
 * - telefon: hub i podstrony zastępują się nawzajem ([currentSubView] z powłoki),
 * - okno średnie: hub jako siatka kafelków,
 * - okno szerokie / rozłożony foldable: hub po lewej, wybrana podstrona po prawej.
 */
@Composable
fun MoreScreen(
    uiState: ScheduleUiState,
    currentSubView: MoreSubView,
    onNavigateSubView: (MoreSubView) -> Unit,
    onMarkAnnouncementRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    val windowLayout = LocalWseiWindowLayout.current
    val subPage: @Composable (MoreSubView, embedded: Boolean) -> Unit = { subView, embedded ->
        val onBack: (() -> Unit)? = if (embedded) null else ({ onNavigateSubView(MoreSubView.HUB) })
        when (subView) {
            MoreSubView.HUB -> if (embedded) {
                MoreDetailPlaceholder(
                    unreadCount = uiState.unreadAnnouncementsCount,
                    onOpenAnnouncements = { onNavigateSubView(MoreSubView.ANNOUNCEMENTS) }
                )
            } else {
                MoreHub(uiState = uiState, onOpen = onNavigateSubView)
            }
            MoreSubView.ANNOUNCEMENTS -> AnnouncementsScreen(
                uiState = uiState,
                onBack = onBack,
                embedded = embedded,
                onMarkRead = onMarkAnnouncementRead,
                onDownloadAttachment = onDownloadAttachment,
                onRefresh = onRefresh
            )
            MoreSubView.SETTINGS -> SettingsScreen(
                uiState = uiState,
                onBack = onBack,
                embedded = embedded,
                onSelectThemeMode = onSelectThemeMode,
                onToggleLiveNotification = onToggleLiveNotification,
                onToggleHideSensitiveData = onToggleHideSensitiveData
            )
            MoreSubView.ABOUT -> AboutScreen(onBack = onBack, embedded = embedded)
        }
    }

    if (windowLayout.supportsTwoPane) {
        WseiTwoPane(
            modifier = Modifier.fillMaxSize(),
            firstPaneFraction = 0.4f,
            // Each pane has its own screen padding, so no extra gap is needed between them.
            gap = 0.dp,
            first = {
                MoreHub(
                    uiState = uiState,
                    onOpen = onNavigateSubView,
                    selected = currentSubView,
                    forceList = true
                )
            },
            second = {
                AnimatedContent(
                    targetState = currentSubView,
                    transitionSpec = {
                        (fadeIn(WseiMotion.effects()) +
                            slideInHorizontally(WseiMotion.spatial()) { width -> width / 12 }) togetherWith
                            fadeOut(WseiMotion.effects())
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "moreDetailTransition"
                ) { subView -> subPage(subView, true) }
            }
        )
    } else {
        AnimatedContent(
            targetState = currentSubView,
            transitionSpec = {
                val forward = targetState != MoreSubView.HUB
                (fadeIn(WseiMotion.effects()) +
                    slideInHorizontally(WseiMotion.spatial()) { width -> if (forward) width / 8 else -width / 8 }) togetherWith
                    fadeOut(WseiMotion.effects())
            },
            modifier = Modifier.fillMaxSize(),
            label = "moreSubViewTransition"
        ) { subView -> subPage(subView, false) }
    }
}

/**
 * Wspólna rama podstron „Więcej”. Osadzona w prawym panelu ([embedded]) nie ma przycisku
 * powrotu ani drugiego skrótu do profilu (ten jest już w panelu huba).
 */
@Composable
private fun MorePage(
    title: String,
    onBack: (() -> Unit)?,
    embedded: Boolean,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    content: LazyListScope.(itemModifier: Modifier) -> Unit
) {
    WseiScreenFrame(
        title = title,
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
        showProfileShortcut = !embedded && onBack == null
    ) { scrollModifier ->
        val itemModifier = Modifier.wseiReadableWidth()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            contentPadding = wseiScreenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
        ) {
            content(itemModifier)
        }
    }
}

// -----------------------------------------------------------------------------------------
// Hub
// -----------------------------------------------------------------------------------------

private class HubEntry(
    val key: String,
    val title: String,
    val supporting: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subView: MoreSubView? = null,
    val unreadCount: Int = 0
)

/** „1 nowe”, „3 nowe”, „5 nowych”, „12 nowych”, „22 nowe”. */
private fun polishNewCountLabel(count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    val word = if (count == 1 || (lastDigit in 2..4 && lastTwoDigits !in 12..14)) "nowe" else "nowych"
    return "$count $word"
}

@Composable
private fun MoreHub(
    uiState: ScheduleUiState,
    onOpen: (MoreSubView) -> Unit,
    selected: MoreSubView? = null,
    forceList: Boolean = false
) {
    val unread = uiState.unreadAnnouncementsCount
    val appVersion = rememberAppVersion()
    val context = LocalContext.current
    val windowLayout = LocalWseiWindowLayout.current
    val openLink: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    val sections = listOf(
        HubEntry(
            key = "announcements",
            title = "Ogłoszenia",
            supporting = "Wiadomości z dziekanatu i załączniki",
            icon = WseiIcons.Campaign,
            onClick = { onOpen(MoreSubView.ANNOUNCEMENTS) },
            subView = MoreSubView.ANNOUNCEMENTS,
            unreadCount = unread
        ),
        HubEntry(
            key = "settings",
            title = "Ustawienia",
            supporting = "Motyw, powiadomienia i prywatność",
            icon = WseiIcons.Settings,
            onClick = { onOpen(MoreSubView.SETTINGS) },
            subView = MoreSubView.SETTINGS
        ),
        HubEntry(
            key = "about",
            title = "O aplikacji",
            supporting = "Wersja $appVersion • licencje",
            icon = WseiIcons.Info,
            onClick = { onOpen(MoreSubView.ABOUT) },
            subView = MoreSubView.ABOUT
        )
    )
    val links = listOf(
        HubEntry(
            key = "link_dean",
            title = "Wirtualny dziekanat",
            supporting = "dziekanat.wsei.lublin.pl",
            icon = WseiIcons.Badge,
            onClick = { openLink(LINK_VIRTUAL_DEAN) }
        ),
        HubEntry(
            key = "link_elearning",
            title = "Platforma e-learningowa",
            supporting = "dl.wsei.pl",
            icon = WseiIcons.School,
            onClick = { openLink(LINK_ELEARNING) }
        )
    )
    // Tiles on medium windows; a compact list on phones and in the narrow hub pane.
    val useTiles = !forceList && !windowLayout.isCompact

    WseiScreenFrame(title = "Więcej") { scrollModifier ->
        val itemModifier = Modifier.wseiReadableWidth()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            contentPadding = wseiScreenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
        ) {
            item(key = "hub_sections", contentType = "hub_group") {
                if (useTiles) {
                    HubTileGrid(entries = sections, modifier = itemModifier)
                } else {
                    HubList(entries = sections, selected = selected, modifier = itemModifier)
                }
            }
            item(key = "section_links", contentType = "section") {
                WseiSectionTitle("Przydatne linki", modifier = itemModifier)
            }
            item(key = "links_list", contentType = "hub_group") {
                if (useTiles) {
                    HubTileGrid(entries = links, modifier = itemModifier, shapeOffset = sections.size)
                } else {
                    HubList(entries = links, selected = null, modifier = itemModifier)
                }
            }
        }
    }
}

@Composable
private fun HubTrailing(entry: HubEntry) {
    if (entry.unreadCount > 0) {
        WseiPill(
            text = polishNewCountLabel(entry.unreadCount),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            emphasized = true
        )
    }
    if (entry.subView != null) WseiChevron() else WseiExternalLinkMark()
}

@Composable
private fun HubList(entries: List<HubEntry>, selected: MoreSubView?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    WseiListGroup(
        modifier = modifier,
        entries = entries.map { entry ->
            WseiListEntry(entry.key) { shape ->
                val hasUnread = entry.unreadCount > 0
                WseiListItem(
                    title = entry.title,
                    supporting = entry.supporting,
                    icon = entry.icon,
                    iconContainerColor = if (hasUnread) colors.error else colors.secondaryContainer,
                    iconContentColor = if (hasUnread) colors.onError else colors.onSecondaryContainer,
                    shape = shape,
                    selected = selected != null && entry.subView == selected,
                    onClick = entry.onClick,
                    trailing = { HubTrailing(entry) }
                )
            }
        }
    )
}

/** Expressive shapes for the hub tiles — each tile gets its own recognisable silhouette. */
@Composable
private fun hubTileShape(index: Int): Shape = when (index % 5) {
    0 -> MaterialShapes.Cookie9Sided
    1 -> MaterialShapes.Clover4Leaf
    2 -> MaterialShapes.Sunny
    3 -> MaterialShapes.SoftBurst
    else -> MaterialShapes.Cookie6Sided
}.toShape()

@Composable
private fun HubTileGrid(entries: List<HubEntry>, modifier: Modifier = Modifier, shapeOffset: Int = 0) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = (maxWidth / HubTileMinWidth).toInt().coerceIn(2, 4)
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)) {
            entries.chunked(columns).forEachIndexed { rowIndex, rowEntries ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                ) {
                    rowEntries.forEachIndexed { columnIndex, entry ->
                        HubTile(
                            entry = entry,
                            shape = hubTileShape(shapeOffset + rowIndex * columns + columnIndex),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    repeat(columns - rowEntries.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun HubTile(entry: HubEntry, shape: Shape, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val hasUnread = entry.unreadCount > 0
    WseiCard(
        modifier = modifier,
        onClick = entry.onClick,
        containerColor = colors.surfaceContainer,
        verticalSpacing = WseiSpacing.m
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WseiIconBadge(
                icon = entry.icon,
                shape = shape,
                containerColor = if (hasUnread) colors.error else colors.secondaryContainer,
                contentColor = if (hasUnread) colors.onError else colors.onSecondaryContainer,
                size = 52.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.weight(1f))
            HubTrailing(entry)
        }
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = entry.supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Prawy panel na szerokim ekranie, zanim użytkownik wybierze sekcję. */
@Composable
private fun MoreDetailPlaceholder(unreadCount: Int, onOpenAnnouncements: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = WseiSpacing.xxl, end = WseiSpacing.xxl, bottom = wseiBottomClearance()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(colors.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WseiIcons.Apps,
                    contentDescription = null,
                    tint = colors.onTertiaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = "Wybierz sekcję",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ogłoszenia, ustawienia i informacje o aplikacji otworzą się w tym miejscu.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onOpenAnnouncements, shape = CircleShape) {
                Icon(
                    imageVector = WseiIcons.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (unreadCount > 0) {
                        "Ogłoszenia • ${polishNewCountLabel(unreadCount)}"
                    } else {
                        "Pokaż ogłoszenia"
                    },
                    modifier = Modifier.padding(start = WseiSpacing.s)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Ogłoszenia
// -----------------------------------------------------------------------------------------

@Composable
private fun AnnouncementsScreen(
    uiState: ScheduleUiState,
    onBack: (() -> Unit)?,
    embedded: Boolean,
    onMarkRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onRefresh: () -> Unit
) {
    MorePage(
        title = "Ogłoszenia",
        onBack = onBack,
        embedded = embedded,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) { itemModifier ->
        if (uiState.announcements.isEmpty()) {
            item(key = "announcements_empty", contentType = "empty") {
                WseiEmptyState(
                    icon = WseiIcons.Campaign,
                    title = "Brak ogłoszeń",
                    message = "Nowe wiadomości z dziekanatu pojawią się tutaj.",
                    modifier = itemModifier
                )
            }
        }
        items(
            items = uiState.announcements,
            key = { "announcement_${it.id}" },
            contentType = { "announcement_card" }
        ) { announcement ->
            AnnouncementCard(
                announcement = announcement,
                onMarkRead = { onMarkRead(announcement) },
                onDownloadAttachment = onDownloadAttachment,
                modifier = itemModifier
            )
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: DeanAnnouncement,
    onMarkRead: () -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(announcement.id) { mutableStateOf(false) }
    val unread = !announcement.isRead
    val colors = MaterialTheme.colorScheme

    WseiCard(
        onClick = {
            expanded = !expanded
            if (unread) onMarkRead()
        },
        containerColor = if (unread) colors.surfaceContainerHigh else colors.surfaceContainer,
        modifier = modifier.animateContentSize(WseiMotion.size())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            WseiIconBadge(
                icon = if (announcement.hasAttachment) WseiIcons.AttachFile else WseiIcons.Campaign,
                shape = if (unread) M3XCookieShape else CircleShape,
                containerColor = if (unread) colors.primary else colors.surfaceContainerHighest,
                contentColor = if (unread) colors.onPrimary else colors.onSurfaceVariant
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = "${announcement.authorDepartment} • ${announcement.publishedAtIso}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) colors.primary else colors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (unread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                )
            }
        }

        Text(
            text = if (expanded) announcement.fullText else announcement.bodySnippet,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        AnimatedVisibility(
            visible = expanded && announcement.attachments.isNotEmpty(),
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.effects())
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
                announcement.attachments.forEach { attachment ->
                    FilledTonalButton(
                        onClick = { onDownloadAttachment(attachment) },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = WseiIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = WseiSpacing.s)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Ustawienia
// -----------------------------------------------------------------------------------------

@Composable
private fun SettingsScreen(
    uiState: ScheduleUiState,
    onBack: (() -> Unit)?,
    embedded: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    MorePage(title = "Ustawienia", onBack = onBack, embedded = embedded) { itemModifier ->
        item(key = "section_theme", contentType = "section") {
            WseiSectionTitle("Wygląd", modifier = itemModifier)
        }
        item(key = "theme_options", contentType = "theme_selector") {
            ThemeModeSelector(
                selected = uiState.themeMode,
                onSelect = onSelectThemeMode,
                modifier = itemModifier
            )
        }

        item(key = "section_behaviour", contentType = "section") {
            WseiSectionTitle("Działanie", modifier = itemModifier)
        }
        item(key = "switches", contentType = "switch_group") {
            Column(
                modifier = itemModifier,
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SettingsSwitchItem(
                    index = 0,
                    count = 2,
                    title = "Zajęcia na ekranie blokady",
                    supporting = "Postęp trwających zajęć w pasku stanu i na ekranie blokady",
                    icon = WseiIcons.NotificationsActive,
                    checked = uiState.isLiveNotificationEnabled,
                    onCheckedChange = onToggleLiveNotification
                )
                SettingsSwitchItem(
                    index = 1,
                    count = 2,
                    title = "Ukryj dane",
                    supporting = "Maskuje numer albumu, numer konta, kwoty i oceny w miejscach publicznych",
                    icon = if (uiState.isHideSensitiveData) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                    checked = uiState.isHideSensitiveData,
                    onCheckedChange = onToggleHideSensitiveData
                )
            }
        }
    }
}

/** Wiersz ustawienia z przełącznikiem: cały wiersz przełącza (M3 Expressive segmented list). */
@Composable
private fun SettingsSwitchItem(
    index: Int,
    count: Int,
    title: String,
    supporting: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = colors.surfaceContainer,
            selectedContainerColor = colors.surfaceContainer,
            selectedContentColor = colors.onSurface,
            selectedSupportingContentColor = colors.onSurfaceVariant
        ),
        leadingContent = {
            WseiIconBadge(
                icon = icon,
                containerColor = if (checked) colors.primary else colors.secondaryContainer,
                contentColor = if (checked) colors.onPrimary else colors.onSecondaryContainer,
                size = 40.dp,
                iconSize = 20.dp
            )
        },
        supportingContent = { Text(supporting) },
        // The whole row toggles; the switch only mirrors the state.
        trailingContent = { WseiSwitch(checked = checked, onCheckedChange = null) }
    ) {
        Text(title)
    }
}

/** Kolejność wyboru motywu w grupie przycisków. */
private val ThemeModeOrder = listOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.SYSTEM, AppThemeMode.DYNAMIC)

private val AppThemeMode.shortLabel: String
    get() = when (this) {
        AppThemeMode.LIGHT -> "Jasny"
        AppThemeMode.DARK -> "Ciemny"
        AppThemeMode.SYSTEM -> "System"
        AppThemeMode.DYNAMIC -> "Dynamiczny"
    }

private val AppThemeMode.description: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "Jasny lub ciemny, tak jak w ustawieniach telefonu"
        AppThemeMode.LIGHT -> "Zawsze jasne kolory"
        AppThemeMode.DARK -> "Zawsze ciemne kolory"
        AppThemeMode.DYNAMIC -> "Kolory dopasowane do tapety (Material You)"
    }

private fun themeSymbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black)
    ).build()

private val LightModeIcon: ImageVector by lazy {
    themeSymbol(
        "LightMode",
        "M12,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5s5,-2.24 5,-5S14.76,7 12,7z" +
            "M2,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H2c-0.55,0 -1,0.45 -1,1S1.45,13 2,13z" +
            "M20,13h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1S19.45,13 20,13z" +
            "M11,2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V2c0,-0.55 -0.45,-1 -1,-1S11,1.45 11,2z" +
            "M11,20v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1S11,19.45 11,20z" +
            "M5.99,4.58c-0.39,-0.39 -1.03,-0.39 -1.41,0c-0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0s0.39,-1.03 0,-1.41L5.99,4.58z" +
            "M18.36,16.95c-0.39,-0.39 -1.03,-0.39 -1.41,0c-0.39,0.39 -0.39,1.03 0,1.41l1.06,1.06c0.39,0.39 1.03,0.39 1.41,0c0.39,-0.39 0.39,-1.03 0,-1.41L18.36,16.95z" +
            "M19.42,5.99c0.39,-0.39 0.39,-1.03 0,-1.41c-0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0L19.42,5.99z" +
            "M7.05,18.36c0.39,-0.39 0.39,-1.03 0,-1.41c-0.39,-0.39 -1.03,-0.39 -1.41,0l-1.06,1.06c-0.39,0.39 -0.39,1.03 0,1.41s1.03,0.39 1.41,0L7.05,18.36z"
    )
}

private val DarkModeIcon: ImageVector by lazy {
    themeSymbol(
        "DarkMode",
        "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9s9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36" +
            "c-0.98,1.37 -2.58,2.26 -4.4,2.26c-2.98,0 -5.4,-2.42 -5.4,-5.4c0,-1.81 0.89,-3.42 2.26,-4.4" +
            "C12.92,3.04 12.46,3 12,3z"
    )
}

private val SystemModeIcon: ImageVector by lazy {
    themeSymbol(
        "SystemMode",
        "M12,22c5.52,0 10,-4.48 10,-10S17.52,2 12,2S2,6.48 2,12S6.48,22 12,22z" +
            "M13,4.07c3.94,0.49 7,3.85 7,7.93s-3.05,7.44 -7,7.93V4.07z"
    )
}

private val AppThemeMode.icon: ImageVector
    get() = when (this) {
        AppThemeMode.LIGHT -> LightModeIcon
        AppThemeMode.DARK -> DarkModeIcon
        AppThemeMode.SYSTEM -> SystemModeIcon
        AppThemeMode.DYNAMIC -> WseiIcons.Palette
    }

/**
 * Wybór motywu jako połączona grupa przycisków (M3 Expressive connected button group).
 * Wybrany przycisk morfuje do pigułki, a pod grupą pojawia się opis bieżącego trybu.
 */
@Composable
private fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val lastIndex = ThemeModeOrder.lastIndex
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        ButtonGroup(
            overflowIndicator = { menuState -> ButtonGroupDefaults.OverflowIndicator(menuState = menuState) },
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            // Explicit scope: the enclosing Column also offers a `Modifier.weight`.
            val groupScope = this
            ThemeModeOrder.forEachIndexed { index, mode ->
                val enabled = mode != AppThemeMode.DYNAMIC || dynamicSupported
                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }
                        ToggleButton(
                            checked = selected == mode,
                            onCheckedChange = { onSelect(mode) },
                            enabled = enabled,
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            contentPadding = PaddingValues(
                                horizontal = WseiSpacing.xs,
                                vertical = WseiSpacing.m
                            ),
                            interactionSource = interactionSource,
                            modifier = with(groupScope) {
                                Modifier
                                    .weight(1f)
                                    .animateWidth(interactionSource)
                            }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                val labelStyle = MaterialTheme.typography.labelLarge
                                Text(
                                    text = mode.shortLabel,
                                    style = labelStyle,
                                    maxLines = 1,
                                    autoSize = TextAutoSize.StepBased(
                                        minFontSize = 10.sp,
                                        maxFontSize = labelStyle.fontSize,
                                        stepSize = 0.5.sp
                                    )
                                )
                            }
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(mode.shortLabel) },
                            onClick = {
                                onSelect(mode)
                                menuState.dismiss()
                            },
                            leadingIcon = { Icon(imageVector = mode.icon, contentDescription = null) },
                            enabled = enabled
                        )
                    }
                )
            }
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(WseiMotion.effects()) togetherWith fadeOut(WseiMotion.effects()) },
            label = "themeModeDescription"
        ) { mode ->
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WseiSpacing.xs)
            )
        }
        if (!dynamicSupported) {
            Text(
                text = "Kolory z tapety wymagają Androida 12 lub nowszego.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = WseiSpacing.xs)
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// O aplikacji
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
    // Re-checked on every resume: the user may have just granted "install unknown apps".
    var needsInstallPermission by remember { mutableStateOf(viewModel.needsInstallPermission()) }
    LifecycleResumeEffect(viewModel) {
        needsInstallPermission = viewModel.needsInstallPermission()
        onPauseOrDispose { }
    }
    val highlighted = state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.READY

    val statusText = when (state.phase) {
        UpdatePhase.IDLE -> "Zainstalowana wersja: ${viewModel.currentVersion}"
        UpdatePhase.CHECKING -> "Sprawdzam najnowsze wydanie na GitHubie…"
        UpdatePhase.UP_TO_DATE -> "Masz najnowszą wersję (${viewModel.currentVersion})."
        UpdatePhase.AVAILABLE -> "Dostępna wersja ${release?.version} (masz ${viewModel.currentVersion})."
        UpdatePhase.DOWNLOADING -> "Pobieranie wersji ${release?.version}… ${(state.progress * 100).toInt()}%"
        UpdatePhase.READY -> "Wersja ${release?.version} jest pobrana i gotowa do instalacji."
        UpdatePhase.ERROR -> state.message ?: "Nie udało się sprawdzić aktualizacji."
    }

    WseiCard(
        modifier = modifier.animateContentSize(WseiMotion.size()),
        containerColor = colors.surfaceContainer,
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
                    shape = CircleShape,
                    containerColor = if (highlighted) colors.primary else colors.secondaryContainer,
                    contentColor = if (highlighted) colors.onPrimary else colors.onSecondaryContainer,
                    size = 44.dp,
                    iconSize = 22.dp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aktualizacje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.phase == UpdatePhase.ERROR) colors.error else colors.onSurfaceVariant
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
                text = "Co nowego",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary
            )
            Text(
                text = notes.replace(Regex("[#*`]+"), "").trim(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
            when (state.phase) {
                UpdatePhase.AVAILABLE -> Button(onClick = viewModel::download) { Text("Pobierz") }
                UpdatePhase.DOWNLOADING -> OutlinedButton(onClick = viewModel::cancelDownload) { Text("Anuluj") }
                UpdatePhase.READY -> Button(onClick = {
                    viewModel.installIntent()?.let { runCatching { context.startActivity(it) } }
                }) {
                    Text(if (needsInstallPermission) "Zezwól i zainstaluj" else "Zainstaluj")
                }
                UpdatePhase.CHECKING -> Unit
                else -> FilledTonalButton(onClick = viewModel::checkForUpdate) { Text("Sprawdź aktualizacje") }
            }
            if (highlighted) {
                OutlinedButton(onClick = viewModel::checkForUpdate) { Text("Sprawdź ponownie") }
            }
        }

        if (state.phase == UpdatePhase.READY && needsInstallPermission) {
            Text(
                text = "Android poprosi o zgodę na instalowanie aplikacji z Taala. Po jej nadaniu wróć i dotknij „Zainstaluj”.",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutScreen(onBack: (() -> Unit)?, embedded: Boolean) {
    val appVersion = rememberAppVersion()
    MorePage(title = "O aplikacji", onBack = onBack, embedded = embedded) { itemModifier ->
        item(key = "about_hero", contentType = "hero") {
            WseiCard(
                modifier = itemModifier,
                shape = WseiHeroShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                verticalSpacing = WseiSpacing.l
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
                ) {
                    WseiIconBadge(
                        icon = WseiIcons.TaalVolcano,
                        shape = M3XCookieShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        size = 72.dp,
                        iconSize = 36.dp
                    )
                    Column {
                        Text(text = "Taal", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Wersja $appVersion",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Text(
                    text = "Aplikacja dla studentów Uniwersytetu WSEI w Lublinie. Pokazuje plan zajęć, oceny i opłaty z Wirtualnego Dziekanatu.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item(key = "about_update", contentType = "update") { UpdateCard(modifier = itemModifier) }
        item(key = "about_credits", contentType = "credits") {
            WseiInset(
                modifier = itemModifier,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                WseiInfoRow("Dane", "Wirtualny Dziekanat (APR System)")
                WseiInfoRow("Czcionka", "Google Sans Flex (OFL)")
                WseiInfoRow("Wygląd", "Material 3 Expressive")
                WseiInfoRow("Licencja", "MIT")
            }
        }
        item(key = "about_disclaimer", contentType = "disclaimer") {
            Text(
                text = "Niezależny projekt studencki, niepowiązany oficjalnie z Uniwersytetem WSEI ani z APR System.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = itemModifier.padding(horizontal = WseiSpacing.xs)
            )
        }
    }
}
