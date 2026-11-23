@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package pl.lublin.wsei.plan.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocus
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusKind
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
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
import pl.lublin.wsei.plan.ui.schedule.ScheduleAccentBadge
import pl.lublin.wsei.plan.ui.schedule.ScheduleCountdown
import pl.lublin.wsei.plan.ui.schedule.ScheduleStatusChip
import pl.lublin.wsei.plan.ui.schedule.ScheduleUiState
import pl.lublin.wsei.plan.ui.schedule.relativeDaysLabel
import pl.lublin.wsei.plan.ui.schedule.rememberDayTitle
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XCloverShape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSunnyShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
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
import pl.lublin.wsei.plan.l10n.labelRes
import pl.lublin.wsei.plan.l10n.localizedLabel
import pl.lublin.wsei.plan.l10n.localizedNameOrPlaceholder
import pl.lublin.wsei.plan.l10n.rememberLocalizedDateFormatter

/** CLDR skeleton of the day headers ("piątek, 20 listopada" / "Friday, November 20"). */
private const val DayHeaderSkeleton = "EEEEdMMMM"

/**
 * Ekran Start: powitanie, karta dnia „w centrum uwagi” (trwające / następne zajęcia albo
 * najbliższy dzień z zajęciami) z listą pozostałych zajęć, szybkie skróty i najbliższa opłata.
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
        item(key = "home_intro", contentType = "home_intro") {
            HomeIntro(
                uiState = uiState,
                todayIso = todayIso,
                modifier = if (windowLayout.isCompact) Modifier else Modifier.wseiReadableWidth(HomeDashboardMaxWidth)
            )
        }
        if (windowLayout.isCompact) {
            item(key = "home_focus", contentType = "home_focus") {
                HomeFocusSection(uiState = uiState, todayIso = todayIso, onOpenSchedule = onOpenSchedule)
            }
            item(key = "home_shortcuts", contentType = "home_shortcuts") {
                QuickShortcutsGrid(
                    uiState = uiState,
                    onOpenSchedule = onOpenSchedule,
                    onOpenSubjects = onOpenSubjects,
                    onOpenFinances = onOpenFinances,
                    onOpenAnnouncements = onOpenAnnouncements
                )
            }
            item(key = "home_payment", contentType = "home_payment") {
                NextPaymentCard(uiState = uiState, onClick = onOpenFinances)
            }
        } else {
            // Tablets, unfolded foldables and landscape: the focus hero and its day list on the
            // left, the summary tiles in a side column — instead of cards stretched edge to edge.
            item(key = "home_dashboard", contentType = "home_dashboard") {
                Row(
                    modifier = Modifier.wseiReadableWidth(HomeDashboardMaxWidth),
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                ) {
                    val primaryWeight = if (windowLayout.widthClass == WseiWidthClass.EXPANDED) 0.58f else 0.5f
                    HomeFocusSection(
                        uiState = uiState,
                        todayIso = todayIso,
                        onOpenSchedule = onOpenSchedule,
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

// -----------------------------------------------------------------------------------------
// W centrum uwagi — karta dnia (dziś / jutro / dalej w zjeździe / następny zjazd)
// -----------------------------------------------------------------------------------------

/** Visual state of the focus hero; switching between these is the only hero transition. */
private enum class FocusMode { LIVE, NEXT_TODAY, AWAY, NONE }

/**
 * What the focus hero shows. [event] is the class in the spotlight (active, next, or the first
 * class of the focus day). The AnimatedContent animates on [key] only, so the per-minute ticker
 * updates the countdown in place instead of re-running the transition.
 */
@Immutable
private data class FocusHeroModel(
    val mode: FocusMode,
    val focus: ScheduleFocus,
    val event: ClassEvent?
) {
    val key: Any get() = Triple(mode, event?.id, focus.dateIso)
}

private fun focusHeroModelOf(focus: ScheduleFocus): FocusHeroModel = when {
    focus.kind == ScheduleFocusKind.TODAY && focus.activeEvent != null ->
        FocusHeroModel(FocusMode.LIVE, focus, focus.activeEvent)
    focus.kind == ScheduleFocusKind.TODAY && focus.nextEvent != null ->
        FocusHeroModel(FocusMode.NEXT_TODAY, focus, focus.nextEvent)
    focus.kind != ScheduleFocusKind.NONE && focus.kind != ScheduleFocusKind.TODAY && focus.events.isNotEmpty() ->
        FocusHeroModel(FocusMode.AWAY, focus, focus.events.first())
    else -> FocusHeroModel(FocusMode.NONE, focus, null)
}

/**
 * Start's schedule block: the expressive focus hero driven by [ScheduleUiState.scheduleFocus]
 * and, below it, a compact list of the focus day's remaining classes.
 */
@Composable
private fun HomeFocusSection(
    uiState: ScheduleUiState,
    todayIso: String,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = uiState.scheduleFocus
    val model = remember(focus) { focusHeroModelOf(focus) }
    val now = uiState.nowEpochMillis
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
    ) {
        AnimatedContent(
            targetState = model,
            contentKey = { it.key },
            transitionSpec = {
                (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.spatial(), initialScale = 0.96f)) togetherWith
                    fadeOut(WseiMotion.fastEffects()) using
                    SizeTransform(clip = false) { _, _ -> WseiMotion.size() }
            },
            contentAlignment = Alignment.TopCenter,
            label = "homeFocusHero"
        ) { shown ->
            FocusHeroCard(
                model = shown,
                uiState = uiState,
                todayIso = todayIso,
                nowEpochMillis = now,
                onClick = onOpenSchedule
            )
        }
        // The list follows the focus day; it disappears for the calm "no classes" state. The last
        // shown day is kept in a plain holder so the list keeps its rows while it animates out.
        val lastListModel = remember { arrayOfNulls<FocusHeroModel>(1) }
        if (model.mode != FocusMode.NONE) lastListModel[0] = model
        val listModel = lastListModel[0]
        val upcoming = remember(listModel, now) { listModel?.remainingAfterHero(now).orEmpty() }
        AnimatedVisibility(
            visible = model.mode != FocusMode.NONE,
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
        ) {
            if (listModel != null) {
                FocusDayList(
                    uiState = uiState,
                    isToday = listModel.focus.isToday,
                    dateIso = listModel.focus.dateIso,
                    events = upcoming,
                    nowEpochMillis = now,
                    onClick = onOpenSchedule
                )
            }
        }
    }
}

/** Classes of the focus day that come after the one in the hero (all of them for a later day). */
private fun FocusHeroModel.remainingAfterHero(now: Long): List<ClassEvent> = when (mode) {
    FocusMode.LIVE, FocusMode.NEXT_TODAY -> {
        val heroStart = event?.timeSlot?.startEpochMillis ?: now
        focus.events.filter { it.id != event?.id && it.timeSlot.startEpochMillis >= heroStart && it.timeSlot.endEpochMillis > now }
    }
    FocusMode.AWAY -> focus.events
    FocusMode.NONE -> emptyList()
}

/** Minutes as "25 min" or "1 h 05 min". */
@Composable
private fun formatCountdown(minutes: Int): String = if (minutes >= 60) {
    stringResource(R.string.home_focus_duration_hours, minutes / 60, minutes % 60)
} else {
    stringResource(R.string.schedule_hero_remaining_value, minutes)
}

@Composable
private fun FocusHeroCard(
    model: FocusHeroModel,
    uiState: ScheduleUiState,
    todayIso: String,
    nowEpochMillis: Long,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val event = model.event
    when (model.mode) {
        FocusMode.LIVE, FocusMode.NEXT_TODAY -> {
            if (event == null) return
            val live = model.mode == FocusMode.LIVE
            val container = if (live) colors.primaryContainer else colors.tertiaryContainer
            val content = if (live) colors.onPrimaryContainer else colors.onTertiaryContainer
            val accent = if (live) colors.primary else colors.tertiary
            val onAccent = if (live) colors.onPrimary else colors.onTertiary
            val minutes = if (live) {
                event.remainingMinutes(nowEpochMillis)
            } else {
                ((event.timeSlot.startEpochMillis - nowEpochMillis + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
            }
            val location = if (event.deliveryMode == DeliveryMode.ON_SITE) {
                event.room.localizedLabel()
            } else {
                stringResource(event.deliveryMode.labelRes)
            }
            FocusHeroLayout(
                containerColor = container,
                contentColor = content,
                accentColor = accent,
                onAccentColor = onAccent,
                status = stringResource(if (live) R.string.schedule_hero_status_live else R.string.class_state_next),
                trailing = stringResource(
                    R.string.schedule_hero_time_range,
                    event.timeSlot.startFormatted,
                    event.timeSlot.endFormatted
                ),
                icon = WseiIcons.forSubject(event.subject.name, event.classType),
                overline = stringResource(event.classType.labelRes),
                title = event.subject.name,
                countdownValue = formatCountdown(minutes),
                countdownCaption = stringResource(
                    if (live) R.string.schedule_hero_remaining_label else R.string.home_focus_until_start
                ),
                message = null,
                pills = listOf(
                    WseiIcons.LocationOn to location,
                    WseiIcons.Person to uiState.maskLecturer(event.lecturer.localizedNameOrPlaceholder())
                ),
                onClick = onClick
            )
        }
        FocusMode.AWAY -> {
            val focus = model.focus
            val todayHadClasses = remember(uiState.allEvents, todayIso) {
                uiState.allEvents.any { it.dateIso == todayIso }
            }
            // Same resolver as Plan zajęć's day-off card, so both screens agree on the countdown.
            val nextDay = remember(uiState.allEvents, todayIso, focus.dateIso) {
                ScheduleFocusResolver.nextClassDayAfter(uiState.allEvents, todayIso, nowEpochMillis)
                    ?.takeIf { it.dateIso == focus.dateIso }
            }
            val daysUntil = nextDay?.daysFromToday ?: 0
            val count = nextDay?.blockCount ?: focus.events.size
            val kindLabel = stringResource(
                when (focus.kind) {
                    ScheduleFocusKind.TOMORROW -> R.string.home_focus_status_tomorrow
                    ScheduleFocusKind.LATER_THIS_ZJAZD -> R.string.home_focus_status_later
                    else -> R.string.home_focus_status_next_zjazd
                }
            )
            // "Jutro" already says when; later days get "• za 11 dni".
            val countdown = if (daysUntil > 1) relativeDaysLabel(daysUntil) else null
            FocusHeroLayout(
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                accentColor = colors.secondary,
                onAccentColor = colors.onSecondary,
                // Today's state is a quiet header of its own, apart from the next-day block below.
                status = stringResource(
                    if (todayHadClasses) R.string.home_focus_free_now else R.string.home_focus_free_today
                ),
                statusIcon = WseiIcons.CheckCircle,
                trailing = null,
                icon = WseiIcons.CalendarMonth,
                overline = if (countdown != null) {
                    stringResource(R.string.schedule_day_date_count, kindLabel, countdown)
                } else {
                    kindLabel
                },
                title = rememberDayTitle(focus.dateIso),
                countdownValue = nextDay?.firstStartLabel ?: event?.timeSlot?.startFormatted,
                countdownCaption = stringResource(R.string.home_focus_first_class_label),
                message = null,
                pills = listOf(
                    WseiIcons.School to pluralStringResource(R.plurals.schedule_day_blocks, count, count)
                ),
                onClick = onClick
            )
        }
        FocusMode.NONE -> FocusHeroLayout(
            containerColor = colors.surfaceContainerHigh,
            contentColor = colors.onSurface,
            accentColor = colors.tertiaryContainer,
            onAccentColor = colors.onTertiaryContainer,
            status = null,
            trailing = null,
            icon = WseiIcons.CheckCircle,
            overline = null,
            title = stringResource(R.string.schedule_day_off_title),
            countdownValue = null,
            countdownCaption = null,
            message = stringResource(R.string.home_no_classes),
            pills = emptyList(),
            onClick = onClick
        )
    }
}

/**
 * Shared layout of every focus state, in the visual language of Plan zajęć's „Trwa teraz” hero.
 * All states keep the same geometry: header (status + trailing), cookie accent badge with
 * overline and title, big figure with caption, and a footer with detail pills and the
 * "Otwórz plan ›" affordance (the whole card opens Plan zajęć).
 *
 * [statusIcon] == null renders [status] as an accent chip ("Trwa teraz"); with an icon it is a
 * quiet line about today ("Dziś bez zajęć") that stays visually apart from the block below.
 */
@Composable
private fun FocusHeroLayout(
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    status: String?,
    trailing: String?,
    icon: ImageVector,
    overline: String?,
    title: String,
    countdownValue: String?,
    countdownCaption: String?,
    message: String?,
    pills: List<Pair<ImageVector, String>>,
    onClick: () -> Unit,
    statusIcon: ImageVector? = null
) {
    val mutedColor = contentColor.copy(alpha = 0.78f)
    WseiCard(
        onClick = onClick,
        shape = ActiveHeroCardAsymmetricShape,
        containerColor = containerColor,
        contentColor = contentColor,
        contentPadding = PaddingValues(WseiSpacing.xl),
        verticalSpacing = WseiSpacing.l,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (status != null || trailing != null) {
            Row(
                modifier = Modifier.heightIn(min = FocusHeaderMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                if (status != null) {
                    if (statusIcon == null) {
                        ScheduleStatusChip(text = status, containerColor = accentColor, contentColor = onAccentColor)
                    } else {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = mutedColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelLarge,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            ScheduleAccentBadge(icon = icon, containerColor = accentColor, contentColor = onAccentColor)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                if (!overline.isNullOrBlank()) {
                    Text(
                        text = overline,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor.takeIf { status != null } ?: mutedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        if (countdownValue != null && countdownCaption != null) {
            ScheduleCountdown(value = countdownValue, caption = countdownCaption, captionColor = mutedColor)
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
            ) {
                pills.forEach { (pillIcon, text) ->
                    WseiPill(
                        text = text,
                        icon = pillIcon,
                        containerColor = contentColor.copy(alpha = 0.1f),
                        contentColor = contentColor
                    )
                }
            }
            OpenScheduleAffordance(color = mutedColor)
        }
    }
}

/** Keeps the chip header and the quiet status line at the same height across focus states. */
private val FocusHeaderMinHeight = 30.dp

/** Subtle "Otwórz plan ›" at the end of the hero; the whole card is the click target. */
@Composable
private fun OpenScheduleAffordance(color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
    ) {
        Text(
            text = stringResource(R.string.home_focus_open_schedule),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1
        )
        Icon(
            imageVector = WseiIcons.ChevronRight,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Compact list of the focus day's (remaining) classes under the hero. */
@Composable
private fun FocusDayList(
    uiState: ScheduleUiState,
    isToday: Boolean,
    dateIso: String?,
    events: List<ClassEvent>,
    nowEpochMillis: Long,
    onClick: () -> Unit
) {
    val visible = events.take(FocusListMaxRows)
    val hiddenCount = events.size - visible.size
    // "Dalej dzisiaj" only for today; another day is named by its date, never "Plan dnia".
    val shortDateFormatter = rememberLocalizedDateFormatter(FocusListDateSkeleton)
    val title = if (isToday) {
        stringResource(R.string.home_focus_list_later_today)
    } else {
        val shortDate = remember(dateIso, shortDateFormatter) {
            dateIso?.let { iso -> runCatching { LocalDate.parse(iso).format(shortDateFormatter) }.getOrNull() }.orEmpty()
        }
        stringResource(R.string.home_focus_list_upcoming_date, shortDate)
    }
    val activeId = uiState.scheduleFocus.activeEvent?.id
    val nextId = uiState.scheduleFocus.nextEvent?.id
    WseiCard(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        verticalSpacing = WseiSpacing.s,
        contentPadding = PaddingValues(horizontal = WseiSpacing.l, vertical = WseiSpacing.l),
        // The only size animation of this card: rows leave as classes end.
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(WseiMotion.size())
    ) {
        Row(
            modifier = Modifier.padding(start = WseiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            ArrowBubble()
        }
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.home_focus_last_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = WseiSpacing.xs, bottom = WseiSpacing.xs)
            )
        } else {
            visible.forEachIndexed { index, event ->
                key(event.id, index) {
                    WseiClassCard(
                        event = event,
                        nowEpochMillis = nowEpochMillis,
                        // The hero already carries the live/next highlight; rows stay quiet unless
                        // they are the active or next class of a later-day list.
                        state = classRowStateOf(event, nowEpochMillis, activeId, nextId?.takeIf { !isToday }),
                        compact = true,
                        showContainer = false,
                        lecturerLabel = uiState.maskLecturer(event.lecturer.localizedNameOrPlaceholder())
                    )
                }
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

/** CLDR skeleton of the list title date ("pt., 4 gru" / "Fri, Dec 4"). */
private const val FocusListDateSkeleton = "EEEdMMM"

/** Rows in the Start list before "+N więcej w planie". */
private const val FocusListMaxRows = 4

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
