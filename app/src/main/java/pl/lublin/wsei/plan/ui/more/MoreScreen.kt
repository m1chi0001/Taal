package pl.lublin.wsei.plan.ui.more

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
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
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.WseiSectionTitle
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

/** Zakładka „Więcej”: hub oraz podstrony Ogłoszenia, Ustawienia i O aplikacji. */
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
    val backToHub = { onNavigateSubView(MoreSubView.HUB) }
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
    ) { subView ->
        when (subView) {
            MoreSubView.HUB -> MoreHub(uiState = uiState, onOpen = onNavigateSubView)
            MoreSubView.ANNOUNCEMENTS -> AnnouncementsScreen(
                uiState = uiState,
                onBack = backToHub,
                onMarkRead = onMarkAnnouncementRead,
                onDownloadAttachment = onDownloadAttachment,
                onRefresh = onRefresh
            )
            MoreSubView.SETTINGS -> SettingsScreen(
                uiState = uiState,
                onBack = backToHub,
                onSelectThemeMode = onSelectThemeMode,
                onToggleLiveNotification = onToggleLiveNotification,
                onToggleHideSensitiveData = onToggleHideSensitiveData
            )
            MoreSubView.ABOUT -> AboutScreen(onBack = backToHub)
        }
    }
}

@Composable
private fun MoreHub(uiState: ScheduleUiState, onOpen: (MoreSubView) -> Unit) {
    val unread = uiState.unreadAnnouncementsCount
    val appVersion = rememberAppVersion()
    WseiScreenScaffold(title = "Więcej") {
        item(key = "hub_list") {
            WseiListGroup(
                entries = listOf(
                    WseiListEntry("announcements") { shape ->
                        WseiListItem(
                            title = "Ogłoszenia",
                            supporting = "Wiadomości z dziekanatu i załączniki",
                            icon = WseiIcons.Campaign,
                            iconContainerColor = if (unread > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                            iconContentColor = if (unread > 0) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = shape,
                            onClick = { onOpen(MoreSubView.ANNOUNCEMENTS) },
                            trailing = {
                                if (unread > 0) {
                                    WseiPill(
                                        text = "$unread nowe",
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                        emphasized = true
                                    )
                                }
                                WseiChevron()
                            }
                        )
                    },
                    WseiListEntry("settings") { shape ->
                        WseiListItem(
                            title = "Ustawienia",
                            supporting = "Motyw, powiadomienia i prywatność",
                            icon = WseiIcons.Settings,
                            shape = shape,
                            onClick = { onOpen(MoreSubView.SETTINGS) },
                            trailing = { WseiChevron() }
                        )
                    },
                    WseiListEntry("about") { shape ->
                        WseiListItem(
                            title = "O aplikacji",
                            supporting = "Wersja $appVersion • licencje",
                            icon = WseiIcons.Info,
                            shape = shape,
                            onClick = { onOpen(MoreSubView.ABOUT) },
                            trailing = { WseiChevron() }
                        )
                    }
                )
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Ogłoszenia
// -----------------------------------------------------------------------------------------

@Composable
private fun AnnouncementsScreen(
    uiState: ScheduleUiState,
    onBack: () -> Unit,
    onMarkRead: (DeanAnnouncement) -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit,
    onRefresh: () -> Unit
) {
    WseiScreenScaffold(
        title = "Ogłoszenia",
        onBack = onBack,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing
    ) {
        if (uiState.announcements.isEmpty()) {
            item(key = "announcements_empty") {
                WseiEmptyState(
                    icon = WseiIcons.Campaign,
                    title = "Brak ogłoszeń",
                    message = "Nowe wiadomości z dziekanatu pojawią się tutaj."
                )
            }
        }
        items(
            items = uiState.announcements,
            key = { it.id },
            contentType = { "announcement_card" }
        ) { announcement ->
            AnnouncementCard(
                announcement = announcement,
                onMarkRead = { onMarkRead(announcement) },
                onDownloadAttachment = onDownloadAttachment
            )
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: DeanAnnouncement,
    onMarkRead: () -> Unit,
    onDownloadAttachment: (AnnouncementAttachment) -> Unit
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
        modifier = Modifier.animateContentSize(WseiMotion.size())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Box {
                WseiIconBadge(
                    icon = if (announcement.hasAttachment) WseiIcons.AttachFile else WseiIcons.Campaign,
                    shape = if (unread) M3XCookieShape else CircleShape,
                    containerColor = if (unread) colors.primary else colors.surfaceContainerHighest,
                    contentColor = if (unread) colors.onPrimary else colors.onSurfaceVariant
                )
            }
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

        AnimatedVisibility(visible = expanded && announcement.attachments.isNotEmpty()) {
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
    onBack: () -> Unit,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onToggleLiveNotification: (Boolean) -> Unit,
    onToggleHideSensitiveData: (Boolean) -> Unit
) {
    WseiScreenScaffold(title = "Ustawienia", onBack = onBack) {
        item(key = "section_theme") { WseiSectionTitle("Wygląd") }
        item(key = "theme_options") {
            WseiListGroup(
                entries = AppThemeMode.entries.map { mode ->
                    WseiListEntry(mode.name) { shape ->
                        val selected = uiState.themeMode == mode
                        WseiListItem(
                            title = mode.polishLabel,
                            supporting = mode.description,
                            icon = mode.icon,
                            shape = shape,
                            selected = selected,
                            onClick = { onSelectThemeMode(mode) },
                            trailing = {
                                RadioButton(selected = selected, onClick = { onSelectThemeMode(mode) })
                            }
                        )
                    }
                }
            )
        }

        item(key = "section_behaviour") { WseiSectionTitle("Działanie") }
        item(key = "switches") {
            WseiListGroup(
                entries = listOf(
                    WseiListEntry("live") { shape ->
                        WseiListItem(
                            title = "Zajęcia na ekranie blokady",
                            supporting = "Postęp trwających zajęć w pasku stanu i na ekranie blokady",
                            icon = WseiIcons.NotificationsActive,
                            shape = shape,
                            onClick = { onToggleLiveNotification(!uiState.isLiveNotificationEnabled) },
                            trailing = {
                                Switch(
                                    checked = uiState.isLiveNotificationEnabled,
                                    onCheckedChange = onToggleLiveNotification
                                )
                            }
                        )
                    },
                    WseiListEntry("privacy") { shape ->
                        WseiListItem(
                            title = "Ukryj dane",
                            supporting = "Maskuje numer albumu, kwoty i oceny w miejscach publicznych",
                            icon = if (uiState.isHideSensitiveData) WseiIcons.VisibilityOff else WseiIcons.Visibility,
                            shape = shape,
                            onClick = { onToggleHideSensitiveData(!uiState.isHideSensitiveData) },
                            trailing = {
                                Switch(
                                    checked = uiState.isHideSensitiveData,
                                    onCheckedChange = onToggleHideSensitiveData
                                )
                            }
                        )
                    }
                )
            )
        }
    }
}

private val AppThemeMode.description: String
    get() = when (this) {
        AppThemeMode.SYSTEM -> "Jasny lub ciemny jak w systemie"
        AppThemeMode.LIGHT -> "Zawsze jasne kolory"
        AppThemeMode.DARK -> "Zawsze ciemne kolory"
        AppThemeMode.DYNAMIC -> "Kolory z tapety (Android 12+)"
    }

private val AppThemeMode.icon
    get() = when (this) {
        AppThemeMode.SYSTEM -> WseiIcons.Settings
        AppThemeMode.LIGHT -> WseiIcons.Visibility
        AppThemeMode.DARK -> WseiIcons.VisibilityOff
        AppThemeMode.DYNAMIC -> WseiIcons.Palette
    }

// -----------------------------------------------------------------------------------------
// O aplikacji
// -----------------------------------------------------------------------------------------

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val appVersion = rememberAppVersion()
    WseiScreenScaffold(title = "O aplikacji", onBack = onBack) {
        item(key = "about_hero") {
            WseiCard(
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
        item(key = "about_credits") {
            WseiInset(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                WseiInfoRow("Dane", "Wirtualny Dziekanat (APR System)")
                WseiInfoRow("Czcionka", "Google Sans Flex (OFL)")
                WseiInfoRow("Wygląd", "Material 3 Expressive")
                WseiInfoRow("Licencja", "MIT")
            }
        }
        item(key = "about_disclaimer") {
            Text(
                text = "Niezależny projekt studencki, niepowiązany oficjalnie z Uniwersytetem WSEI ani z APR System.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = WseiSpacing.xs)
            )
        }
    }
}
