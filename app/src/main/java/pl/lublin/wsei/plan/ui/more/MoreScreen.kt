@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package pl.lublin.wsei.plan.ui.more

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonSize
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.graphics.shapes.Morph
import pl.lublin.wsei.plan.BuildConfig
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.update.UpdateViewModel
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiBottomClearance
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiChevron
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiListEntry
import pl.lublin.wsei.plan.ui.components.WseiListGroup
import pl.lublin.wsei.plan.ui.components.WseiListItem
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
import pl.lublin.wsei.plan.ui.components.WseiSwitch
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.navigation.MoreSubView
import pl.lublin.wsei.plan.ui.navigation.WseiPredictiveBackContainer
import pl.lublin.wsei.plan.ui.navigation.rememberWseiPredictiveBackState
import pl.lublin.wsei.plan.ui.schedule.AppThemeMode
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.isWseiDarkTheme
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import pl.lublin.wsei.plan.ui.theme.wseiPaletteSwatch
import androidx.compose.ui.res.pluralStringResource

private const val LINK_VIRTUAL_DEAN = "https://dziekanat.wsei.lublin.pl/"
private const val LINK_ELEARNING = "https://dl.wsei.pl/"

/** Minimum width of a hub tile before the grid drops a column. */
private val HubTileMinWidth = 220.dp

/** Opens [url] in the browser; silently ignored when no browser is installed. */
internal fun openExternalLink(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Arrow marking an entry that opens an external page in the browser. */
@Composable
internal fun WseiExternalLinkMark() {
    Icon(
        imageVector = WseiIcons.ArrowForward,
        contentDescription = stringResource(R.string.settings_opens_in_browser),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

/**
 * "More" tab: the hub plus the Announcements, Settings and About sub-pages.
 *
 * - phone: the hub and sub-pages replace each other ([currentSubView] comes from the shell),
 * - medium window: the hub as a tile grid,
 * - wide window / unfolded foldable: the hub on the left, the selected sub-page on the right.
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
    onRefresh: () -> Unit,
    onSelectColorPalette: (AppColorPalette) -> Unit = {},
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val windowLayout = LocalWseiWindowLayout.current
    val updateStatus by updateViewModel.updateStatus.collectAsStateWithLifecycle()
    val updateAvailable = updateStatus.isUpdateAvailable
    val predictiveBackState = rememberWseiPredictiveBackState()
    val subPage: @Composable (MoreSubView, embedded: Boolean) -> Unit = { subView, embedded ->
        val onBack: (() -> Unit)? = if (embedded) null else ({ onNavigateSubView(MoreSubView.HUB) })
        when (subView) {
            MoreSubView.HUB -> if (embedded) {
                MoreDetailPlaceholder(
                    unreadCount = uiState.unreadAnnouncementsCount,
                    onOpenAnnouncements = { onNavigateSubView(MoreSubView.ANNOUNCEMENTS) }
                )
            } else {
                MoreHub(uiState = uiState, onOpen = onNavigateSubView, updateAvailable = updateAvailable)
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
                onSelectColorPalette = onSelectColorPalette,
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
                    forceList = true,
                    updateAvailable = updateAvailable
                )
            },
            second = {
                WseiPredictiveBackContainer(
                    enabled = currentSubView != MoreSubView.HUB,
                    state = predictiveBackState,
                    onBack = { onNavigateSubView(MoreSubView.HUB) },
                    peekContent = { subPage(MoreSubView.HUB, true) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = currentSubView,
                        transitionSpec = {
                            if (predictiveBackState.consumeCommittedSwipe()) {
                                EnterTransition.None togetherWith ExitTransition.None using null
                            } else {
                                (fadeIn(WseiMotion.effects()) +
                                    slideInHorizontally(WseiMotion.spatial()) { width -> width / 12 }) togetherWith
                                    fadeOut(WseiMotion.fastEffects()) using null
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "moreDetailTransition"
                    ) { subView -> subPage(subView, true) }
                }
            }
        )
    } else {
        WseiPredictiveBackContainer(
            enabled = currentSubView != MoreSubView.HUB,
            state = predictiveBackState,
            onBack = { onNavigateSubView(MoreSubView.HUB) },
            peekContent = { subPage(MoreSubView.HUB, false) },
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentSubView,
                transitionSpec = {
                    if (predictiveBackState.consumeCommittedSwipe()) {
                        EnterTransition.None togetherWith ExitTransition.None using null
                    } else {
                        // Forward when leaving the hub, backward when returning to it.
                        val forward = targetState != MoreSubView.HUB
                        (fadeIn(WseiMotion.effects()) +
                            slideInHorizontally(WseiMotion.spatial()) { width -> if (forward) width / 8 else -width / 8 }) togetherWith
                            fadeOut(WseiMotion.fastEffects()) using null
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "moreSubViewTransition"
            ) { subView -> subPage(subView, false) }
        }
    }
}

/**
 * Shared frame of all "More" sub-pages (Announcements, Settings, About), so their top app bars
 * are identical: same unscrolled and scrolled container colours, no extra background behind the
 * list. Embedded in the right pane ([embedded]) it has no back button and no second profile
 * shortcut (the hub pane already shows one).
 */
@Composable
internal fun MorePage(
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
    val unreadCount: Int = 0,
    /** Short highlighted label, e.g. "Nowa wersja" on "O aplikacji". */
    val badge: String? = null
)

/** Badge with the number of unread announcements („1 nowe”, „5 nowych”). */
@Composable
private fun newCountLabel(count: Int): String = pluralStringResource(R.plurals.more_new_count, count, count)

@Composable
private fun MoreHub(
    uiState: ScheduleUiState,
    onOpen: (MoreSubView) -> Unit,
    selected: MoreSubView? = null,
    forceList: Boolean = false,
    updateAvailable: Boolean = false
) {
    val unread = uiState.unreadAnnouncementsCount
    val context = LocalContext.current
    val windowLayout = LocalWseiWindowLayout.current
    val openLink: (String) -> Unit = { url -> openExternalLink(context, url) }
    val sections = listOf(
        HubEntry(
            key = "announcements",
            title = stringResource(R.string.announcements_title),
            supporting = stringResource(R.string.more_announcements_supporting),
            icon = WseiIcons.Campaign,
            onClick = { onOpen(MoreSubView.ANNOUNCEMENTS) },
            subView = MoreSubView.ANNOUNCEMENTS,
            unreadCount = unread
        ),
        HubEntry(
            key = "settings",
            title = stringResource(R.string.settings_title),
            supporting = stringResource(R.string.settings_hub_supporting),
            icon = WseiIcons.Settings,
            onClick = { onOpen(MoreSubView.SETTINGS) },
            subView = MoreSubView.SETTINGS
        ),
        HubEntry(
            key = "about",
            title = stringResource(R.string.about_title),
            supporting = stringResource(R.string.about_hub_supporting, BuildConfig.VERSION_NAME),
            icon = WseiIcons.Info,
            onClick = { onOpen(MoreSubView.ABOUT) },
            subView = MoreSubView.ABOUT,
            badge = if (updateAvailable) stringResource(R.string.about_hub_update_badge) else null
        )
    )
    val links = listOf(
        HubEntry(
            key = "link_dean",
            title = stringResource(R.string.more_link_virtual_dean),
            supporting = "dziekanat.wsei.lublin.pl",
            icon = WseiIcons.Badge,
            onClick = { openLink(LINK_VIRTUAL_DEAN) }
        ),
        HubEntry(
            key = "link_elearning",
            title = stringResource(R.string.more_link_elearning),
            supporting = "dl.wsei.pl",
            icon = WseiIcons.School,
            onClick = { openLink(LINK_ELEARNING) }
        )
    )
    // Tiles on medium windows; a compact list on phones and in the narrow hub pane.
    val useTiles = !forceList && !windowLayout.isCompact

    WseiScreenFrame(title = stringResource(R.string.nav_more)) { scrollModifier ->
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
                WseiSectionTitle(stringResource(R.string.more_useful_links), modifier = itemModifier)
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
            text = newCountLabel(entry.unreadCount),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            emphasized = true
        )
    }
    if (entry.badge != null) {
        WseiPill(
            text = entry.badge,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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

/** Right pane on wide windows before a section is picked. */
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
                text = stringResource(R.string.more_pick_section_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.more_pick_section_message),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            FilledTonalButton(onClick = onOpenAnnouncements, shapes = ButtonDefaults.shapes()) {
                Icon(
                    imageVector = WseiIcons.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (unreadCount > 0) {
                        stringResource(R.string.more_announcements_with_count, newCountLabel(unreadCount))
                    } else {
                        stringResource(R.string.more_show_announcements)
                    },
                    modifier = Modifier.padding(start = WseiSpacing.s)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Settings
// -----------------------------------------------------------------------------------------

@Composable
private fun SettingsScreen(
    uiState: ScheduleUiState,
    onBack: (() -> Unit)?,
    embedded: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectColorPalette: (AppColorPalette) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    MorePage(title = stringResource(R.string.settings_title), onBack = onBack, embedded = embedded) { itemModifier ->
        item(key = "section_theme", contentType = "section") {
            WseiSectionTitle(stringResource(R.string.settings_section_theme), modifier = itemModifier)
        }
        item(key = "theme_options", contentType = "theme_selector") {
            ThemeModeSelector(
                selected = uiState.themeMode,
                onSelect = onSelectThemeMode,
                modifier = itemModifier
            )
        }

        item(key = "section_palette", contentType = "section") {
            WseiSectionTitle(stringResource(R.string.settings_section_palette), modifier = itemModifier)
        }
        item(key = "palette_options", contentType = "palette_selector") {
            ColorPaletteSelector(
                selected = uiState.colorPalette,
                isDark = isWseiDarkTheme(uiState.themeMode),
                onSelect = onSelectColorPalette,
                modifier = itemModifier
            )
        }

        item(key = "section_language", contentType = "section") {
            WseiSectionTitle(stringResource(R.string.settings_section_language), modifier = itemModifier)
        }
        item(key = "language_options", contentType = "language_selector") {
            AppLanguageSelector(modifier = itemModifier)
        }

        item(key = "section_behaviour", contentType = "section") {
            WseiSectionTitle(stringResource(R.string.settings_section_behaviour), modifier = itemModifier)
        }
        item(key = "switches", contentType = "switch_group") {
            Column(
                modifier = itemModifier,
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SettingsSwitchItem(
                    index = 0,
                    count = 2,
                    title = stringResource(R.string.settings_live_notification_title),
                    supporting = stringResource(R.string.settings_live_notification_supporting),
                    icon = WseiIcons.NotificationsActive,
                    checked = uiState.isLiveNotificationEnabled,
                    onCheckedChange = onToggleLiveNotification
                )
                SettingsSwitchItem(
                    index = 1,
                    count = 2,
                    title = stringResource(R.string.settings_hide_data_title),
                    supporting = stringResource(R.string.settings_hide_data_supporting),
                    icon = if (uiState.isHideSensitiveData) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                    checked = uiState.isHideSensitiveData,
                    onCheckedChange = onToggleHideSensitiveData
                )
            }
        }
    }
}

/** Settings row with a switch: the whole row toggles (M3 Expressive segmented list). */
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

// --- Theme mode ---------------------------------------------------------------------------

/** Order of the theme modes in the button group. */
private val ThemeModeOrder = listOf(AppThemeMode.LIGHT, AppThemeMode.DARK, AppThemeMode.SYSTEM)

@get:StringRes
private val AppThemeMode.shortLabelRes: Int
    get() = when (this) {
        AppThemeMode.LIGHT -> R.string.settings_theme_light
        AppThemeMode.DARK -> R.string.settings_theme_dark
        AppThemeMode.SYSTEM -> R.string.settings_theme_system
    }

@get:StringRes
private val AppThemeMode.descriptionRes: Int
    get() = when (this) {
        AppThemeMode.SYSTEM -> R.string.settings_theme_system_description
        AppThemeMode.LIGHT -> R.string.settings_theme_light_description
        AppThemeMode.DARK -> R.string.settings_theme_dark_description
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

/** Material Symbols „check” — the selected palette swatch mark. */
private val CheckIcon: ImageVector by lazy {
    themeSymbol("Check", "M9,16.17L4.83,12l-1.42,1.41L9,19L21,7l-1.41,-1.41z")
}

private val AppThemeMode.icon: ImageVector
    get() = when (this) {
        AppThemeMode.LIGHT -> LightModeIcon
        AppThemeMode.DARK -> DarkModeIcon
        AppThemeMode.SYSTEM -> SystemModeIcon
    }

/**
 * Theme mode (light / dark / system) as an M3 Expressive connected button group: three
 * equal-width segments with the icon and label on one centred line. The checked segment morphs
 * into a pill and the mode description is shown below. Colours are chosen separately
 * ([ColorPaletteSelector]).
 */
@Composable
private fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
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
                customItem(
                    buttonGroupContent = {
                        val interactionSource = remember { MutableInteractionSource() }
                        val label = stringResource(mode.shortLabelRes)
                        ToggleButton(
                            checked = selected == mode,
                            onCheckedChange = { onSelect(mode) },
                            buttonSize = ToggleButtonSize.Medium,
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            // Same padding on every segment, so icon + label sit centred in equal widths.
                            contentPadding = PaddingValues(horizontal = WseiSpacing.m),
                            interactionSource = interactionSource,
                            icon = { Icon(imageVector = mode.icon, contentDescription = null) },
                            modifier = with(groupScope) {
                                Modifier
                                    .weight(1f)
                                    .animateWidth(interactionSource)
                            }
                        ) {
                            val labelStyle = MaterialTheme.typography.labelLarge
                            // Long translations shrink instead of wrapping or pushing the icon out.
                            Text(
                                text = label,
                                style = labelStyle,
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 11.sp,
                                    maxFontSize = labelStyle.fontSize,
                                    stepSize = 0.5.sp
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    },
                    menuContent = { menuState ->
                        DropdownMenuItem(
                            text = { Text(stringResource(mode.shortLabelRes)) },
                            onClick = {
                                onSelect(mode)
                                menuState.dismiss()
                            },
                            leadingIcon = { Icon(imageVector = mode.icon, contentDescription = null) }
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
                text = stringResource(mode.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WseiSpacing.xs)
            )
        }
    }
}

// --- Colour palette -----------------------------------------------------------------------

/** Display order of the palettes in the swatch grid. */
private val PaletteOrder = listOf(
    AppColorPalette.TAAL,
    AppColorPalette.DYNAMIC,
    AppColorPalette.COBALT,
    AppColorPalette.TERRACOTTA,
    AppColorPalette.PLUM,
    AppColorPalette.OCEAN
)

@get:StringRes
private val AppColorPalette.labelRes: Int
    get() = when (this) {
        AppColorPalette.TAAL -> R.string.settings_palette_taal
        AppColorPalette.DYNAMIC -> R.string.settings_palette_dynamic
        AppColorPalette.COBALT -> R.string.settings_palette_cobalt
        AppColorPalette.TERRACOTTA -> R.string.settings_palette_terracotta
        AppColorPalette.PLUM -> R.string.settings_palette_plum
        AppColorPalette.OCEAN -> R.string.settings_palette_ocean
    }

/** Minimum width of one swatch column before the grid drops a column. */
private val PaletteChipMinWidth = 96.dp

/** Diameter of a palette swatch. */
private val PaletteSwatchSize = 60.dp

/**
 * A [Shape] that renders [morph] at [progress] (0 = start polygon, 1 = end polygon), scaled to
 * the layout size and centred — the same mapping as `RoundedPolygon.toShape()`.
 */
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress = progress)
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}

/**
 * Colour palette picker: a grid of round swatches (primary, secondary and tertiary colour).
 * Works in every theme mode. The dynamic palette (Material You) needs Android 12+.
 */
@Composable
private fun ColorPaletteSelector(
    selected: AppColorPalette,
    isDark: Boolean,
    onSelect: (AppColorPalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = MaterialTheme.colorScheme
    // Swatches are resolved once per palette for the current light/dark appearance.
    val swatches = PaletteOrder.associateWith { palette -> wseiPaletteSwatch(palette, isDark) }
    WseiCard(
        modifier = modifier,
        containerColor = colors.surfaceContainer,
        contentPadding = PaddingValues(horizontal = WseiSpacing.s, vertical = WseiSpacing.l),
        verticalSpacing = WseiSpacing.m
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = (maxWidth / PaletteChipMinWidth).toInt().coerceIn(3, PaletteOrder.size)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                PaletteOrder.chunked(columns).forEach { rowPalettes ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowPalettes.forEach { palette ->
                            PaletteSwatchChip(
                                label = stringResource(palette.labelRes),
                                sublabel = if (palette == AppColorPalette.DYNAMIC) {
                                    stringResource(R.string.settings_palette_dynamic_hint)
                                } else {
                                    null
                                },
                                swatch = swatches.getValue(palette),
                                selected = palette == selected,
                                enabled = palette != AppColorPalette.DYNAMIC || dynamicSupported,
                                onClick = { onSelect(palette) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowPalettes.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
        val note = when {
            !dynamicSupported -> stringResource(R.string.settings_palette_dynamic_unsupported)
            selected == AppColorPalette.DYNAMIC -> stringResource(R.string.settings_palette_dynamic_description)
            else -> stringResource(R.string.settings_palette_description)
        }
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WseiSpacing.m)
        )
    }
}

/**
 * One palette swatch. When selected the circle morphs into a cookie (MaterialShapes) on a spring,
 * grows and shows a check mark; pressing squeezes it slightly.
 */
@Composable
private fun PaletteSwatchChip(
    label: String,
    sublabel: String?,
    swatch: List<Color>,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val morphProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = WseiMotion.size(),
        label = "paletteMorph"
    )
    val selectionScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.86f,
        animationSpec = WseiMotion.spatial(),
        label = "paletteScale"
    )
    val primary = swatch.getOrElse(0) { colors.primary }
    val secondary = swatch.getOrElse(1) { colors.secondary }
    val tertiary = swatch.getOrElse(2) { colors.tertiary }

    Column(
        modifier = modifier
            .clip(WseiInsetShape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = WseiSpacing.s, horizontal = WseiSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(PaletteSwatchSize)
                .wseiPressScale(interactionSource, pressedScale = 0.9f)
                .graphicsLayer {
                    // Read in the layer phase: the morph and scale animate without recomposition.
                    scaleX = selectionScale
                    scaleY = selectionScale
                    shape = MorphShape(morph, morphProgress)
                    clip = true
                },
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(primary)
                )
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(secondary)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .background(tertiary)
                    )
                }
            }
            // Fully qualified: inside Box-in-Column the ColumnScope overload would be picked.
            androidx.compose.animation.AnimatedVisibility(
                visible = selected,
                enter = scaleIn(WseiMotion.fastSpatial()) + fadeIn(WseiMotion.effects()),
                exit = scaleOut(WseiMotion.fastSpatial()) + fadeOut(WseiMotion.effects())
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CheckIcon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.primary else colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sublabel != null) {
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- App language -------------------------------------------------------------------------

/** Per-app language options; [tag] null means "follow the system language". */
private enum class AppLanguage(val tag: String?, @StringRes val labelRes: Int, val code: String?) {
    SYSTEM(null, R.string.settings_language_system, null),
    POLISH("pl", R.string.settings_language_polish, "PL"),
    ENGLISH("en", R.string.settings_language_english, "EN"),
    UKRAINIAN("uk", R.string.settings_language_ukrainian, "UK")
}

/** Language currently set through AppCompat's per-app locales (empty list = system). */
private fun currentAppLanguage(): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty()) return AppLanguage.SYSTEM
    val language = locales.get(0)?.language
    return AppLanguage.entries.firstOrNull { it.tag == language } ?: AppLanguage.SYSTEM
}

/**
 * App language (per-app language, AppCompat). A change applies the new resources right away —
 * AppCompat recreates the activity; on Android 13+ the choice also shows in the system settings.
 */
@Composable
private fun AppLanguageSelector(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf(currentAppLanguage()) }
    val options = AppLanguage.entries
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        options.forEachIndexed { index, language ->
            val isSelected = language == selected
            val supporting: (@Composable () -> Unit)? = if (language == AppLanguage.SYSTEM) {
                { Text(stringResource(R.string.settings_language_system_supporting)) }
            } else {
                null
            }
            val select = {
                if (selected != language) {
                    selected = language
                    val locales = language.tag
                        ?.let { LocaleListCompat.forLanguageTags(it) }
                        ?: LocaleListCompat.getEmptyLocaleList()
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            }
            SegmentedListItem(
                selected = isSelected,
                onClick = select,
                shapes = ListItemDefaults.segmentedShapes(index = index, count = options.size),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = colors.surfaceContainer,
                    selectedContainerColor = colors.secondaryContainer,
                    selectedContentColor = colors.onSecondaryContainer,
                    selectedSupportingContentColor = colors.onSecondaryContainer
                ),
                leadingContent = { LanguageBadge(language = language, selected = isSelected) },
                supportingContent = supporting,
                // The whole row selects; the radio button only mirrors the state.
                trailingContent = { RadioButton(selected = isSelected, onClick = null) }
            ) {
                Text(stringResource(language.labelRes))
            }
        }
    }
}

@Composable
private fun LanguageBadge(language: AppLanguage, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val code = language.code
    if (code == null) {
        WseiIconBadge(
            icon = WseiIcons.Settings,
            containerColor = if (selected) colors.primary else colors.secondaryContainer,
            contentColor = if (selected) colors.onPrimary else colors.onSecondaryContainer,
            size = 40.dp,
            iconSize = 20.dp
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(M3XSquircleShape)
                .background(if (selected) colors.primary else colors.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) colors.onPrimary else colors.onSecondaryContainer
            )
        }
    }
}
