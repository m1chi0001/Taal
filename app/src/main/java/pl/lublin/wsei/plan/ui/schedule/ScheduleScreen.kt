@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package pl.lublin.wsei.plan.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.components.WseiScreenHeader
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import java.time.YearMonth

/**
 * Main Schedule Screen ("Plan zajęć") implementing Material 3 Expressive guidelines:
 *
 * - Unified Expandable Calendar (`ExpressiveExpandableCalendarCard`):
 *   - Starts collapsed showing ONLY the active 7-day week (`Pon`..`Ndz`) with week `<`/`>` navigation
 *   - Includes an explicit button (`Rozwiń kalendarz` / `Zwiń do tygodnia`) to expand into the full
 *     interactive month grid with month `<`/`>` navigation and day selection across any month/year
 * - Personal Plan (`MsScheduleType.Student = 5`) & Group Plan (`MsScheduleType.GroupsForStudent = 100`)
 * - Live [ActiveClassHeroCard] with segmented day progress (`Zajęcia – Przerwa – Zajęcia`)
 * - Subject-matched Material Icons (`WseiIcons.forSubject`) inside M3X squircle badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        WseiScreenHeader(
            title = "Plan zaj\u0119\u0107",
            icon = WseiIcons.CalendarMonth
        )
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefreshSchedule,
            modifier = Modifier.fillMaxSize()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
            val isExpandedWindow = maxWidth >= 840.dp

            if (isExpandedWindow) {
                ExpandedTwoPaneScheduleContent(
                    uiState = uiState,
                    onSelectDay = onSelectDay,
                    onSelectDate = onSelectDate,
                    onToggleCalendarExpanded = onToggleCalendarExpanded,
                    onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                    onNextCalendarPeriod = onNextCalendarPeriod,
                    onSelectScheduleFilter = onSelectScheduleFilter,
                    onToggleHeroExpanded = onToggleHeroExpanded,
                    onSyncLiveNotification = onSyncLiveNotification
                )
            } else {
                CompactSingleColumnScheduleContent(
                    uiState = uiState,
                    onSelectDay = onSelectDay,
                    onSelectDate = onSelectDate,
                    onToggleCalendarExpanded = onToggleCalendarExpanded,
                    onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                    onNextCalendarPeriod = onNextCalendarPeriod,
                    onSelectScheduleFilter = onSelectScheduleFilter,
                    onToggleHeroExpanded = onToggleHeroExpanded,
                    onSyncLiveNotification = onSyncLiveNotification
                )
            }
        }
    }
    }
}
}

@Composable
private fun CompactSingleColumnScheduleContent(
    uiState: ScheduleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit
) {
    val selectedDay = uiState.selectedDay
    val activeClass = uiState.activeClass

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "schedule_filter_segment") {
            ScheduleFilterSegmentRow(
                selectedFilter = uiState.scheduleFilterType,
                onSelectFilter = onSelectScheduleFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        item(key = "expandable_calendar_card") {
            ExpressiveExpandableCalendarCard(
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
                onSelectDate = onSelectDate,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (activeClass != null) {
            item(key = "active_hero_${activeClass.id}") {
                ActiveClassHeroCard(
                    activeClass = activeClass,
                    dayEvents = uiState.activeDayEvents,
                    nextClassPreview = uiState.nextClassPreview,
                    nowEpochMillis = uiState.nowEpochMillis,
                    isExpanded = uiState.isHeroExpanded,
                    onToggleExpand = onToggleHeroExpanded,
                    onTriggerLiveNotification = { onSyncLiveNotification(activeClass) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "day_header") {
            DaySectionHeader(
                selectedDay = selectedDay,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        val dayEvents = selectedDay?.events.orEmpty()
        if (dayEvents.isEmpty()) {
            item(key = "empty_day") {
                EmptyDayNotice(
                    dayLabel = selectedDay?.dayOfWeekLabel ?: "Wybrany dzień",
                    dateLabel = selectedDay?.date?.toString().orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(
                items = dayEvents,
                key = { it.id },
                contentType = { "timeline_class_row" }
            ) { event ->
                val isCurrentlyOngoing = event.isOngoing(uiState.nowEpochMillis)
                val isNextUp = uiState.nextClassPreview?.nextEvent?.id == event.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCurrentlyOngoing) {
                        LiveNowTimelineDivider(
                            nowFormatted = "TERAZ W PLANIE • ${event.elapsedMinutes(uiState.nowEpochMillis)} MIN Z ${event.totalDurationMinutes} MIN"
                        )
                    }
                    TimelineClassRow(
                        event = event,
                        nowEpochMillis = uiState.nowEpochMillis,
                        isOngoing = isCurrentlyOngoing,
                        isNextUp = isNextUp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedTwoPaneScheduleContent(
    uiState: ScheduleUiState,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit
) {
    val selectedDay = uiState.selectedDay

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.46f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScheduleFilterSegmentRow(
                selectedFilter = uiState.scheduleFilterType,
                onSelectFilter = onSelectScheduleFilter
            )

            ExpressiveExpandableCalendarCard(
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

        LazyColumn(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "expanded_day_header") {
                DaySectionHeader(selectedDay = selectedDay)
            }

            val dayEvents = selectedDay?.events.orEmpty()
            if (dayEvents.isEmpty()) {
                item(key = "expanded_empty_day") {
                    EmptyDayNotice(
                        dayLabel = selectedDay?.dayOfWeekLabel ?: "Wybrany dzień",
                        dateLabel = selectedDay?.date?.toString().orEmpty()
                    )
                }
            } else {
                items(
                    items = dayEvents,
                    key = { it.id },
                    contentType = { "timeline_class_row" }
                ) { event ->
                    val isCurrentlyOngoing = event.isOngoing(uiState.nowEpochMillis)
                    val isNextUp = uiState.nextClassPreview?.nextEvent?.id == event.id
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isCurrentlyOngoing) {
                            LiveNowTimelineDivider(
                                nowFormatted = "TERAZ W PLANIE • ${event.elapsedMinutes(uiState.nowEpochMillis)} MIN Z ${event.totalDurationMinutes} MIN"
                            )
                        }
                        TimelineClassRow(
                            event = event,
                            nowEpochMillis = uiState.nowEpochMillis,
                            isOngoing = isCurrentlyOngoing,
                            isNextUp = isNextUp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Calendar Component:
 * - On start (`isExpanded = false`): shows ONLY the 7-day week strip (`Pon`..`Ndz`) with `<`/`>` week buttons.
 * - Has an explicit expand/collapse button (`Pełny miesiąc` / `Tylko tydzień`) to reveal the full month grid
 *   and navigate freely across any day, week, or month.
 */
@Composable
private fun ExpressiveExpandableCalendarCard(
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
    val monthTitle = "${polishMonthName(displayedMonth)} $displayedYear"
    val weekStart = daysOfCurrentWeek.firstOrNull()?.date
    val weekEnd = daysOfCurrentWeek.lastOrNull()?.date
    val weekRangeSubtitle = if (weekStart != null && weekEnd != null) {
        "Tydzień: ${weekStart.dayOfMonth.toString().padStart(2, '0')}.${weekStart.monthNumber.toString().padStart(2, '0')} – ${weekEnd.dayOfMonth.toString().padStart(2, '0')}.${weekEnd.monthNumber.toString().padStart(2, '0')}.${weekEnd.year}"
    } else {
        ""
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unified Home-style card header: 42.dp Squircle Icon + Overline + Title + Expand Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isExpanded) "KALENDARZ • WIDOK MIESIĘCZNY" else "KALENDARZ • ${weekRangeSubtitle.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onPreviousPeriod,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = WseiIcons.ChevronLeft,
                            contentDescription = if (isExpanded) "Poprzedni miesiąc" else "Poprzedni tydzień",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onNextPeriod,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = WseiIcons.ChevronRight,
                            contentDescription = if (isExpanded) "Następny miesiąc" else "Następny tydzień",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = onToggleExpanded,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Zwiń" else "Miesiąc",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isExpanded) WseiIcons.ExpandLess else WseiIcons.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Default collapsed view: 7-day week strip
            AnimatedVisibility(
                visible = !isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                WeekDaysStripRow(
                    days = daysOfCurrentWeek,
                    selectedDate = selectedDate,
                    onSelectDay = onSelectDayInWeek
                )
            }

            // Expanded view: Full interactive month grid
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InteractiveMonthCalendarGrid(
                    displayedYear = displayedYear,
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    scheduledMonthDays = scheduledMonthDays,
                    onDayClicked = { dayOfMonth ->
                        onSelectDate(LocalDate(displayedYear, displayedMonth, dayOfMonth))
                    }
                )
            }
        }
    }
}

@Composable
private fun WeekDaysStripRow(
    days: List<AcademicDay>,
    selectedDate: LocalDate,
    onSelectDay: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = day.date == selectedDate
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 14.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "dayCornerRadius"
            )
            val containerColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    day.events.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                label = "dayContainerColor"
            )
            val contentColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                day.events.isNotEmpty() -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            Surface(
                onClick = { onSelectDay(index) },
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .semantics {
                        stateDescription = if (isSelected) "Wybrany dzień" else "Dzień tygodnia"
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = day.shortDayLabel,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = day.dayOfMonthNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected || day.events.isNotEmpty()) FontWeight.Bold else FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (day.events.isNotEmpty()) {
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
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

@Composable
private fun InteractiveMonthCalendarGrid(
    displayedYear: Int,
    displayedMonth: Int,
    selectedDate: LocalDate,
    scheduledMonthDays: Set<Int>,
    onDayClicked: (Int) -> Unit
) {
    val yearMonth = YearMonth.of(displayedYear, displayedMonth)
    val daysInMonth = yearMonth.lengthOfMonth()
    // Monday = 1 .. Sunday = 7 -> 0..6 offset
    val firstDayOffset = yearMonth.atDay(1).dayOfWeek.value - 1

    val dayHeaders = listOf("Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Ndz")
    val cells = buildList<Int?> {
        repeat(firstDayOffset) { add(null) }
        for (d in 1..daysInMonth) add(d)
        while (size % 7 != 0) add(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { dayNum ->
                    if (dayNum == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        )
                    } else {
                        val isSelected = selectedDate.year == displayedYear &&
                            selectedDate.monthNumber == displayedMonth &&
                            selectedDate.dayOfMonth == dayNum
                        val hasClasses = dayNum in scheduledMonthDays

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(if (isSelected) M3XSquircleShape else CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        hasClasses -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                )
                                .clickable { onDayClicked(dayNum) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected || hasClasses) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        hasClasses -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (hasClasses) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
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

@Composable
private fun ScheduleFilterSegmentRow(
    selectedFilter: ScheduleFilterType,
    onSelectFilter: (ScheduleFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ScheduleFilterType.entries.forEach { filter ->
            val selected = filter == selectedFilter
            val cornerRadius by animateDpAsState(
                targetValue = if (selected) 28.dp else 14.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "filterSegmentCorner"
            )
            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                label = "filterSegmentColor"
            )
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (filter == ScheduleFilterType.MY_PLAN) {
                            WseiIcons.Person
                        } else {
                            WseiIcons.Groups
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = filter.polishLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveClassHeroCard(
    activeClass: ClassEvent,
    dayEvents: List<ClassEvent>,
    nextClassPreview: NextClassPreview?,
    nowEpochMillis: Long,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTriggerLiveNotification: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val elapsedMin = activeClass.elapsedMinutes(nowEpochMillis)
    val totalMin = activeClass.totalDurationMinutes
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val rawProgress = activeClass.progressFraction(nowEpochMillis)

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liveClassProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .semantics {
                stateDescription =
                    "Trwają zajęcia: ${activeClass.subject.name}, pozostało $remainingMin minut"
            },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (isExpanded) "Zwiń szczegóły zajęć" else "Rozwiń szczegóły zajęć",
                    onClick = onToggleExpand
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unified Home-style Header Row: 42.dp Squircle Icon + Overline + Title + Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = M3XSquircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = WseiIcons.forSubject(activeClass.subject.name, activeClass.classType),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "TRWA TERAZ • ${activeClass.classType.polishLabel.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activeClass.subject.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = WseiIcons.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "−$remainingMin min",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val activeLocationLabel = if (activeClass.deliveryMode == DeliveryMode.ON_SITE) {
                "${activeClass.room.displayLabel} • Stacjonarnie"
            } else {
                activeClass.deliveryMode.polishBadge
            }

            // Inner 16.dp progress box matching StartDashboardScreen style
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$activeLocationLabel • ${activeClass.lecturer.fullDisplayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$elapsedMin z $totalMin min • do ${activeClass.timeSlot.endFormatted}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .semantics {
                                contentDescription = "Postęp zajęć: $elapsedMin z $totalMin minut"
                            },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                    )
                }
            }

            // Segmented Day Progress Bar (`Zajęcia – Przerwa – Zajęcia`)
            SegmentedDayProgressBarCompose(
                dayEvents = dayEvents.ifEmpty { listOf(activeClass) },
                nowEpochMillis = nowEpochMillis
            )

            AnimatedVisibility(
                visible = isExpanded && nextClassPreview != null,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (nextClassPreview != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "NASTĘPNIE PO PRZERWIE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = nextClassPreview.summaryText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = WseiIcons.ExpandLess,
                                contentDescription = "Zwiń",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-width segmented day progress bar in Compose (`Zajęcia – Przerwa – Zajęcia – Przerwa – Zajęcia`).
 */
@Composable
fun SegmentedDayProgressBarCompose(
    dayEvents: List<ClassEvent>,
    nowEpochMillis: Long,
    modifier: Modifier = Modifier
) {
    val sorted = dayEvents.sortedBy { it.timeSlot.startEpochMillis }
    if (sorted.isEmpty()) return

    data class UiSegment(
        val label: String,
        val isBreak: Boolean,
        val startMs: Long,
        val endMs: Long,
        val weight: Float
    ) {
        val progress: Float
            get() {
                val total = (endMs - startMs).coerceAtLeast(1L)
                val elapsed = (nowEpochMillis - startMs).coerceIn(0L, total)
                return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            }
        val isOngoing: Boolean
            get() = nowEpochMillis in startMs until endMs
    }

    val segments = buildList {
        sorted.forEachIndexed { index, ev ->
            add(
                UiSegment(
                    label = "Zaj. ${index + 1}",
                    isBreak = false,
                    startMs = ev.timeSlot.startEpochMillis,
                    endMs = ev.timeSlot.endEpochMillis,
                    weight = 1.6f
                )
            )
            val next = sorted.getOrNull(index + 1)
            if (next != null && next.timeSlot.startEpochMillis > ev.timeSlot.endEpochMillis) {
                add(
                    UiSegment(
                        label = "Przerwa",
                        isBreak = true,
                        startMs = ev.timeSlot.endEpochMillis,
                        endMs = next.timeSlot.startEpochMillis,
                        weight = 0.8f
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Postęp całego dnia (Zajęcia • Przerwa • Zajęcia):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEach { seg ->
                Column(
                    modifier = Modifier.weight(seg.weight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { seg.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (seg.isBreak) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = seg.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (seg.isOngoing) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySectionHeader(
    selectedDay: AcademicDay?,
    modifier: Modifier = Modifier
) {
    if (selectedDay == null) return
    AnimatedContent(
        targetState = selectedDay,
        transitionSpec = {
            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) togetherWith
                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        },
        modifier = modifier.fillMaxWidth(),
        label = "daySectionHeaderTransition"
    ) { day ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${day.dayOfWeekLabel}, ${day.date}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            val countText = when (val count = day.events.size) {
                0 -> "Brak zajęć"
                1 -> "1 blok zajęć"
                in 2..4 -> "$count bloki zajęć"
                else -> "$count bloków zajęć"
            }
            Text(
                text = if (day.isWeekendExtramuralSession) "Zjazd • $countText" else countText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun LiveNowTimelineDivider(
    nowFormatted: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Text(
            text = nowFormatted,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun TimelineClassRow(
    event: ClassEvent,
    nowEpochMillis: Long,
    isOngoing: Boolean,
    isNextUp: Boolean,
    modifier: Modifier = Modifier
) {
    val isFinished = nowEpochMillis >= event.timeSlot.endEpochMillis

    val containerColor = when {
        isOngoing -> MaterialTheme.colorScheme.primaryContainer
        isNextUp -> MaterialTheme.colorScheme.surfaceContainerHigh
        isFinished -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.68f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isOngoing -> MaterialTheme.colorScheme.onPrimaryContainer
        isFinished -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 42.dp Subject Material Icon in M3X Squircle (matching Home Screen cards)
                Surface(
                    shape = M3XSquircleShape,
                    color = if (isOngoing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (isOngoing) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = WseiIcons.forSubject(event.subject.name, event.classType),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Center: Overline + Subject Title + Room/Lecturer Subtitle (matching Home Screen)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val statusPrefix = when {
                        isOngoing -> "TRWA • ${event.classType.polishLabel.uppercase()}"
                        isNextUp -> "NASTĘPNE • ${event.classType.polishLabel.uppercase()}"
                        isFinished -> "ZAKOŃCZONE • ${event.classType.polishLabel.uppercase()}"
                        else -> event.classType.polishLabel.uppercase()
                    }

                    Text(
                        text = "$statusPrefix • ${event.deliveryMode.polishBadge.uppercase()} • ${event.groupCode}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = event.subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${event.room.displayLabel} • ${event.lecturer.fullDisplayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.82f)
                    )
                }

                // Right: Time Slot Pill Badge (matching Home Screen)
                Surface(
                    shape = CircleShape,
                    color = if (isOngoing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (isOngoing) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ) {
                    Text(
                        text = "${event.timeSlot.startFormatted}–${event.timeSlot.endFormatted}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (isOngoing) {
                LinearWavyProgressIndicator(
                    progress = { event.progressFraction(nowEpochMillis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun EmptyDayNotice(
    dayLabel: String,
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = M3XSquircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = WseiIcons.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "HARMONOGRAM DNIA • WOLNE OD ZAJĘĆ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$dayLabel ($dateLabel) — brak zajęć",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "W Wirtualnym Dziekanacie nie zaplanowano wykładów ani laboratoriów na wybrany dzień. Rozwiń kalendarz powyżej, aby przejść do innych dni lub miesięcy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
