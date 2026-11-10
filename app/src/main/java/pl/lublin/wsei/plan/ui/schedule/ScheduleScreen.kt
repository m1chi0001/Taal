@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package pl.lublin.wsei.plan.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiClassCard
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.classRowStateOf
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import java.time.YearMonth

/**
 * Plan zajęć (Material 3 Expressive):
 * - przełącznik „Mój plan / Plan grupy” jako połączona grupa przycisków,
 * - kalendarz tygodniowy rozwijany do pełnego miesiąca,
 * - karta trwających zajęć z falującym postępem i osią dnia,
 * - lista zajęć wybranego dnia.
 * Na szerokich ekranach (≥ 840 dp) układ dwukolumnowy.
 */
@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit,
    onRefreshSchedule: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onLogout: () -> Unit = {}
) {
    WseiScreenFrame(
        title = "Plan zajęć",
        modifier = modifier.fillMaxSize(),
        onRefresh = onRefreshSchedule,
        isRefreshing = uiState.isRefreshing
    ) { scrollModifier ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
                ScheduleControls(
                    uiState = uiState,
                    onSelectDay = onSelectDay,
                    onSelectDate = onSelectDate,
                    onToggleCalendarExpanded = onToggleCalendarExpanded,
                    onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                    onNextCalendarPeriod = onNextCalendarPeriod,
                    onSelectScheduleFilter = onSelectScheduleFilter,
                    onToggleHeroExpanded = onToggleHeroExpanded,
                    onSyncLiveNotification = onSyncLiveNotification,
                    modifier = controlsModifier
                )
            }

            if (maxWidth >= 840.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = WseiSpacing.xxl),
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxl)
                ) {
                    controls(
                        Modifier
                            .weight(0.46f)
                            .widthIn(max = 520.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = WseiSpacing.bottomBarClearance)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.54f)
                            .fillMaxHeight()
                            .then(scrollModifier),
                        contentPadding = PaddingValues(top = WseiSpacing.xs, bottom = WseiSpacing.bottomBarClearance),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                    ) {
                        dayTimeline(uiState)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollModifier),
                    contentPadding = WseiSpacing.screenContent,
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                ) {
                    item(key = "schedule_controls") { controls(Modifier) }
                    dayTimeline(uiState)
                }
            }
        }
    }
}

@Composable
private fun ScheduleControls(
    uiState: ScheduleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
    ) {
        ScheduleFilterButtonGroup(
            selectedFilter = uiState.scheduleFilterType,
            onSelectFilter = onSelectScheduleFilter
        )
        ExpandableCalendarCard(
            daysOfCurrentWeek = uiState.days,
            selectedDate = uiState.selectedDate,
            displayedYear = uiState.displayedYear,
            displayedMonth = uiState.displayedMonth,
            scheduledMonthDays = uiState.scheduledMonthDays,
            isExpanded = uiState.isCalendarExpanded,
            onToggleExpanded = onToggleCalendarExpanded,
            onPreviousPeriod = onPreviousCalendarPeriod,
            onNextPeriod = onNextCalendarPeriod,
            onSelectDayInWeek = onSelectDay,
            onSelectDate = onSelectDate
        )
        val activeClass = uiState.activeClass
        if (activeClass != null) {
            ActiveClassHeroCard(
                activeClass = activeClass,
                dayEvents = uiState.activeDayEvents,
                nextClassPreview = uiState.nextClassPreview,
                nowEpochMillis = uiState.nowEpochMillis,
                isExpanded = uiState.isHeroExpanded,
                onToggleExpand = onToggleHeroExpanded,
                onTriggerLiveNotification = { onSyncLiveNotification(activeClass) }
            )
        }
    }
}

/** Nagłówek dnia i karty zajęć — wspólne dla układu jedno- i dwukolumnowego. */
private fun LazyListScope.dayTimeline(uiState: ScheduleUiState) {
    val selectedDay = uiState.selectedDay
    item(key = "day_header") {
        DaySectionHeader(selectedDay = selectedDay)
    }
    val dayEvents = selectedDay?.events.orEmpty()
    if (dayEvents.isEmpty()) {
        item(key = "empty_day") {
            WseiEmptyState(
                icon = WseiIcons.CalendarMonth,
                title = "Wolne od zajęć",
                message = "Na ten dzień nie ma zajęć. Wybierz inny dzień w kalendarzu."
            )
        }
    } else {
        val now = uiState.nowEpochMillis
        val activeId = uiState.activeClass?.id
        val nextId = uiState.nextClassPreview?.nextEvent?.id
        items(
            items = dayEvents,
            key = { it.id },
            contentType = { "timeline_class_row" }
        ) { event ->
            WseiClassCard(
                event = event,
                nowEpochMillis = now,
                state = classRowStateOf(event, now, activeId, nextId)
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// Przełącznik planu — połączona grupa przycisków (M3 Expressive „connected button group”)
// -----------------------------------------------------------------------------------------

@Composable
private fun ScheduleFilterButtonGroup(
    selectedFilter: ScheduleFilterType,
    onSelectFilter: (ScheduleFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = ScheduleFilterType.entries
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
    ) {
        filters.forEachIndexed { index, filter ->
            val selected = filter == selectedFilter
            val isFirst = index == 0
            val isLast = index == filters.lastIndex
            // Wybrany segment zaokrągla się w pełną pigułkę; pozostałe mają małe rogi wewnętrzne.
            val innerCorner by animateDpAsState(
                targetValue = if (selected) 26.dp else 8.dp,
                animationSpec = WseiMotion.fastSpatial(),
                label = "filterInnerCorner"
            )
            val containerColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = WseiMotion.effects(),
                label = "filterContainer"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = WseiMotion.effects(),
                label = "filterContent"
            )
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(
                    topStart = if (isFirst) 26.dp else innerCorner,
                    bottomStart = if (isFirst) 26.dp else innerCorner,
                    topEnd = if (isLast) 26.dp else innerCorner,
                    bottomEnd = if (isLast) 26.dp else innerCorner
                ),
                color = containerColor,
                contentColor = contentColor,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .wseiPressScale(interactionSource, pressedScale = 0.96f)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = selected
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = WseiSpacing.l),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (filter == ScheduleFilterType.MY_PLAN) WseiIcons.Person else WseiIcons.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(WseiSpacing.s))
                    Text(
                        text = filter.polishLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// Kalendarz: tydzień ⇄ miesiąc
// -----------------------------------------------------------------------------------------

@Composable
private fun ExpandableCalendarCard(
    daysOfCurrentWeek: List<AcademicDay>,
    selectedDate: LocalDate,
    displayedYear: Int,
    displayedMonth: Int,
    scheduledMonthDays: Set<Int>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectDayInWeek: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayIso = remember { java.time.LocalDate.now().toString() }
    val weekStart = daysOfCurrentWeek.firstOrNull()?.date
    val weekEnd = daysOfCurrentWeek.lastOrNull()?.date
    val subtitle = when {
        isExpanded -> "Widok miesiąca"
        weekStart != null && weekEnd != null -> "${weekStart.dayOfMonth}.${weekStart.monthNumber.twoDigits()} – " +
            "${weekEnd.dayOfMonth}.${weekEnd.monthNumber.twoDigits()}"
        else -> ""
    }

    WseiCard(
        modifier = modifier.animateContentSize(animationSpec = WseiMotion.size()),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(WseiSpacing.l),
        verticalSpacing = WseiSpacing.m
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = WseiSpacing.xs)
            ) {
                AnimatedContent(
                    targetState = "${polishMonthName(displayedMonth)} $displayedYear",
                    transitionSpec = {
                        (fadeIn(WseiMotion.effects()) + slideInHorizontally(WseiMotion.spatial()) { it / 6 }) togetherWith
                            fadeOut(WseiMotion.effects())
                    },
                    label = "calendarMonthTitle"
                ) { title ->
                    Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalIconButton(onClick = onPreviousPeriod, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = WseiIcons.ChevronLeft,
                    contentDescription = if (isExpanded) "Poprzedni miesiąc" else "Poprzedni tydzień",
                    modifier = Modifier.size(20.dp)
                )
            }
            FilledTonalIconButton(onClick = onNextPeriod, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = WseiIcons.ChevronRight,
                    contentDescription = if (isExpanded) "Następny miesiąc" else "Następny tydzień",
                    modifier = Modifier.size(20.dp)
                )
            }
            FilledTonalButton(
                onClick = onToggleExpanded,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = if (isExpanded) "Tydzień" else "Miesiąc",
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = if (isExpanded) WseiIcons.ExpandLess else WseiIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = !isExpanded,
            enter = expandVertically(WseiMotion.size()) + fadeIn(),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut()
        ) {
            WeekDaysStrip(
                days = daysOfCurrentWeek,
                selectedDate = selectedDate,
                todayIso = todayIso,
                onSelectDay = onSelectDayInWeek
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(WseiMotion.size()) + fadeIn(),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut()
        ) {
            MonthGrid(
                displayedYear = displayedYear,
                displayedMonth = displayedMonth,
                selectedDate = selectedDate,
                todayIso = todayIso,
                scheduledMonthDays = scheduledMonthDays,
                onDayClicked = { day -> onSelectDate(LocalDate(displayedYear, displayedMonth, day)) }
            )
        }
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')

@Composable
private fun WeekDaysStrip(
    days: List<AcademicDay>,
    selectedDate: LocalDate,
    todayIso: String,
    onSelectDay: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = day.date == selectedDate
            val isToday = day.date.toString() == todayIso
            val hasClasses = day.events.isNotEmpty()
            val corner by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 14.dp,
                animationSpec = WseiMotion.fastSpatial(),
                label = "dayCorner"
            )
            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    hasClasses -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                },
                animationSpec = WseiMotion.effects(),
                label = "dayContainer"
            )
            val contentColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                hasClasses -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val interactionSource = remember { MutableInteractionSource() }
            Surface(
                onClick = { onSelectDay(index) },
                shape = RoundedCornerShape(corner),
                color = containerColor,
                contentColor = contentColor,
                border = if (isToday && !isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .wseiPressScale(interactionSource, pressedScale = 0.9f)
                    .semantics {
                        this.selected = isSelected
                        stateDescription = buildString {
                            if (isToday) append("Dzisiaj. ")
                            append(if (hasClasses) "${day.events.size} zajęć" else "Bez zajęć")
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = day.shortDayLabel, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (isSelected || hasClasses) FontWeight.Bold else FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(width = if (hasClasses) 14.dp else 4.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasClasses) {
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                } else {
                                    containerColor
                                }
                            )
                    )
                }
            }
        }
    }
}

private val PolishWeekdayHeaders = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

@Composable
private fun MonthGrid(
    displayedYear: Int,
    displayedMonth: Int,
    selectedDate: LocalDate,
    todayIso: String,
    scheduledMonthDays: Set<Int>,
    onDayClicked: (Int) -> Unit
) {
    val cells = remember(displayedYear, displayedMonth) {
        val yearMonth = YearMonth.of(displayedYear, displayedMonth)
        val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1
        buildList<Int?> {
            repeat(firstDayOffset) { add(null) }
            for (d in 1..yearMonth.lengthOfMonth()) add(d)
            while (size % 7 != 0) add(null)
        }.chunked(7)
    }
    val todayPrefix = "%04d-%02d-".format(displayedYear, displayedMonth)
    val todayDay = if (todayIso.startsWith(todayPrefix)) todayIso.takeLast(2).toIntOrNull() else null

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PolishWeekdayHeaders.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        cells.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                week.forEach { dayNum ->
                    if (dayNum == null) {
                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                    } else {
                        val isSelected = selectedDate.year == displayedYear &&
                            selectedDate.monthNumber == displayedMonth &&
                            selectedDate.dayOfMonth == dayNum
                        val hasClasses = dayNum in scheduledMonthDays
                        val isToday = dayNum == todayDay
                        Surface(
                            onClick = { onDayClicked(dayNum) },
                            shape = if (isSelected) M3XCookieShape else if (hasClasses) M3XSquircleShape else CircleShape,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                hasClasses -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            },
                            contentColor = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasClasses -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            border = if (isToday && !isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .semantics { this.selected = isSelected }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected || hasClasses) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun polishMonthName(monthNumber: Int): String = when (monthNumber) {
    1 -> "Styczeń"
    2 -> "Luty"
    3 -> "Marzec"
    4 -> "Kwiecień"
    5 -> "Maj"
    6 -> "Czerwiec"
    7 -> "Lipiec"
    8 -> "Sierpień"
    9 -> "Wrzesień"
    10 -> "Październik"
    11 -> "Listopad"
    12 -> "Grudzień"
    else -> "Miesiąc"
}

// -----------------------------------------------------------------------------------------
// Trwające zajęcia
// -----------------------------------------------------------------------------------------

@Composable
fun ActiveClassHeroCard(
    activeClass: ClassEvent,
    dayEvents: List<ClassEvent>,
    nextClassPreview: NextClassPreview?,
    nowEpochMillis: Long,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onTriggerLiveNotification: () -> Unit = {}
) {
    val elapsedMin = activeClass.elapsedMinutes(nowEpochMillis)
    val totalMin = activeClass.totalDurationMinutes
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val animatedProgress by animateFloatAsState(
        targetValue = activeClass.progressFraction(nowEpochMillis),
        animationSpec = WseiMotion.size(),
        label = "liveClassProgress"
    )
    val colors = MaterialTheme.colorScheme
    val location = if (activeClass.deliveryMode == DeliveryMode.ON_SITE) {
        activeClass.room.displayLabel
    } else {
        activeClass.deliveryMode.polishBadge
    }

    WseiCard(
        onClick = onToggleExpand,
        shape = ActiveHeroCardAsymmetricShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        verticalSpacing = WseiSpacing.l,
        modifier = modifier
            .animateContentSize(WseiMotion.size())
            .semantics {
                stateDescription = "Trwają zajęcia: ${activeClass.subject.name}, pozostało $remainingMin minut"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            WseiIconBadge(
                icon = WseiIcons.forSubject(activeClass.subject.name, activeClass.classType),
                shape = M3XCookieShape,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                size = 52.dp,
                iconSize = 26.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trwa teraz • ${activeClass.classType.polishLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Text(
                    text = activeClass.subject.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$remainingMin min",
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1
                )
                Text(
                    text = "do końca • $elapsedMin z $totalMin min",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            WseiPill(
                text = "do ${activeClass.timeSlot.endFormatted}",
                icon = WseiIcons.Schedule,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                emphasized = true
            )
        }

        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .semantics { contentDescription = "Postęp zajęć: $elapsedMin z $totalMin minut" },
            color = colors.primary,
            trackColor = colors.onPrimaryContainer.copy(alpha = 0.16f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            WseiPill(
                text = location,
                icon = WseiIcons.LocationOn,
                containerColor = colors.onPrimaryContainer.copy(alpha = 0.1f),
                contentColor = colors.onPrimaryContainer
            )
            WseiPill(
                text = activeClass.lecturer.displayNameOrPlaceholder,
                icon = WseiIcons.Person,
                containerColor = colors.onPrimaryContainer.copy(alpha = 0.1f),
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        SegmentedDayProgressBarCompose(
            dayEvents = dayEvents.ifEmpty { listOf(activeClass) },
            nowEpochMillis = nowEpochMillis
        )

        AnimatedVisibility(
            visible = isExpanded && nextClassPreview != null,
            enter = expandVertically(WseiMotion.size()) + fadeIn(),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut()
        ) {
            if (nextClassPreview != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.onPrimaryContainer.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = WseiSpacing.l, vertical = WseiSpacing.m),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                    ) {
                        Text(
                            text = "Następnie",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.tertiary
                        )
                        Text(
                            text = nextClassPreview.summaryText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Oś dnia: zajęcia i przerwy jako segmenty proporcjonalne do czasu trwania,
 * każdy z własnym postępem.
 */
@Composable
fun SegmentedDayProgressBarCompose(
    dayEvents: List<ClassEvent>,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier
) {
    val segments = remember(dayEvents) { buildDaySegments(dayEvents) }
    if (segments.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Twój dzień",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
        ) {
            segments.forEach { segment ->
                val ongoing = nowEpochMillis in segment.startMs until segment.endMs
                Column(
                    modifier = Modifier.weight(segment.weight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
                ) {
                    LinearProgressIndicator(
                        progress = { segment.progressAt(nowEpochMillis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (ongoing) 10.dp else 6.dp)
                            .clip(CircleShape),
                        color = if (segment.isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = segment.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (ongoing) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

private class DaySegment(
    val label: String,
    val isBreak: Boolean,
    val startMs: Long,
    val endMs: Long,
    val weight: Float
) {
    fun progressAt(now: Long): Float {
        val total = (endMs - startMs).coerceAtLeast(1L)
        val elapsed = (now - startMs).coerceIn(0L, total)
        return elapsed.toFloat() / total.toFloat()
    }
}

private fun buildDaySegments(dayEvents: List<ClassEvent>): List<DaySegment> {
    val sorted = dayEvents.sortedBy { it.timeSlot.startEpochMillis }
    return buildList {
        sorted.forEachIndexed { index, event ->
            add(
                DaySegment(
                    label = "Zaj. ${index + 1}",
                    isBreak = false,
                    startMs = event.timeSlot.startEpochMillis,
                    endMs = event.timeSlot.endEpochMillis,
                    weight = 1.6f
                )
            )
            val next = sorted.getOrNull(index + 1)
            if (next != null && next.timeSlot.startEpochMillis > event.timeSlot.endEpochMillis) {
                add(
                    DaySegment(
                        label = "Przerwa",
                        isBreak = true,
                        startMs = event.timeSlot.endEpochMillis,
                        endMs = next.timeSlot.startEpochMillis,
                        weight = 0.8f
                    )
                )
            }
        }
    }
}

@Composable
private fun DaySectionHeader(selectedDay: AcademicDay?, modifier: Modifier = Modifier) {
    if (selectedDay == null) return
    val count = selectedDay.events.size
    val countText = when {
        count == 0 -> "Brak zajęć"
        count == 1 -> "1 blok"
        count in 2..4 -> "$count bloki"
        else -> "$count bloków"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = WseiSpacing.xs, end = WseiSpacing.xs, top = WseiSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = selectedDay.dayOfWeekLabel,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "${selectedDay.date.dayOfMonth} ${polishMonthGenitive(selectedDay.date.monthNumber)} ${selectedDay.date.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        WseiPill(
            text = if (selectedDay.isWeekendExtramuralSession && count > 0) "Zjazd • $countText" else countText,
            containerColor = if (count > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (count > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            emphasized = true
        )
    }
}

private fun polishMonthGenitive(monthNumber: Int): String = when (monthNumber) {
    1 -> "stycznia"
    2 -> "lutego"
    3 -> "marca"
    4 -> "kwietnia"
    5 -> "maja"
    6 -> "czerwca"
    7 -> "lipca"
    8 -> "sierpnia"
    9 -> "września"
    10 -> "października"
    11 -> "listopada"
    12 -> "grudnia"
    else -> ""
}
