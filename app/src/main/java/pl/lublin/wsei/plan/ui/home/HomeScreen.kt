package pl.lublin.wsei.plan.ui.home

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiWidthClass
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiCardHeader
import pl.lublin.wsei.plan.ui.components.WseiClassCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenScaffold
import pl.lublin.wsei.plan.ui.components.classRowStateOf
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.finances.formatPln
import pl.lublin.wsei.plan.ui.finances.isDueThisMonth
import pl.lublin.wsei.plan.ui.finances.outstandingAmountPln
import pl.lublin.wsei.plan.ui.finances.sortedForDisplay
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.theme.M3XCloverShape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSunnyShape
import pl.lublin.wsei.plan.ui.theme.WseiHeroShape
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.l10n.appLocale
import pl.lublin.wsei.plan.l10n.capitalizeFirst
import pl.lublin.wsei.plan.l10n.localizedNameOrPlaceholder
import pl.lublin.wsei.plan.l10n.rememberLocalizedDateFormatter

/** CLDR skeleton of the day headers ("piątek, 20 listopada" / "Friday, November 20"). */
private const val DayHeaderSkeleton = "EEEEdMMMM"

/**
 * Ekran Start: powitanie, plan na dziś, szybkie skróty i najbliższa opłata.
 * Wszystko, czego student potrzebuje rano, bez przechodzenia między zakładkami.
 */
@Composable
fun HomeScreen(
    uiState: ScheduleUiState,
    onOpenSchedule: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenFinances: () -> Unit,
    onOpenAnnouncements: () -> Unit,
    onRefresh: () -> Unit
) {
    val session = uiState.currentSession
    val firstName = session?.firstName?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.home_greeting_fallback_name)
    // Keyed on the current hour (from the per-minute ticker), so the greeting changes from
    // "Dzień dobry" to "Cześć" while the app stays open.
    val hour = remember(uiState.nowEpochMillis) {
        Instant.ofEpochMilli(uiState.nowEpochMillis).atZone(ZoneId.systemDefault()).hour
    }
    val greeting = stringResource(
        when (hour) {
            in 5..10 -> R.string.home_greeting_morning
            in 18..23, in 0..4 -> R.string.home_greeting_evening
            else -> R.string.home_greeting_day
        },
        firstName
    )

    val windowLayout = LocalWseiWindowLayout.current
    // The ticker in the ViewModel updates `nowEpochMillis` every minute, so "today" follows
    // midnight without restarting the screen.
    val todayIso = remember(uiState.nowEpochMillis) { localDateOf(uiState.nowEpochMillis).toString() }

    WseiScreenScaffold(
        title = greeting,
        onRefresh = onRefresh,
        isRefreshing = uiState.isRefreshing,
        contentPadding = wseiScreenContentPadding()
    ) {
        item(key = "home_intro") {
            HomeIntro(
                uiState = uiState,
                todayIso = todayIso,
                modifier = if (windowLayout.isCompact) Modifier else Modifier.wseiReadableWidth(HomeDashboardMaxWidth)
            )
        }
        if (windowLayout.isCompact) {
            item(key = "home_today") {
                TodayScheduleCard(uiState = uiState, todayIso = todayIso, onClick = onOpenSchedule)
            }
            item(key = "home_shortcuts") {
                QuickShortcutsGrid(
                    uiState = uiState,
                    onOpenSchedule = onOpenSchedule,
                    onOpenSubjects = onOpenSubjects,
                    onOpenFinances = onOpenFinances,
                    onOpenAnnouncements = onOpenAnnouncements
                )
            }
            item(key = "home_payment") {
                NextPaymentCard(uiState = uiState, onClick = onOpenFinances)
            }
        } else {
            // Tablets, unfolded foldables and landscape: the day plan on the left, the summary
            // tiles in a side column — instead of four cards stretched edge to edge.
            item(key = "home_dashboard") {
                Row(
                    modifier = Modifier.wseiReadableWidth(HomeDashboardMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                ) {
                    val primaryWeight = if (windowLayout.widthClass == WseiWidthClass.EXPANDED) 0.58f else 0.5f
                    TodayScheduleCard(
                        uiState = uiState,
                        todayIso = todayIso,
                        onClick = onOpenSchedule,
                        modifier = Modifier.weight(primaryWeight)
                    )
                    Column(
                        modifier = Modifier.weight(1f - primaryWeight),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                    ) {
                        QuickShortcutsGrid(
                            uiState = uiState,
                            onOpenSchedule = onOpenSchedule,
                            onOpenSubjects = onOpenSubjects,
                            onOpenFinances = onOpenFinances,
                            onOpenAnnouncements = onOpenAnnouncements
                        )
                        NextPaymentCard(uiState = uiState, onClick = onOpenFinances)
                    }
                }
            }
        }
    }
}

/** Width cap of the two-column dashboard so it stays readable on very wide windows. */
private val HomeDashboardMaxWidth = 1200.dp

/** Class dates (`dateIso`) are in Warsaw time, so "today" is resolved in the same zone. */
private fun localDateOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(WarsawZone).toLocalDate()

private val WarsawZone: ZoneId = ZoneId.of("Europe/Warsaw")

@Composable
private fun HomeIntro(
    uiState: ScheduleUiState,
    todayIso: String,
    modifier: Modifier = Modifier
) {
    val session = uiState.currentSession
    val locale = appLocale()
    val dayHeaderFormatter = rememberLocalizedDateFormatter(DayHeaderSkeleton)
    val today = remember(todayIso, dayHeaderFormatter) {
        LocalDate.parse(todayIso).format(dayHeaderFormatter).capitalizeFirst(locale)
    }
    Column(
        modifier = modifier.padding(horizontal = WseiSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        Text(
            text = today,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
            WseiPill(
                text = stringResource(R.string.subjects_semester_number, session?.semesterNumber ?: 1),
                icon = WseiIcons.School,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                emphasized = true
            )
            WseiPill(
                text = stringResource(R.string.home_album_number, uiState.maskSensitive(session?.albumNumber ?: "—")),
                icon = WseiIcons.Badge
            )
        }
    }
}

/**
 * Plan na dziś (albo najbliższy dzień z zajęciami). Te same karty zajęć co w Planie,
 * w wersji kompaktowej; trwające zajęcia mają pasek postępu.
 */
@Composable
private fun TodayScheduleCard(
    uiState: ScheduleUiState,
    todayIso: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayEvents: List<ClassEvent> = remember(uiState.allEvents, todayIso) {
        val upcomingDate = uiState.allEvents
            .asSequence()
            .map { it.dateIso }
            .filter { it >= todayIso }
            .minOrNull()
        if (upcomingDate == null) {
            emptyList()
        } else {
            uiState.allEvents
                .filter { it.dateIso == upcomingDate }
                .sortedBy { it.timeSlot.startEpochMillis }
        }
    }
    val now = uiState.nowEpochMillis
    val displayedDateIso = dayEvents.firstOrNull()?.dateIso ?: todayIso
    val isToday = displayedDateIso == todayIso
    val locale = appLocale()
    val dayHeaderFormatter = rememberLocalizedDateFormatter(DayHeaderSkeleton)
    val dateLabel = remember(displayedDateIso, dayHeaderFormatter) {
        runCatching { LocalDate.parse(displayedDateIso).format(dayHeaderFormatter) }
            .getOrDefault(displayedDateIso)
            .capitalizeFirst(locale)
    }
    val classCount = pluralStringResource(R.plurals.home_classes_short, dayEvents.size, dayEvents.size)

    val remaining = remember(dayEvents, now) { dayEvents.filter { it.timeSlot.endEpochMillis > now } }
    val source = remaining.ifEmpty { dayEvents }
    val visible = source.take(3)
    val hiddenCount = source.size - visible.size
    val activeId = uiState.activeClass?.id
    val nextId = remaining.firstOrNull { it.timeSlot.startEpochMillis > now }?.id

    WseiCard(
        modifier = modifier,
        onClick = onClick,
        shape = WseiHeroShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        verticalSpacing = WseiSpacing.s
    ) {
        WseiCardHeader(
            title = if (isToday) stringResource(R.string.home_today) else dateLabel,
            overline = when {
                dayEvents.isEmpty() -> stringResource(R.string.nav_schedule)
                isToday -> stringResource(R.string.home_today_overline, dateLabel, classCount)
                else -> stringResource(R.string.home_next_session_overline, classCount)
            },
            icon = WseiIcons.CalendarMonth,
            iconContainerColor = MaterialTheme.colorScheme.primary,
            iconContentColor = MaterialTheme.colorScheme.onPrimary,
            trailing = { ArrowBubble() }
        )

        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.home_no_classes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = WseiSpacing.s)
            )
        } else {
            visible.forEach { event ->
                WseiClassCard(
                    event = event,
                    nowEpochMillis = now,
                    state = classRowStateOf(event, now, activeId, nextId),
                    compact = true,
                    showContainer = false,
                    lecturerLabel = uiState.maskLecturer(event.lecturer.localizedNameOrPlaceholder())
                )
            }
            if (hiddenCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.home_more_in_schedule, hiddenCount, hiddenCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = WseiSpacing.s, top = WseiSpacing.xs)
                )
            }
        }
    }
}

@Composable
private fun ArrowBubble(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = WseiIcons.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Skróty — siatka 2×2 kolorowych kafli z falowanymi ikonami (M3 Expressive „hero tiles”).
// -----------------------------------------------------------------------------------------

private data class Shortcut(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val iconShape: Shape,
    val containerColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val onAccentColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickShortcutsGrid(
    uiState: ScheduleUiState,
    onOpenSchedule: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenFinances: () -> Unit,
    onOpenAnnouncements: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val session = uiState.currentSession
    val unread = uiState.unreadAnnouncementsCount
    val unpaidCount = uiState.financials.count { !it.isPaid }
    val weekClasses = uiState.currentWeekClassCount

    val shortcuts = listOf(
        Shortcut(
            title = stringResource(R.string.nav_schedule),
            value = if (weekClasses > 0) {
                pluralStringResource(R.plurals.home_week_classes, weekClasses, weekClasses)
            } else {
                stringResource(R.string.home_week_free)
            },
            icon = WseiIcons.CalendarMonth,
            iconShape = M3XCookieShape,
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            accentColor = colors.primary,
            onAccentColor = colors.onPrimary,
            onClick = onOpenSchedule
        ),
        Shortcut(
            title = stringResource(R.string.home_shortcut_grades),
            value = stringResource(
                R.string.home_average_value,
                uiState.maskSensitive(String.format(Locale.US, "%.2f", session?.averageCurrentSemester ?: 0.0))
            ),
            icon = WseiIcons.School,
            iconShape = M3XCloverShape,
            containerColor = colors.secondaryContainer,
            contentColor = colors.onSecondaryContainer,
            accentColor = colors.secondary,
            onAccentColor = colors.onSecondary,
            onClick = onOpenSubjects
        ),
        Shortcut(
            title = stringResource(R.string.nav_finances),
            value = if (unpaidCount > 0) {
                pluralStringResource(R.plurals.home_unpaid_count, unpaidCount, unpaidCount)
            } else {
                stringResource(R.string.home_all_paid)
            },
            icon = WseiIcons.AccountBalanceWallet,
            iconShape = M3XSunnyShape,
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer,
            accentColor = colors.tertiary,
            onAccentColor = colors.onTertiary,
            onClick = onOpenFinances
        ),
        Shortcut(
            title = stringResource(R.string.announcements_title),
            value = if (unread > 0) {
                pluralStringResource(R.plurals.home_unread_count, unread, unread)
            } else {
                stringResource(R.string.home_no_new_announcements)
            },
            icon = WseiIcons.Campaign,
            iconShape = M3XCookieShape,
            containerColor = if (unread > 0) colors.errorContainer else colors.surfaceContainerHigh,
            contentColor = if (unread > 0) colors.onErrorContainer else colors.onSurface,
            accentColor = if (unread > 0) colors.error else colors.primary,
            onAccentColor = if (unread > 0) colors.onError else colors.onPrimary,
            onClick = onOpenAnnouncements
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
        shortcuts.chunked(2).forEachIndexed { rowIndex, row ->
            // Intrinsic height keeps both tiles of a row equally tall when a larger font scale
            // makes one of them grow past the default height (previously the text was clipped).
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                row.forEachIndexed { columnIndex, shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        shape = shortcutTileShape(rowIndex, columnIndex),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

/** Kafle stykające się wewnętrznymi rogami tworzą jeden „blok” z dużymi rogami na zewnątrz. */
private fun shortcutTileShape(row: Int, column: Int): Shape {
    val outer = 32.dp
    val inner = 10.dp
    return RoundedCornerShape(
        topStart = if (row == 0 && column == 0) outer else inner,
        topEnd = if (row == 0 && column == 1) outer else inner,
        bottomStart = if (row == 1 && column == 0) outer else inner,
        bottomEnd = if (row == 1 && column == 1) outer else inner
    )
}

@Composable
private fun ShortcutTile(
    shortcut: Shortcut,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = shortcut.onClick,
        shape = shape,
        color = shortcut.containerColor,
        contentColor = shortcut.contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .heightIn(min = 132.dp)
            .wseiPressScale(interactionSource, pressedScale = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(WseiSpacing.l),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            WseiIconBadge(
                icon = shortcut.icon,
                shape = shortcut.iconShape,
                containerColor = shortcut.accentColor,
                contentColor = shortcut.onAccentColor,
                size = 46.dp,
                iconSize = 22.dp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WseiSpacing.s),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = shortcut.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shortcut.value,
                    style = MaterialTheme.typography.labelMedium,
                    color = shortcut.contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Najbliższa opłata
// -----------------------------------------------------------------------------------------

@Composable
private fun NextPaymentCard(
    uiState: ScheduleUiState,
    onClick: () -> Unit
) {
    val nextUnpaid = remember(uiState.financials) {
        uiState.financials.sortedForDisplay().firstOrNull { !it.isPaid }
    }
    val urgent = nextUnpaid?.isDueThisMonth() == true
    val colors = MaterialTheme.colorScheme
    val containerColor = if (urgent) colors.errorContainer else colors.surfaceContainer
    val contentColor = if (urgent) colors.onErrorContainer else colors.onSurface

    WseiCard(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor
    ) {
        WseiCardHeader(
            title = when {
                nextUnpaid == null -> stringResource(R.string.home_all_paid)
                urgent -> stringResource(R.string.home_payment_due_this_month)
                else -> stringResource(R.string.home_payment_next)
            },
            overline = stringResource(R.string.nav_finances),
            overlineColor = if (urgent) colors.error else colors.primary,
            icon = if (nextUnpaid == null) WseiIcons.CheckCircle else WseiIcons.AccountBalanceWallet,
            iconContainerColor = if (urgent) colors.error else colors.secondaryContainer,
            iconContentColor = if (urgent) colors.onError else colors.onSecondaryContainer,
            trailing = { ArrowBubble(containerColor = contentColor.copy(alpha = 0.08f), contentColor = contentColor) }
        )

        if (nextUnpaid != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                ) {
                    Text(
                        text = nextUnpaid.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.home_payment_due_date, nextUnpaid.dueDateIso),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
                Text(
                    text = uiState.maskSensitive(formatPln(nextUnpaid.outstandingAmountPln)),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = stringResource(R.string.home_payment_none_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.75f)
            )
        }
    }
}
