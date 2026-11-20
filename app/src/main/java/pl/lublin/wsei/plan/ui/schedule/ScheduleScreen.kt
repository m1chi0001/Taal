@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package pl.lublin.wsei.plan.ui.schedule

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDate
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.calendar.DeviceCalendar
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiClassCard
import pl.lublin.wsei.plan.ui.components.WseiEmptyState
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.classRowStateOf
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Plan zajęć (Material 3 Expressive):
 * - przełącznik „Mój plan / Plan grupy” jako połączona grupa przycisków,
 * - kalendarz tygodniowy rozwijany do pełnego miesiąca,
 * - karta trwających zajęć z falującym postępem i osią dnia,
 * - lista zajęć wybranego dnia.
 * Na szerokich ekranach i rozłożonych składakach układ dwupanelowy (kalendarz | lista dnia),
 * dopasowany do zawiasu.
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
    onLoadDeviceCalendars: () -> Unit = {},
    onSyncToCalendar: (calendarId: Long) -> Unit = {},
    onDismissCalendarSyncStatus: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onLogout: () -> Unit = {}
) {
    val windowLayout = LocalWseiWindowLayout.current
    val todayIso = remember(uiState.nowEpochMillis) { warsawTodayIso(uiState.nowEpochMillis) }
    val dayEvents = uiState.selectedDay?.events.orEmpty()
    // Lazy keys must be unique. Event ids are built from source data (id + date + start time),
    // so a source that lists the same block twice would crash LazyColumn on a duplicate key.
    val timelineRows = remember(dayEvents) { dayEvents.withUniqueKeys() }

    // ---- Calendar sync: permission → calendar picker → progress → result ----
    val context = LocalContext.current
    var isCalendarPickerVisible by rememberSaveable { mutableStateOf(false) }
    var calendarPermissionIssue by rememberSaveable { mutableStateOf<CalendarPermissionIssue?>(null) }
    val openCalendarPicker = {
        calendarPermissionIssue = null
        onLoadDeviceCalendars()
        isCalendarPickerVisible = true
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val denied = CalendarPermissions.filterNot { grants[it] == true || context.hasPermission(it) }
        if (denied.isEmpty()) {
            openCalendarPicker()
        } else {
            // Right after a denial the system only offers the rationale while it may still ask;
            // no rationale means "don't ask again" — the user has to go to the app settings.
            val activity = context.findActivity()
            val canAskAgain = activity != null &&
                denied.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            calendarPermissionIssue = if (canAskAgain) {
                CalendarPermissionIssue.DENIED
            } else {
                CalendarPermissionIssue.PERMANENTLY_DENIED
            }
        }
    }
    val requestCalendarSync: () -> Unit = {
        if (CalendarPermissions.all { context.hasPermission(it) }) {
            openCalendarPicker()
        } else {
            calendarPermissionLauncher.launch(CalendarPermissions)
        }
    }
    val syncStatus = uiState.calendarSyncStatus
    val isCalendarSyncBusy = syncStatus is CalendarSyncStatus.LoadingCalendars ||
        syncStatus is CalendarSyncStatus.Syncing

    CalendarSyncDialogs(
        status = syncStatus,
        calendars = uiState.deviceCalendars,
        lastSyncCalendarId = uiState.syncCalendarId,
        isLecturerHidden = uiState.isHideSensitiveData,
        isPickerVisible = isCalendarPickerVisible,
        permissionIssue = calendarPermissionIssue,
        onDismissPicker = { isCalendarPickerVisible = false },
        onConfirmCalendar = { calendarId ->
            isCalendarPickerVisible = false
            onSyncToCalendar(calendarId)
        },
        onDismissStatus = {
            isCalendarPickerVisible = false
            onDismissCalendarSyncStatus()
        },
        onDismissPermissionIssue = { calendarPermissionIssue = null },
        onRetryPermission = {
            calendarPermissionIssue = null
            calendarPermissionLauncher.launch(CalendarPermissions)
        },
        onOpenAppSettings = {
            calendarPermissionIssue = null
            context.openAppSettings()
        }
    )

    WseiScreenFrame(
        title = "Plan zajęć",
        modifier = modifier.fillMaxSize(),
        onRefresh = onRefreshSchedule,
        isRefreshing = uiState.isRefreshing
    ) { scrollModifier ->
        val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
            ScheduleControls(
                uiState = uiState,
                todayIso = todayIso,
                onSelectDay = onSelectDay,
                onSelectDate = onSelectDate,
                onToggleCalendarExpanded = onToggleCalendarExpanded,
                onPreviousCalendarPeriod = onPreviousCalendarPeriod,
                onNextCalendarPeriod = onNextCalendarPeriod,
                onSelectScheduleFilter = onSelectScheduleFilter,
                onToggleHeroExpanded = onToggleHeroExpanded,
                onSyncLiveNotification = onSyncLiveNotification,
                isCalendarSyncBusy = isCalendarSyncBusy,
                onRequestCalendarSync = requestCalendarSync,
                modifier = controlsModifier
            )
        }
        val contentPadding = wseiScreenContentPadding()

        if (windowLayout.supportsTwoPane) {
            // Calendar + live class in the first pane, the day's classes in the second. The
            // nested-scroll connection sits on the parent so both panes collapse the top bar,
            // and both scroll containers reach the pull-to-refresh box above them.
            val layoutDirection = LocalLayoutDirection.current
            val paneTop = contentPadding.calculateTopPadding()
            val paneBottom = contentPadding.calculateBottomPadding()
            WseiTwoPane(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
                    .padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection)
                    ),
                gap = WseiSpacing.xxl,
                first = {
                    controls(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = paneTop, bottom = paneBottom)
                    )
                },
                second = {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = paneTop, bottom = paneBottom),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
                    ) {
                        dayTimeline(uiState, timelineRows)
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
            ) {
                item(key = "schedule_controls", contentType = "schedule_controls") {
                    controls(if (windowLayout.isCompact) Modifier else Modifier.wseiReadableWidth())
                }
                dayTimeline(uiState, timelineRows, readableWidth = !windowLayout.isCompact)
            }
        }
    }
}

/** A class row with a key that is unique within the rendered day. */
@Immutable
private class TimelineRow(val key: String, val event: ClassEvent)

private fun List<ClassEvent>.withUniqueKeys(): List<TimelineRow> {
    val seen = HashMap<String, Int>(size)
    return map { event ->
        val occurrence = (seen[event.id] ?: 0) + 1
        seen[event.id] = occurrence
        TimelineRow(if (occurrence == 1) event.id else "${event.id}#$occurrence", event)
    }
}

private fun warsawTodayIso(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(WarsawZone).toLocalDate().toString()

private val WarsawZone: ZoneId = ZoneId.of("Europe/Warsaw")

@Composable
private fun ScheduleControls(
    uiState: ScheduleUiState,
    todayIso: String,
    onSelectDay: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToggleCalendarExpanded: () -> Unit,
    onPreviousCalendarPeriod: () -> Unit,
    onNextCalendarPeriod: () -> Unit,
    onSelectScheduleFilter: (ScheduleFilterType) -> Unit,
    onToggleHeroExpanded: () -> Unit,
    onSyncLiveNotification: (ClassEvent) -> Unit,
    isCalendarSyncBusy: Boolean,
    onRequestCalendarSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.betweenCards)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            ScheduleFilterButtonGroup(
                selectedFilter = uiState.scheduleFilterType,
                onSelectFilter = onSelectScheduleFilter,
                modifier = Modifier.weight(1f)
            )
            CalendarSyncButton(isBusy = isCalendarSyncBusy, onClick = onRequestCalendarSync)
        }
        ExpandableCalendarCard(
            daysOfCurrentWeek = uiState.days,
            selectedDate = uiState.selectedDate,
            displayedYear = uiState.displayedYear,
            displayedMonth = uiState.displayedMonth,
            scheduledMonthDays = uiState.scheduledMonthDays,
            isExpanded = uiState.isCalendarExpanded,
            todayIso = todayIso,
            onToggleExpanded = onToggleCalendarExpanded,
            onPreviousPeriod = onPreviousCalendarPeriod,
            onNextPeriod = onNextCalendarPeriod,
            onSelectDayInWeek = onSelectDay,
            onSelectDate = onSelectDate
        )
        val activeClass = uiState.activeClass
        // The last live class stays rendered while the card animates out when the class ends
        // (a plain holder, not state — writing it during composition causes no extra pass).
        val lastActiveHolder = remember { arrayOfNulls<ClassEvent>(1) }
        if (activeClass != null) lastActiveHolder[0] = activeClass
        AnimatedVisibility(
            visible = activeClass != null,
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()) +
                scaleIn(WseiMotion.spatial(), initialScale = 0.94f),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
        ) {
            val shown = activeClass ?: lastActiveHolder[0]
            if (shown != null) {
                ActiveClassHeroCard(
                    activeClass = shown,
                    dayEvents = uiState.activeDayEvents,
                    nextClassPreview = uiState.nextClassPreview,
                    nowEpochMillis = uiState.nowEpochMillis,
                    isExpanded = uiState.isHeroExpanded,
                    onToggleExpand = onToggleHeroExpanded,
                    lecturerLabel = uiState.maskLecturer(shown.lecturer.displayNameOrPlaceholder),
                    onTriggerLiveNotification = { onSyncLiveNotification(shown) }
                )
            }
        }
    }
}

/** Tonal icon action for the device-calendar export, with a long-press tooltip. */
@Composable
private fun CalendarSyncButton(isBusy: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.calendar_sync_action)
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = !isBusy,
            shape = M3XSquircleShape,
            modifier = Modifier
                .size(52.dp)
                .semantics { contentDescription = label }
        ) {
            AnimatedContent(
                targetState = isBusy,
                transitionSpec = {
                    (fadeIn(WseiMotion.effects()) + scaleIn(WseiMotion.fastSpatial(), initialScale = 0.6f)) togetherWith
                        fadeOut(WseiMotion.fastEffects())
                },
                label = "calendarSyncBusy"
            ) { busy ->
                if (busy) {
                    LoadingIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = WseiIcons.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** Nagłówek dnia i karty zajęć — wspólne dla układu jedno- i dwupanelowego. */
private fun LazyListScope.dayTimeline(
    uiState: ScheduleUiState,
    rows: List<TimelineRow>,
    readableWidth: Boolean = false
) {
    val itemModifier = if (readableWidth) Modifier.wseiReadableWidth() else Modifier
    val selectedDay = uiState.selectedDay
    item(key = "day_header", contentType = "day_header") {
        DaySectionHeader(selectedDay = selectedDay, modifier = itemModifier)
    }
    if (rows.isEmpty()) {
        item(key = "empty_day", contentType = "empty_day") {
            WseiEmptyState(
                icon = WseiIcons.CalendarMonth,
                title = "Wolne od zajęć",
                message = "Na ten dzień nie ma zajęć. Wybierz inny dzień w kalendarzu.",
                modifier = itemModifier
            )
        }
    } else {
        val now = uiState.nowEpochMillis
        val activeId = uiState.activeClass?.id
        val nextId = uiState.nextClassPreview?.nextEvent?.id
        items(
            items = rows,
            key = { it.key },
            contentType = { "timeline_class_row" }
        ) { row ->
            WseiClassCard(
                event = row.event,
                nowEpochMillis = now,
                state = classRowStateOf(row.event, now, activeId, nextId),
                lecturerLabel = uiState.maskLecturer(row.event.lecturer.displayNameOrPlaceholder),
                modifier = itemModifier.animateItem(
                    fadeInSpec = WseiMotion.effects(),
                    placementSpec = WseiMotion.spatial(),
                    // Day switches replace every key; dropping old rows at once avoids two days overlapping.
                    fadeOutSpec = null
                )
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
    // M3 Expressive connected button group: the checked segment morphs into a full pill and
    // the pressed one squeezes its inner corners — shape motion comes from ToggleButton itself.
    val colors = ToggleButtonDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        checkedContainerColor = MaterialTheme.colorScheme.primary,
        checkedContentColor = MaterialTheme.colorScheme.onPrimary
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        filters.forEachIndexed { index, filter ->
            val selected = filter == selectedFilter
            ToggleButton(
                checked = selected,
                onCheckedChange = { onSelectFilter(filter) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    filters.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = colors,
                icon = {
                    Icon(
                        imageVector = if (filter == ScheduleFilterType.MY_PLAN) WseiIcons.Person else WseiIcons.Groups,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .semantics { role = Role.RadioButton }
            ) {
                Text(
                    text = filter.polishLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    todayIso: String,
    onToggleExpanded: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSelectDayInWeek: (Int) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekStart = daysOfCurrentWeek.firstOrNull()?.date
    val weekEnd = daysOfCurrentWeek.lastOrNull()?.date
    val subtitle = when {
        isExpanded -> "Widok miesiąca"
        weekStart != null && weekEnd != null -> "${weekStart.dayOfMonth}.${weekStart.monthNumber.twoDigits()} – " +
            "${weekEnd.dayOfMonth}.${weekEnd.monthNumber.twoDigits()}"
        else -> ""
    }

    // No animateContentSize here: the week/month AnimatedVisibility pair already animates the
    // height, and a second size animation on the card made the swap stutter.
    WseiCard(
        modifier = modifier,
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
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
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
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
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
    // Locale.ROOT: with a non-Latin-digit default locale the prefix never matched the ISO date,
    // so "today" lost its outline in the month grid.
    val todayDay = remember(displayedYear, displayedMonth, todayIso) {
        val todayPrefix = String.format(Locale.ROOT, "%04d-%02d-", displayedYear, displayedMonth)
        if (todayIso.startsWith(todayPrefix)) todayIso.takeLast(2).toIntOrNull() else null
    }

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

/** Hero content switches to two columns once the card is at least this wide (medium+ windows). */
private val HeroTwoColumnMinWidth = 560.dp

/**
 * „Trwa teraz” — karta trwających zajęć (M3 Expressive):
 * status, przedmiot i forma, przedział godzin, sala i prowadzący (maskowany przy „Ukryj dane”),
 * falujący postęp z liczbą minut do końca, a po rozwinięciu oś dnia i zapowiedź następnych zajęć.
 * Na szerokich oknach treść układa się w dwie kolumny zamiast rozciągać się na całą szerokość.
 *
 * @param lecturerLabel lecturer as it may be shown — pass `uiState.maskLecturer(...)`.
 */
@Composable
fun ActiveClassHeroCard(
    activeClass: ClassEvent,
    dayEvents: List<ClassEvent>,
    nextClassPreview: NextClassPreview?,
    nowEpochMillis: Long,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    lecturerLabel: String = activeClass.lecturer.displayNameOrPlaceholder,
    @Suppress("UNUSED_PARAMETER") onTriggerLiveNotification: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val stateText = stringResource(R.string.schedule_hero_state, activeClass.subject.name, remainingMin)

    WseiCard(
        onClick = onToggleExpand,
        shape = ActiveHeroCardAsymmetricShape,
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        contentPadding = PaddingValues(WseiSpacing.xl),
        verticalSpacing = WseiSpacing.l,
        modifier = modifier.semantics { stateDescription = stateText }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= HeroTwoColumnMinWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xxl)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
                    ) {
                        HeroHeader(activeClass = activeClass, isExpanded = isExpanded)
                        HeroDetails(activeClass = activeClass, lecturerLabel = lecturerLabel)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
                    ) {
                        HeroProgress(activeClass = activeClass, nowEpochMillis = nowEpochMillis)
                        HeroExpandedContent(
                            isExpanded = isExpanded,
                            activeClass = activeClass,
                            dayEvents = dayEvents,
                            nextClassPreview = nextClassPreview,
                            nowEpochMillis = nowEpochMillis
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)) {
                    HeroHeader(activeClass = activeClass, isExpanded = isExpanded)
                    HeroProgress(activeClass = activeClass, nowEpochMillis = nowEpochMillis)
                    HeroDetails(activeClass = activeClass, lecturerLabel = lecturerLabel)
                    HeroExpandedContent(
                        isExpanded = isExpanded,
                        activeClass = activeClass,
                        dayEvents = dayEvents,
                        nextClassPreview = nextClassPreview,
                        nowEpochMillis = nowEpochMillis
                    )
                }
            }
        }
    }
}

/** Status chip, time range, expand affordance, then the accent badge with type and subject. */
@Composable
private fun HeroHeader(activeClass: ClassEvent, isExpanded: Boolean) {
    val colors = MaterialTheme.colorScheme
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = WseiMotion.fastSpatial(),
        label = "heroChevron"
    )
    val toggleLabel = stringResource(
        if (isExpanded) R.string.schedule_hero_collapse_cd else R.string.schedule_hero_expand_cd
    )
    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                LiveStatusChip()
                WseiPill(
                    text = stringResource(
                        R.string.schedule_hero_time_range,
                        activeClass.timeSlot.startFormatted,
                        activeClass.timeSlot.endFormatted
                    ),
                    icon = WseiIcons.Schedule,
                    containerColor = colors.onPrimaryContainer.copy(alpha = 0.1f),
                    contentColor = colors.onPrimaryContainer,
                    emphasized = true,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.onPrimaryContainer.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WseiIcons.ExpandMore,
                    // Read as part of the (clickable) card: tells what a tap does.
                    contentDescription = toggleLabel,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WseiIcons.forSubject(activeClass.subject.name, activeClass.classType),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = activeClass.classType.polishLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activeClass.subject.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
    }
}

/** „Trwa teraz” status chip with a sunny-shaped live marker. */
@Composable
private fun LiveStatusChip() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = colors.primary,
        contentColor = colors.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(MaterialShapes.Sunny.toShape())
                    .background(colors.onPrimary)
            )
            Text(
                text = stringResource(R.string.schedule_hero_status_live),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Remaining minutes as the key figure, wavy progress and elapsed/total underneath. */
@Composable
private fun HeroProgress(activeClass: ClassEvent, nowEpochMillis: Long) {
    val colors = MaterialTheme.colorScheme
    val elapsedMin = activeClass.elapsedMinutes(nowEpochMillis)
    val totalMin = activeClass.totalDurationMinutes
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val animatedProgress by animateFloatAsState(
        targetValue = activeClass.progressFraction(nowEpochMillis),
        animationSpec = WseiMotion.size(),
        label = "liveClassProgress"
    )
    val progressDescription = stringResource(R.string.schedule_hero_progress_cd, elapsedMin, totalMin)
    val mutedColor = colors.onPrimaryContainer.copy(alpha = 0.78f)

    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = remainingMin,
                    transitionSpec = {
                        (slideInVertically(WseiMotion.spatial()) { it / 2 } + fadeIn(WseiMotion.effects())) togetherWith
                            (slideOutVertically(WseiMotion.spatial()) { -it / 2 } + fadeOut(WseiMotion.fastEffects())) using
                            SizeTransform(clip = false)
                    },
                    label = "heroRemainingMinutes"
                ) { minutes ->
                    Text(
                        text = stringResource(R.string.schedule_hero_remaining_value, minutes),
                        style = MaterialTheme.typography.displaySmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(R.string.schedule_hero_remaining_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = mutedColor,
                    maxLines = 1
                )
            }
            WseiPill(
                text = stringResource(R.string.schedule_hero_ends_at, activeClass.timeSlot.endFormatted),
                icon = WseiIcons.Schedule,
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                emphasized = true,
                modifier = Modifier.padding(bottom = WseiSpacing.xxs)
            )
        }
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .semantics { contentDescription = progressDescription },
            color = colors.primary,
            trackColor = colors.onPrimaryContainer.copy(alpha = 0.16f)
        )
        Text(
            text = stringResource(R.string.schedule_hero_elapsed, elapsedMin, totalMin),
            style = MaterialTheme.typography.labelMedium,
            color = mutedColor
        )
    }
}

/** Room / delivery mode, lecturer (already masked by the caller) and group. */
@Composable
private fun HeroDetails(activeClass: ClassEvent, lecturerLabel: String) {
    val colors = MaterialTheme.colorScheme
    val chipColor = colors.onPrimaryContainer.copy(alpha = 0.1f)
    val location = if (activeClass.deliveryMode == DeliveryMode.ON_SITE) {
        activeClass.room.displayLabel
    } else {
        activeClass.deliveryMode.polishBadge
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        WseiPill(
            text = location,
            icon = WseiIcons.LocationOn,
            containerColor = chipColor,
            contentColor = colors.onPrimaryContainer
        )
        WseiPill(
            text = lecturerLabel,
            icon = WseiIcons.Person,
            containerColor = chipColor,
            contentColor = colors.onPrimaryContainer
        )
        if (activeClass.groupCode.isNotBlank()) {
            WseiPill(
                text = activeClass.groupCode,
                icon = WseiIcons.Groups,
                containerColor = chipColor,
                contentColor = colors.onPrimaryContainer
            )
        }
    }
}

/** Day timeline and the next class — revealed with a single size animation. */
@Composable
private fun HeroExpandedContent(
    isExpanded: Boolean,
    activeClass: ClassEvent,
    dayEvents: List<ClassEvent>,
    nextClassPreview: NextClassPreview?,
    nowEpochMillis: Long
) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
        exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
            SegmentedDayProgressBarCompose(
                dayEvents = dayEvents.ifEmpty { listOf(activeClass) },
                nowEpochMillis = nowEpochMillis
            )
            if (nextClassPreview != null) {
                NextClassPreviewCard(preview = nextClassPreview)
            }
        }
    }
}

@Composable
private fun NextClassPreviewCard(preview: NextClassPreview) {
    val colors = MaterialTheme.colorScheme
    val next = preview.nextEvent
    val location = if (next.deliveryMode == DeliveryMode.ON_SITE) next.room.displayLabel else next.deliveryMode.polishBadge
    val breakLabel = if (preview.breakDurationMinutes > 0) {
        stringResource(R.string.schedule_hero_next_break, preview.breakDurationMinutes)
    } else {
        stringResource(R.string.schedule_hero_next_no_break)
    }
    Surface(
        shape = WseiInsetShape,
        color = colors.surfaceContainerLowest.copy(alpha = 0.55f),
        contentColor = colors.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = WseiSpacing.l, vertical = WseiSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Clover4Leaf.toShape())
                    .background(colors.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = WseiIcons.ArrowForward,
                    contentDescription = null,
                    tint = colors.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.schedule_hero_next_title, next.timeSlot.startFormatted),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.tertiary
                )
                Text(
                    text = next.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$breakLabel • $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

// -----------------------------------------------------------------------------------------
// Synchronizacja z kalendarzem urządzenia
// -----------------------------------------------------------------------------------------

private val CalendarPermissions = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR
)

/** Why calendar access is missing after the system permission dialog. */
private enum class CalendarPermissionIssue { DENIED, PERMANENTLY_DENIED }

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

/**
 * All dialogs of the calendar export flow. The ViewModel status wins over the picker:
 * while syncing a progress dialog is shown, then the result (counts or error) until dismissed.
 */
@Composable
private fun CalendarSyncDialogs(
    status: CalendarSyncStatus,
    calendars: List<DeviceCalendar>,
    lastSyncCalendarId: Long?,
    isLecturerHidden: Boolean,
    isPickerVisible: Boolean,
    permissionIssue: CalendarPermissionIssue?,
    onDismissPicker: () -> Unit,
    onConfirmCalendar: (Long) -> Unit,
    onDismissStatus: () -> Unit,
    onDismissPermissionIssue: () -> Unit,
    onRetryPermission: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    when {
        permissionIssue != null -> CalendarPermissionDialog(
            issue = permissionIssue,
            onDismiss = onDismissPermissionIssue,
            onRetry = onRetryPermission,
            onOpenSettings = onOpenAppSettings
        )
        status is CalendarSyncStatus.Syncing -> CalendarSyncProgressDialog()
        status is CalendarSyncStatus.Done -> CalendarSyncResultDialog(
            icon = WseiIcons.CheckCircle,
            title = stringResource(R.string.calendar_sync_done_title),
            message = with(status.result) {
                if (inserted == 0 && updated == 0 && deleted == 0) {
                    stringResource(R.string.calendar_sync_done_no_changes)
                } else {
                    stringResource(R.string.calendar_sync_done_message, inserted, updated, deleted)
                }
            },
            onDismiss = onDismissStatus
        )
        status is CalendarSyncStatus.Failed -> CalendarSyncResultDialog(
            icon = WseiIcons.Info,
            title = stringResource(R.string.calendar_sync_failed_title),
            message = status.message?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.calendar_sync_failed_generic),
            onDismiss = onDismissStatus
        )
        isPickerVisible -> CalendarPickerDialog(
            isLoading = status is CalendarSyncStatus.LoadingCalendars,
            calendars = calendars,
            lastSyncCalendarId = lastSyncCalendarId,
            isLecturerHidden = isLecturerHidden,
            onDismiss = onDismissPicker,
            onConfirm = onConfirmCalendar
        )
    }
}

@Composable
private fun CalendarPickerDialog(
    isLoading: Boolean,
    calendars: List<DeviceCalendar>,
    lastSyncCalendarId: Long?,
    isLecturerHidden: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var chosenId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Explicit choice → calendar used last time → the primary account calendar → the first one.
    val selectedId = remember(chosenId, calendars, lastSyncCalendarId) {
        chosenId?.takeIf { id -> calendars.any { it.id == id } }
            ?: lastSyncCalendarId?.takeIf { id -> calendars.any { it.id == id } }
            ?: calendars.firstOrNull { it.isPrimary }?.id
            ?: calendars.firstOrNull()?.id
    }
    val canConfirm = !isLoading && selectedId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = WseiIcons.CalendarMonth, contentDescription = null) },
        title = { Text(text = stringResource(R.string.calendar_sync_picker_title)) },
        text = {
            AnimatedContent(
                targetState = when {
                    isLoading -> PickerContent.LOADING
                    calendars.isEmpty() -> PickerContent.EMPTY
                    else -> PickerContent.LIST
                },
                transitionSpec = {
                    fadeIn(WseiMotion.effects()) togetherWith fadeOut(WseiMotion.fastEffects()) using
                        SizeTransform(clip = false) { _, _ -> WseiMotion.size() }
                },
                label = "calendarPickerContent"
            ) { content ->
                when (content) {
                    PickerContent.LOADING -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = WseiSpacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
                    ) {
                        LoadingIndicator(modifier = Modifier.size(40.dp))
                        Text(
                            text = stringResource(R.string.calendar_sync_loading),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    PickerContent.EMPTY -> Text(
                        text = stringResource(R.string.calendar_sync_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PickerContent.LIST -> Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
                        Text(
                            text = stringResource(R.string.calendar_sync_picker_message),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isLecturerHidden) {
                            Text(
                                text = stringResource(R.string.calendar_sync_hidden_lecturer_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
                        ) {
                            calendars.forEachIndexed { index, calendar ->
                                CalendarOptionRow(
                                    calendar = calendar,
                                    selected = calendar.id == selectedId,
                                    shape = wseiGroupItemShape(index, calendars.size),
                                    onSelect = { chosenId = calendar.id }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let(onConfirm) },
                enabled = canConfirm
            ) {
                Text(text = stringResource(R.string.calendar_sync_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.calendar_sync_cancel))
            }
        }
    )
}

private enum class PickerContent { LOADING, EMPTY, LIST }

@Composable
private fun CalendarOptionRow(
    calendar: DeviceCalendar,
    selected: Boolean,
    shape: Shape,
    onSelect: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
        animationSpec = WseiMotion.effects(),
        label = "calendarOptionContainer"
    )
    // The calendar's own colour (from the provider); falls back to the theme accent.
    val dotColor = calendar.colorArgb?.let { Color(it) } ?: colors.primary
    Surface(
        selected = selected,
        onClick = onSelect,
        shape = shape,
        color = containerColor,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
    ) {
        Row(
            modifier = Modifier.padding(start = WseiSpacing.xs, end = WseiSpacing.m, top = WseiSpacing.xs, bottom = WseiSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(WseiSpacing.s))
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = calendar.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (calendar.accountName.isNotBlank() && calendar.accountName != calendar.displayName) {
                    Text(
                        text = calendar.accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (calendar.isPrimary) {
                WseiPill(
                    text = stringResource(R.string.calendar_sync_primary),
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun CalendarSyncProgressDialog() {
    AlertDialog(
        // Syncing is short and must not be interrupted half-way; the result dialog follows.
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
            ) {
                LoadingIndicator(modifier = Modifier.size(48.dp))
                Text(
                    text = stringResource(R.string.calendar_sync_in_progress),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )
}

@Composable
private fun CalendarSyncResultDialog(
    icon: ImageVector,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.calendar_sync_close))
            }
        }
    )
}

@Composable
private fun CalendarPermissionDialog(
    issue: CalendarPermissionIssue,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val permanentlyDenied = issue == CalendarPermissionIssue.PERMANENTLY_DENIED
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = WseiIcons.CalendarMonth, contentDescription = null) },
        title = { Text(text = stringResource(R.string.calendar_permission_title)) },
        text = {
            Text(
                text = stringResource(
                    if (permanentlyDenied) {
                        R.string.calendar_permission_permanently_denied
                    } else {
                        R.string.calendar_permission_denied
                    }
                )
            )
        },
        confirmButton = {
            TextButton(onClick = if (permanentlyDenied) onOpenSettings else onRetry) {
                Text(
                    text = stringResource(
                        if (permanentlyDenied) R.string.calendar_permission_open_settings else R.string.calendar_permission_retry
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.calendar_sync_cancel))
            }
        }
    )
}
