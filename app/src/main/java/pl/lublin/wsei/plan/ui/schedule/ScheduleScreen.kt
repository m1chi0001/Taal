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
import androidx.compose.animation.EnterTransition
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.unit.Dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.launch
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.domain.calendar.DeviceCalendar
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocus
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusKind
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
import pl.lublin.wsei.plan.domain.schedule.NextClassDay
import pl.lublin.wsei.plan.ui.adaptive.LocalWseiWindowLayout
import pl.lublin.wsei.plan.ui.adaptive.WseiReadableContentMaxWidth
import pl.lublin.wsei.plan.ui.adaptive.WseiTwoPane
import pl.lublin.wsei.plan.ui.adaptive.wseiReadableWidth
import pl.lublin.wsei.plan.ui.adaptive.wseiScreenContentPadding
import pl.lublin.wsei.plan.ui.components.ClassRowState
import pl.lublin.wsei.plan.ui.components.WseiCard
import pl.lublin.wsei.plan.ui.components.WseiClassCard
import pl.lublin.wsei.plan.ui.components.WseiIconBadge
import pl.lublin.wsei.plan.ui.components.WseiPill
import pl.lublin.wsei.plan.ui.components.WseiScreenFrame
import pl.lublin.wsei.plan.ui.components.classRowStateOf
import pl.lublin.wsei.plan.ui.components.displayGroupCode
import pl.lublin.wsei.plan.ui.components.shouldHighlightAsNextClass
import pl.lublin.wsei.plan.ui.components.wseiPressScale
import pl.lublin.wsei.plan.ui.icons.WseiIcons
import pl.lublin.wsei.plan.ui.theme.ActiveHeroCardAsymmetricShape
import pl.lublin.wsei.plan.ui.theme.M3XCookieShape
import pl.lublin.wsei.plan.ui.theme.M3XSquircleShape
import pl.lublin.wsei.plan.ui.theme.WseiCardShape
import pl.lublin.wsei.plan.ui.theme.WseiCorners
import pl.lublin.wsei.plan.ui.theme.WseiInsetShape
import pl.lublin.wsei.plan.ui.theme.WseiMotion
import pl.lublin.wsei.plan.ui.theme.WseiSpacing
import pl.lublin.wsei.plan.ui.theme.mutedContentColor
import pl.lublin.wsei.plan.ui.theme.tonalLayer
import pl.lublin.wsei.plan.ui.theme.withContrastAgainst
import pl.lublin.wsei.plan.ui.theme.wseiGroupItemShape
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import pl.lublin.wsei.plan.l10n.appLocale
import pl.lublin.wsei.plan.l10n.capitalizeFirst
import pl.lublin.wsei.plan.l10n.fullNameRes
import pl.lublin.wsei.plan.l10n.labelRes
import pl.lublin.wsei.plan.l10n.localizedLabel
import pl.lublin.wsei.plan.l10n.localizedNameOrPlaceholder
import pl.lublin.wsei.plan.l10n.rememberLocalizedDateFormatter
import pl.lublin.wsei.plan.l10n.shortNameRes

/**
 * Plan zajęć (Material 3 Expressive):
 * - "Mój plan / Plan grupy" switch as a connected button group,
 * - week calendar that expands to the full month,
 * - "Trwa teraz" hero with a countdown and wavy progress,
 * - a short notice when the plan opens on tomorrow / a later zjazd day / the next zjazd,
 * - the selected day's classes,
 * - export to the device calendar in one sheet (pick → progress → result).
 * Wide windows and unfolded foldables use two panes (calendar | day list), snapped to the hinge
 * and capped at [ScheduleTwoPaneMaxWidth]; the title bar is aligned with the content edge.
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
        // The sheet stays open: it shows the progress and then the result itself.
        onConfirmCalendar = { calendarId -> onSyncToCalendar(calendarId) },
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
        title = stringResource(R.string.nav_schedule),
        modifier = modifier.fillMaxSize(),
        onRefresh = onRefreshSchedule,
        isRefreshing = uiState.isRefreshing,
        contentMaxWidth = if (windowLayout.supportsTwoPane) ScheduleTwoPaneMaxWidth else WseiReadableContentMaxWidth
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
            val start = contentPadding.calculateStartPadding(layoutDirection)
            val end = contentPadding.calculateEndPadding(layoutDirection)
            // Both panes share one top edge and one centred, capped column, so the controls row,
            // the calendar card and the day list line up with each other and with the title.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier),
                contentAlignment = Alignment.TopCenter
            ) {
                WseiTwoPane(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = ScheduleTwoPaneMaxWidth + start + end)
                        .fillMaxWidth()
                        .padding(start = start, end = end),
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
                            dayTimeline(uiState, timelineRows, todayIso, onSelectDate, headerTopPadding = 0.dp)
                        }
                    }
                )
            }
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
                dayTimeline(uiState, timelineRows, todayIso, onSelectDate, readableWidth = !windowLayout.isCompact)
            }
        }
    }
}

/** Width cap of the two-pane layout (calendar | day list) on very wide windows. */
private val ScheduleTwoPaneMaxWidth: Dp = 1200.dp

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
    // No spacedBy on this column: the hero's gap lives inside its AnimatedVisibility, so the
    // 12 dp spacing shrinks together with the card instead of popping when the class ends.
    Column(modifier = modifier) {
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
            onSelectDate = onSelectDate,
            modifier = Modifier.padding(top = WseiSpacing.betweenCards)
        )
        val activeClass = uiState.activeClass
        // The last live class stays rendered while the card animates out when the class ends
        // (a plain holder, not state — writing it during composition causes no extra pass).
        val lastActiveHolder = remember { arrayOfNulls<ClassEvent>(1) }
        if (activeClass != null) lastActiveHolder[0] = activeClass
        AnimatedVisibility(
            visible = activeClass != null,
            enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()) +
                scaleIn(WseiMotion.spatial(), initialScale = 0.96f),
            exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
        ) {
            val shown = activeClass ?: lastActiveHolder[0]
            if (shown != null) {
                ActiveClassHeroCard(
                    modifier = Modifier.padding(top = WseiSpacing.betweenCards),
                    activeClass = shown,
                    nextClassPreview = uiState.nextClassPreview,
                    nowEpochMillis = uiState.nowEpochMillis,
                    isExpanded = uiState.isHeroExpanded,
                    onToggleExpand = onToggleHeroExpanded,
                    lecturerLabel = uiState.maskLecturer(shown.lecturer.localizedNameOrPlaceholder()),
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

/**
 * Day header and class cards — shared by the one- and two-pane layouts.
 *
 * @param headerTopPadding air above the day header; 0 in the second pane, so the header starts
 *   on the same line as the controls row of the first pane.
 */
private fun LazyListScope.dayTimeline(
    uiState: ScheduleUiState,
    rows: List<TimelineRow>,
    todayIso: String,
    onSelectDate: (LocalDate) -> Unit,
    readableWidth: Boolean = false,
    headerTopPadding: Dp = WseiSpacing.s
) {
    val itemModifier = if (readableWidth) Modifier.wseiReadableWidth() else Modifier
    val selectedDay = uiState.selectedDay
    val focus = uiState.scheduleFocus
    // Explains why Plan zajęć opened on another day: only while that focus day is the one shown.
    // The day header right below already names the date, so the notice only says what happened
    // today and what kind of day is shown — never the date again.
    if (!focus.isToday && focus.kind != ScheduleFocusKind.NONE &&
        focus.dateIso != null && focus.dateIso == selectedDay?.date?.toString()
    ) {
        item(key = "focus_notice", contentType = "focus_notice") {
            val todayHadClasses = remember(uiState.allEvents, todayIso) {
                uiState.allEvents.any { it.dateIso == todayIso }
            }
            ScheduleFocusNotice(
                focus = focus,
                todayIso = todayIso,
                todayHadClasses = todayHadClasses,
                modifier = itemModifier.animateItem(
                    fadeInSpec = WseiMotion.effects(),
                    placementSpec = WseiMotion.spatial(),
                    fadeOutSpec = WseiMotion.fastEffects()
                )
            )
        }
    }
    item(key = "day_header", contentType = "day_header") {
        DaySectionHeader(selectedDay = selectedDay, topPadding = headerTopPadding, modifier = itemModifier)
    }
    if (rows.isEmpty()) {
        item(key = "empty_day", contentType = "empty_day") {
            val viewedIso = (selectedDay?.date ?: uiState.selectedDate).toString()
            // Always point to the nearest upcoming class day from TODAY (not after viewedIso), so
            // picking a free day after the upcoming zjazd still offers jumping to that nearest zjazd.
            val nextDay = remember(uiState.allEvents, todayIso, uiState.scheduleFocus.dateIso) {
                ScheduleFocusResolver.nearestClassDayFromNow(uiState.allEvents, uiState.nowEpochMillis)
            }
            DayOffCard(
                model = DayOffModel(viewedIso, nextDay),
                onGoTo = { iso -> runCatching { LocalDate.parse(iso) }.getOrNull()?.let(onSelectDate) },
                modifier = itemModifier.animateItem(
                    fadeInSpec = WseiMotion.effects(),
                    placementSpec = WseiMotion.spatial(),
                    // Like the class rows: leaves at once when the next day's rows come in.
                    fadeOutSpec = null
                )
            )
        }
    } else {
        val now = uiState.nowEpochMillis
        val activeId = uiState.activeClass?.id
        val nextId = uiState.nextClassPreview?.nextEvent
            ?.takeIf { shouldHighlightAsNextClass(it, now, todayIso) }
            ?.id
            ?: rows.firstOrNull { shouldHighlightAsNextClass(it.event, now, todayIso) }?.event?.id
        itemsIndexed(
            items = rows,
            key = { _, row -> row.key },
            contentType = { _, _ -> "timeline_class_row" }
        ) { index, row ->
            val rowState = classRowStateOf(row.event, now, activeId, nextId)
            WseiClassCard(
                event = row.event,
                nowEpochMillis = now,
                state = rowState,
                shape = if (rowState == ClassRowState.ONGOING) {
                    WseiCardShape
                } else {
                    wseiGroupItemShape(index, rows.size, outer = WseiCorners.card, inner = 14.dp)
                },
                lecturerLabel = uiState.maskLecturer(row.event.lecturer.localizedNameOrPlaceholder()),
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
// Plan switch — connected button group (M3 Expressive "connected button group")
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
        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    text = stringResource(filter.labelRes),
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
// Calendar: week ⇄ month
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
    val monthTitle = stringResource(
        R.string.schedule_calendar_month_year,
        stringResource(monthNameRes(displayedMonth)),
        displayedYear
    )
    val subtitle = when {
        isExpanded -> stringResource(R.string.schedule_calendar_month_view)
        weekStart != null && weekEnd != null -> "${weekStart.dayOfMonth}.${weekStart.monthNumber.twoDigits()} – " +
            "${weekEnd.dayOfMonth}.${weekEnd.monthNumber.twoDigits()}"
        else -> ""
    }

    // No animateContentSize here: the AnimatedContent below is the only size animation.
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
                    targetState = monthTitle,
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
            FilledTonalIconButton(
                onClick = onPreviousPeriod,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = WseiIcons.ChevronLeft,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.schedule_calendar_previous_month else R.string.schedule_calendar_previous_week
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
            FilledTonalIconButton(
                onClick = onNextPeriod,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = WseiIcons.ChevronRight,
                    contentDescription = stringResource(
                        if (isExpanded) R.string.schedule_calendar_next_month else R.string.schedule_calendar_next_week
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
            val toggleRotation = animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = WseiMotion.fastSpatial(),
                label = "calendarToggleChevron"
            )
            FilledTonalButton(
                onClick = onToggleExpanded,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                // A fixed minimum width keeps the title column from resizing when the label flips.
                modifier = Modifier
                    .height(40.dp)
                    .widthIn(min = 104.dp)
            ) {
                Text(
                    text = stringResource(
                        if (isExpanded) R.string.schedule_calendar_show_week else R.string.schedule_calendar_show_month
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    imageVector = WseiIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(18.dp)
                        .graphicsLayer { rotationZ = toggleRotation.value }
                )
            }
        }

        // One size-animation source for the week ⇄ month swap (and month ⇄ month paging): the
        // outgoing and incoming views overlap inside a single AnimatedContent whose SizeTransform
        // springs the height. Two sibling AnimatedVisibility blocks (shrink + expand) used to sum
        // their heights and the column spacing popped in/out, which made the card jump.
        val viewKey = CalendarViewKey(
            isMonth = isExpanded,
            year = if (isExpanded) displayedYear else 0,
            month = if (isExpanded) displayedMonth else 0
        )
        AnimatedContent(
            targetState = viewKey,
            transitionSpec = {
                val paging = initialState.isMonth && targetState.isMonth
                if (paging) {
                    val forward = targetState.monthIndex > initialState.monthIndex
                    (fadeIn(WseiMotion.effects()) +
                        slideInHorizontally(WseiMotion.spatial()) { width -> if (forward) width / 5 else -width / 5 }) togetherWith
                        fadeOut(WseiMotion.fastEffects()) using
                        SizeTransform(clip = true) { _, _ -> WseiMotion.size() }
                } else {
                    fadeIn(WseiMotion.effects()) togetherWith fadeOut(WseiMotion.fastEffects()) using
                        SizeTransform(clip = true) { _, _ -> WseiMotion.size() }
                }
            },
            contentAlignment = Alignment.TopCenter,
            label = "calendarWeekMonth"
        ) { key ->
            if (key.isMonth) {
                MonthGrid(
                    displayedYear = key.year,
                    displayedMonth = key.month,
                    selectedDate = selectedDate,
                    todayIso = todayIso,
                    scheduledMonthDays = scheduledMonthDays,
                    onDayClicked = { day -> onSelectDate(LocalDate(key.year, key.month, day)) }
                )
            } else {
                WeekDaysStrip(
                    days = daysOfCurrentWeek,
                    selectedDate = selectedDate,
                    todayIso = todayIso,
                    onSelectDay = onSelectDayInWeek
                )
            }
        }
    }
}

/** Target of the calendar body animation; [year]/[month] are 0 in the week view. */
@Immutable
private data class CalendarViewKey(val isMonth: Boolean, val year: Int, val month: Int) {
    val monthIndex: Int get() = year * 12 + month
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
                    else -> tonalLayer(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        0.5f
                    )
                },
                animationSpec = WseiMotion.effects(),
                label = "dayContainer"
            )
            val contentColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                hasClasses -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val classesState = if (hasClasses) {
                pluralStringResource(R.plurals.schedule_day_classes_state, day.events.size, day.events.size)
            } else {
                stringResource(R.string.schedule_day_no_classes_state)
            }
            val dayState = if (isToday) stringResource(R.string.schedule_day_today_state, classesState) else classesState
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
                        stateDescription = dayState
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(day.date.dayOfWeek.shortNameRes), style = MaterialTheme.typography.labelMedium)
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

/** Two-letter weekday headers of the month grid, Monday first. */
private val WeekdayHeaderRes = listOf(
    R.string.day_monday_min,
    R.string.day_tuesday_min,
    R.string.day_wednesday_min,
    R.string.day_thursday_min,
    R.string.day_friday_min,
    R.string.day_saturday_min,
    R.string.day_sunday_min
)

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
            WeekdayHeaderRes.forEach { labelRes ->
                Text(
                    text = stringResource(labelRes),
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

/** Month name in the nominative case ("Listopad"), as used in the calendar title. */
@StringRes
private fun monthNameRes(monthNumber: Int): Int = when (monthNumber) {
    1 -> R.string.month_january
    2 -> R.string.month_february
    3 -> R.string.month_march
    4 -> R.string.month_april
    5 -> R.string.month_may
    6 -> R.string.month_june
    7 -> R.string.month_july
    8 -> R.string.month_august
    9 -> R.string.month_september
    10 -> R.string.month_october
    11 -> R.string.month_november
    else -> R.string.month_december
}

// -----------------------------------------------------------------------------------------
// Ongoing class
// -----------------------------------------------------------------------------------------

/** Hero content switches to two columns once the card is at least this wide (medium+ windows). */
private val HeroTwoColumnMinWidth = 560.dp

/**
 * "Trwa teraz" — the ongoing class hero (M3 Expressive): status and time range, subject badge,
 * countdown with wavy progress, room and lecturer (masked by "Ukryj dane") and, when expanded,
 * the next class. On wide windows the content uses two columns instead of stretching.
 *
 * Colour: the card is `primaryContainer` / `onPrimaryContainer`. The status chip, the badge and
 * the progress use the inverted pair (`onPrimaryContainer` on top of the container), never raw
 * `primary`, so nothing blends in when a wallpaper produces a high-chroma container.
 *
 * @param lecturerLabel lecturer as it may be shown — pass `uiState.maskLecturer(...)`.
 */
@Composable
fun ActiveClassHeroCard(
    activeClass: ClassEvent,
    nextClassPreview: NextClassPreview?,
    nowEpochMillis: Long,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    lecturerLabel: String = activeClass.lecturer.localizedNameOrPlaceholder(),
    @Suppress("UNUSED_PARAMETER") onTriggerLiveNotification: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val stateText = stringResource(R.string.schedule_hero_state, activeClass.subject.name, remainingMin)
    // Only the next-class preview is behind the expand affordance; without one the card is static.
    val canExpand = nextClassPreview != null

    WseiCard(
        onClick = if (canExpand) onToggleExpand else null,
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
                        HeroHeader(activeClass = activeClass, canExpand = canExpand, isExpanded = isExpanded)
                        HeroDetails(activeClass = activeClass, lecturerLabel = lecturerLabel)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
                    ) {
                        HeroProgress(activeClass = activeClass, nowEpochMillis = nowEpochMillis)
                        HeroNextClass(isExpanded = isExpanded, nextClassPreview = nextClassPreview)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)) {
                    HeroHeader(activeClass = activeClass, canExpand = canExpand, isExpanded = isExpanded)
                    HeroProgress(activeClass = activeClass, nowEpochMillis = nowEpochMillis)
                    HeroDetails(activeClass = activeClass, lecturerLabel = lecturerLabel)
                    HeroNextClass(isExpanded = isExpanded, nextClassPreview = nextClassPreview)
                }
            }
        }
    }
}

/** Status chip and time range, expand affordance, then the accent badge with type and subject. */
@Composable
private fun HeroHeader(activeClass: ClassEvent, canExpand: Boolean, isExpanded: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s)
        ) {
            ScheduleStatusChip(
                text = stringResource(R.string.schedule_hero_status_live),
                containerColor = colors.onPrimaryContainer,
                contentColor = colors.primaryContainer
            )
            Text(
                text = stringResource(
                    R.string.schedule_hero_time_range,
                    activeClass.timeSlot.startFormatted,
                    activeClass.timeSlot.endFormatted
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (canExpand) {
                HeroExpandChevron(isExpanded = isExpanded)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            ScheduleAccentBadge(
                icon = WseiIcons.forSubject(activeClass.subject.name, activeClass.classType),
                containerColor = colors.onPrimaryContainer,
                contentColor = colors.primaryContainer
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xxs)
            ) {
                Text(
                    text = stringResource(activeClass.classType.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer,
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

/** Expand affordance of the hero; rotation is applied in the graphics layer only. */
@Composable
private fun HeroExpandChevron(isExpanded: Boolean) {
    val colors = MaterialTheme.colorScheme
    val rotation = animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = WseiMotion.fastSpatial(),
        label = "heroChevron"
    )
    val toggleLabel = stringResource(
        if (isExpanded) R.string.schedule_hero_collapse_cd else R.string.schedule_hero_expand_cd
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = WseiIcons.ExpandMore,
            // Read as part of the (clickable) card: tells what a tap does.
            contentDescription = toggleLabel,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { rotationZ = rotation.value }
        )
    }
}

/**
 * Status pill with a MaterialShapes marker („Trwa teraz”, „Następne”, „Jutro”…) — shared by the
 * Plan zajęć hero and the Start focus card.
 */
@Composable
internal fun ScheduleStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
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
                    .background(contentColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/** Expressive accent badge (9-sided cookie) holding the subject / state icon. */
@Composable
internal fun ScheduleAccentBadge(
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

/**
 * Big countdown figure with a muted caption — the key number of the hero. The figure rolls
 * vertically when it changes; the caption stays put.
 */
@Composable
internal fun ScheduleCountdown(
    value: String,
    caption: String,
    captionColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically(WseiMotion.spatial()) { it / 2 } + fadeIn(WseiMotion.effects())) togetherWith
                    (slideOutVertically(WseiMotion.spatial()) { -it / 2 } + fadeOut(WseiMotion.fastEffects())) using
                    SizeTransform(clip = false)
            },
            label = "scheduleCountdown"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelLarge,
            color = captionColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Remaining minutes as the key figure with the wavy progress underneath. */
@Composable
private fun HeroProgress(activeClass: ClassEvent, nowEpochMillis: Long) {
    val colors = MaterialTheme.colorScheme
    val elapsedMin = activeClass.elapsedMinutes(nowEpochMillis)
    val totalMin = activeClass.totalDurationMinutes
    val remainingMin = activeClass.remainingMinutes(nowEpochMillis)
    val animatedProgress = animateFloatAsState(
        targetValue = activeClass.progressFraction(nowEpochMillis),
        animationSpec = WseiMotion.size(),
        label = "liveClassProgress"
    )
    val progressDescription = stringResource(R.string.schedule_hero_progress_cd, elapsedMin, totalMin)

    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)) {
        ScheduleCountdown(
            value = stringResource(R.string.schedule_hero_remaining_value, remainingMin),
            caption = stringResource(R.string.schedule_hero_remaining_label),
            captionColor = mutedContentColor(colors.onPrimaryContainer, colors.primaryContainer)
        )
        LinearWavyProgressIndicator(
            // Read lazily: the indicator redraws without recomposing the hero.
            progress = { animatedProgress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .semantics { contentDescription = progressDescription },
            color = colors.onPrimaryContainer,
            trackColor = tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.16f)
        )
    }
}

/** Room / delivery mode, lecturer (already masked by the caller) and a real group code. */
@Composable
private fun HeroDetails(activeClass: ClassEvent, lecturerLabel: String) {
    val colors = MaterialTheme.colorScheme
    val chipColor = tonalLayer(colors.primaryContainer, colors.onPrimaryContainer, 0.1f)
    val chipContent = colors.onPrimaryContainer.withContrastAgainst(chipColor)
    val location = if (activeClass.deliveryMode == DeliveryMode.ON_SITE) {
        activeClass.room.localizedLabel()
    } else {
        stringResource(activeClass.deliveryMode.labelRes)
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.s)
    ) {
        WseiPill(
            text = location,
            icon = WseiIcons.LocationOn,
            containerColor = chipColor,
            contentColor = chipContent
        )
        WseiPill(
            text = lecturerLabel,
            icon = WseiIcons.Person,
            containerColor = chipColor,
            contentColor = chipContent
        )
        displayGroupCode(activeClass.groupCode)?.let { group ->
            WseiPill(
                text = group,
                icon = WseiIcons.Groups,
                containerColor = chipColor,
                contentColor = chipContent
            )
        }
    }
}

/** The next class, revealed with a single size animation. */
@Composable
private fun HeroNextClass(isExpanded: Boolean, nextClassPreview: NextClassPreview?) {
    // Keeps the last preview while the block animates out (a plain holder, not state).
    val lastPreview = remember { arrayOfNulls<NextClassPreview>(1) }
    if (nextClassPreview != null) lastPreview[0] = nextClassPreview
    AnimatedVisibility(
        visible = isExpanded && nextClassPreview != null,
        enter = expandVertically(WseiMotion.size()) + fadeIn(WseiMotion.effects()),
        exit = shrinkVertically(WseiMotion.size()) + fadeOut(WseiMotion.fastEffects())
    ) {
        val shown = nextClassPreview ?: lastPreview[0]
        if (shown != null) {
            NextClassPreviewCard(preview = shown)
        }
    }
}

@Composable
private fun NextClassPreviewCard(preview: NextClassPreview) {
    val colors = MaterialTheme.colorScheme
    val next = preview.nextEvent
    val location = if (next.deliveryMode == DeliveryMode.ON_SITE) {
        next.room.localizedLabel()
    } else {
        stringResource(next.deliveryMode.labelRes)
    }
    val breakLabel = if (preview.breakDurationMinutes > 0) {
        stringResource(R.string.schedule_hero_next_break, preview.breakDurationMinutes)
    } else {
        stringResource(R.string.schedule_hero_next_no_break)
    }
    // An opaque inset on the hero; every text colour is checked against it.
    val insetColor = tonalLayer(colors.primaryContainer, colors.surfaceContainerLowest, 0.6f)
    Surface(
        shape = WseiInsetShape,
        color = insetColor,
        contentColor = colors.onSurface.withContrastAgainst(insetColor),
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
                    color = colors.tertiary.withContrastAgainst(insetColor)
                )
                Text(
                    text = next.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.schedule_hero_next_details, breakLabel, location),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant.withContrastAgainst(insetColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Material 3 Expressive callout card above the day list when the focus moved away from today:
 * explains what happened today ("Dzisiaj bez zajęć" / "Dzisiejsze zajęcia zakończone"), which
 * kind of day is shown right below (tomorrow, a later zjazd day, the next zjazd), and shows an
 * optional relative countdown pill ("Za 7 dni" / "Jutro") without repeating the full date.
 */
@Composable
private fun ScheduleFocusNotice(
    focus: ScheduleFocus,
    todayIso: String,
    todayHadClasses: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val locale = appLocale()
    val title = stringResource(
        if (todayHadClasses) R.string.schedule_focus_today_done else R.string.schedule_focus_today_free
    )
    val detail = stringResource(
        when (focus.kind) {
            ScheduleFocusKind.TOMORROW -> R.string.schedule_focus_showing_tomorrow
            ScheduleFocusKind.LATER_THIS_ZJAZD -> R.string.schedule_focus_showing_later
            else -> R.string.schedule_focus_showing_next_zjazd
        }
    ).capitalizeFirst(locale)
    val daysUntil = remember(todayIso, focus.dateIso) {
        val targetIso = focus.dateIso ?: return@remember null
        runCatching {
            ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(todayIso),
                java.time.LocalDate.parse(targetIso)
            ).toInt()
        }.getOrNull()
    }
    val countdownLabel = daysUntil?.takeIf { it >= 1 }?.let { relativeDaysLabel(it)?.capitalizeFirst(locale) }
    val noticeShape = remember {
        RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = 12.dp,
            bottomEnd = 28.dp
        )
    }

    Surface(
        shape = noticeShape,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WseiSpacing.l, vertical = WseiSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            WseiIconBadge(
                icon = if (todayHadClasses) WseiIcons.CheckCircle else WseiIcons.AutoAwesome,
                shape = if (todayHadClasses) {
                    MaterialShapes.Cookie9Sided.toShape()
                } else {
                    MaterialShapes.Sunny.toShape()
                },
                containerColor = colors.onSecondaryContainer,
                contentColor = colors.secondaryContainer,
                size = 44.dp,
                iconSize = 21.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedContentColor(colors.onSecondaryContainer, colors.secondaryContainer),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (countdownLabel != null) {
                val pillBg = tonalLayer(colors.secondaryContainer, colors.onSecondaryContainer, 0.14f)
                WseiPill(
                    text = countdownLabel,
                    containerColor = pillBg,
                    contentColor = colors.onSecondaryContainer.withContrastAgainst(pillBg),
                    emphasized = true
                )
            }
        }
    }
}

@Composable
private fun DaySectionHeader(selectedDay: AcademicDay?, topPadding: Dp, modifier: Modifier = Modifier) {
    if (selectedDay == null) return
    // Keyed by date: switching days slides the header in the direction of travel, while a refresh
    // of the same day only updates the count in place.
    AnimatedContent(
        targetState = selectedDay,
        contentKey = { it.date },
        transitionSpec = {
            dayChangeEnter(forward = targetState.date > initialState.date) togetherWith
                fadeOut(WseiMotion.fastEffects()) using
                SizeTransform(clip = false) { _, _ -> WseiMotion.size() }
        },
        contentAlignment = Alignment.TopStart,
        modifier = modifier.fillMaxWidth(),
        label = "scheduleDayHeader"
    ) { day ->
        DaySectionHeaderContent(day, topPadding)
    }
}

/** Enter transition of day-keyed content: a short slide from the side we are moving to. */
private fun dayChangeEnter(forward: Boolean): EnterTransition =
    fadeIn(WseiMotion.effects()) +
        slideInHorizontally(WseiMotion.spatial()) { width -> if (forward) width / 10 else -width / 10 }

@Composable
private fun DaySectionHeaderContent(selectedDay: AcademicDay, topPadding: Dp) {
    val count = selectedDay.events.size
    val countText = if (count == 0) {
        stringResource(R.string.schedule_day_no_classes)
    } else {
        pluralStringResource(R.plurals.schedule_day_blocks, count, count)
    }
    val dateFormatter = rememberLocalizedDateFormatter(DayDateSkeleton)
    val dateText = remember(selectedDay.date, dateFormatter) {
        java.time.LocalDate.of(selectedDay.date.year, selectedDay.date.monthNumber, selectedDay.date.dayOfMonth)
            .format(dateFormatter)
    }
    // Count folded into the date line — the former "Zjazd • N bloki" chip only repeated it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = WseiSpacing.xs, end = WseiSpacing.xs, top = topPadding)
    ) {
        Text(
            text = stringResource(selectedDay.date.dayOfWeek.fullNameRes),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.schedule_day_date_count, dateText, countText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -----------------------------------------------------------------------------------------
// Day off — with a hint about the nearest class day from today
// -----------------------------------------------------------------------------------------

/** What the day-off card shows: the viewed day and the nearest upcoming class day from today (if any). */
@Immutable
private data class DayOffModel(val dateIso: String, val next: NextClassDay?)

/**
 * Material 3 Expressive "Wolne od zajęć" card for a day without classes. When an upcoming class
 * day exists from today, an inset card highlights its status ("Następny zjazd" / "Dalej w tym
 * zjeździe" / "Dzisiaj"), relative countdown pill, full day title, first start + block count, and
 * a full-width expressive jump button. Content is keyed by the viewed date so moving between free
 * days cross-fades and resizes with [WseiMotion.size].
 */
@Composable
private fun DayOffCard(
    model: DayOffModel,
    onGoTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    WseiCard(
        modifier = modifier.fillMaxWidth(),
        shape = ActiveHeroCardAsymmetricShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(WseiSpacing.xl)
    ) {
        AnimatedContent(
            targetState = model,
            contentKey = { it.dateIso },
            transitionSpec = {
                dayChangeEnter(forward = targetState.dateIso > initialState.dateIso) togetherWith
                    fadeOut(WseiMotion.fastEffects()) using
                    SizeTransform(clip = false) { _, _ -> WseiMotion.size() }
            },
            contentAlignment = Alignment.TopStart,
            label = "scheduleDayOff"
        ) { shown ->
            DayOffContent(next = shown.next, onGoTo = onGoTo)
        }
    }
}

@Composable
private fun DayOffContent(next: NextClassDay?, onGoTo: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            WseiIconBadge(
                icon = WseiIcons.AutoAwesome,
                shape = MaterialShapes.Sunny.toShape(),
                containerColor = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                size = 52.dp,
                iconSize = 24.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.schedule_day_off_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        if (next == null) R.string.schedule_day_off_message else R.string.schedule_day_off_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        if (next != null) {
            NextClassDayDetails(next = next, onGoTo = onGoTo)
        }
    }
}

@Composable
private fun NextClassDayDetails(next: NextClassDay, onGoTo: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val locale = appLocale()
    val dayTitle = rememberDayTitle(next.dateIso)
    val statusLabel = when {
        next.daysFromToday == 0 -> stringResource(R.string.schedule_relative_today).capitalizeFirst(locale)
        next.startsZjazd -> stringResource(R.string.home_focus_status_next_zjazd)
        else -> stringResource(R.string.home_focus_status_later)
    }
    val countdownLabel = next.daysFromToday
        .takeIf { it >= 1 }
        ?.let { relativeDaysLabel(it)?.capitalizeFirst(locale) }
    val shortDateFormatter = rememberLocalizedDateFormatter(GoToDateSkeleton)
    val shortDate = remember(next.dateIso, shortDateFormatter) {
        runCatching { java.time.LocalDate.parse(next.dateIso).format(shortDateFormatter) }.getOrDefault(next.dateIso)
    }

    Surface(
        shape = WseiInsetShape,
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WseiSpacing.l),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)
        ) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(WseiSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(WseiSpacing.xs)
                ) {
                    WseiPill(
                        text = statusLabel,
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                        emphasized = true
                    )
                    if (countdownLabel != null) {
                        WseiPill(
                            text = countdownLabel,
                            containerColor = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer
                        )
                    }
                }
                Text(
                    text = dayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        R.string.schedule_day_off_next_details,
                        next.firstStartLabel,
                        pluralStringResource(R.plurals.schedule_day_blocks, next.blockCount, next.blockCount)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { onGoTo(next.dateIso) },
                // Expressive shape morph on press, full-width inside the inset card.
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = WseiIcons.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(R.string.schedule_day_off_go_to, shortDate),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** CLDR skeleton of the jump button date ("4 grudnia" / "December 4"). */
private const val GoToDateSkeleton = "dMMMM"

/** CLDR skeleton of a day title ("piątek, 4 grudnia" / "Friday, December 4"). */
private const val DayTitleSkeleton = "EEEEdMMMM"

/** "Piątek, 4 grudnia" for [dateIso] in the app language (empty for a malformed date). */
@Composable
internal fun rememberDayTitle(dateIso: String?): String {
    val locale = appLocale()
    val formatter = rememberLocalizedDateFormatter(DayTitleSkeleton)
    return remember(dateIso, formatter, locale) {
        dateIso?.let { iso ->
            runCatching { java.time.LocalDate.parse(iso).format(formatter).capitalizeFirst(locale) }.getOrNull()
        }.orEmpty()
    }
}

/**
 * Distance of a class day from today: "dzisiaj", "jutro" or "za 11 dni"; `null` for a day in
 * the past (no countdown is shown then).
 */
@Composable
internal fun relativeDaysLabel(days: Int): String? = when {
    days < 0 -> null
    days == 0 -> stringResource(R.string.schedule_relative_today)
    days == 1 -> stringResource(R.string.schedule_relative_tomorrow)
    else -> pluralStringResource(R.plurals.home_focus_in_days, days, days)
}

/** CLDR skeleton of the date under the day name ("20 listopada 2026" / "November 20, 2026"). */
private const val DayDateSkeleton = "dMMMMy"

// -----------------------------------------------------------------------------------------
// Device calendar sync
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
 * The calendar export flow. Permission problems use a plain dialog (nothing else is open then);
 * the picker, the progress and the result all live in ONE modal bottom sheet, so no second
 * dialog pops over the picker and the sheet never re-enters while the flow moves on.
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
    if (permissionIssue != null) {
        CalendarPermissionDialog(
            issue = permissionIssue,
            onDismiss = onDismissPermissionIssue,
            onRetry = onRetryPermission,
            onOpenSettings = onOpenAppSettings
        )
        return
    }
    val hasResult = status is CalendarSyncStatus.Syncing ||
        status is CalendarSyncStatus.Done ||
        status is CalendarSyncStatus.Failed
    if (isPickerVisible || hasResult) {
        CalendarSyncSheet(
            status = status,
            calendars = calendars,
            lastSyncCalendarId = lastSyncCalendarId,
            isLecturerHidden = isLecturerHidden,
            onDismiss = {
                if (status is CalendarSyncStatus.Done || status is CalendarSyncStatus.Failed) {
                    onDismissStatus()
                } else {
                    onDismissPicker()
                }
            },
            onConfirm = onConfirmCalendar
        )
    }
}

/** What the calendar sheet body shows; one AnimatedContent cross-fades between these. */
private enum class SyncSheetStage { LOADING, EMPTY, LIST, SYNCING, DONE, FAILED }

/** Minimum body height, so loading → list → progress → result never collapses the sheet. */
private val SyncSheetBodyMinHeight = 220.dp

@Composable
private fun CalendarSyncSheet(
    status: CalendarSyncStatus,
    calendars: List<DeviceCalendar>,
    lastSyncCalendarId: Long?,
    isLecturerHidden: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val isSyncing = status is CalendarSyncStatus.Syncing
    // Syncing is short and must not be interrupted half-way: swipe, back and scrim are ignored.
    val syncingState = rememberUpdatedState(isSyncing)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value -> value != SheetValue.Hidden || !syncingState.value }
    )
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // Buttons slide the sheet away first, then report the dismissal (same as a swipe).
    val animateDismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) currentOnDismiss()
        }
    }

    var chosenId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Explicit choice → calendar used last time → the primary account calendar → the first one.
    val selectedId = remember(chosenId, calendars, lastSyncCalendarId) {
        chosenId?.takeIf { id -> calendars.any { it.id == id } }
            ?: lastSyncCalendarId?.takeIf { id -> calendars.any { it.id == id } }
            ?: calendars.firstOrNull { it.isPrimary }?.id
            ?: calendars.firstOrNull()?.id
    }
    val stage = when {
        status is CalendarSyncStatus.Syncing -> SyncSheetStage.SYNCING
        status is CalendarSyncStatus.Done -> SyncSheetStage.DONE
        status is CalendarSyncStatus.Failed -> SyncSheetStage.FAILED
        status is CalendarSyncStatus.LoadingCalendars -> SyncSheetStage.LOADING
        calendars.isEmpty() -> SyncSheetStage.EMPTY
        else -> SyncSheetStage.LIST
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !isSyncing,
            shouldDismissOnClickOutside = !isSyncing
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WseiSpacing.xxl)
                .padding(bottom = WseiSpacing.xxl + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.l)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.m)
            ) {
                ScheduleAccentBadge(
                    icon = WseiIcons.CalendarMonth,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    size = 44.dp
                )
                Text(
                    text = stringResource(R.string.calendar_sync_action),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
            }
            // The only size animation of the sheet: a fixed minimum height keeps short stages
            // (loading, progress) from collapsing it, and SizeTransform springs the rest.
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    fadeIn(WseiMotion.effects()) togetherWith fadeOut(WseiMotion.fastEffects()) using
                        SizeTransform(clip = true) { _, _ -> WseiMotion.size() }
                },
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SyncSheetBodyMinHeight),
                label = "calendarSyncSheetStage"
            ) { shown ->
                when (shown) {
                    SyncSheetStage.LOADING -> SyncSheetBusy(text = stringResource(R.string.calendar_sync_loading))
                    SyncSheetStage.SYNCING -> SyncSheetBusy(text = stringResource(R.string.calendar_sync_in_progress))
                    SyncSheetStage.EMPTY -> Text(
                        text = stringResource(R.string.calendar_sync_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SyncSheetStage.LIST -> CalendarPickerList(
                        calendars = calendars,
                        selectedId = selectedId,
                        isLecturerHidden = isLecturerHidden,
                        onSelect = { chosenId = it }
                    )
                    SyncSheetStage.DONE -> {
                        val result = (status as? CalendarSyncStatus.Done)?.result
                        SyncSheetResult(
                            icon = WseiIcons.CheckCircle,
                            isError = false,
                            title = stringResource(R.string.calendar_sync_done_title),
                            message = when {
                                result == null -> ""
                                result.inserted == 0 && result.updated == 0 && result.deleted == 0 ->
                                    stringResource(R.string.calendar_sync_done_no_changes)
                                else -> stringResource(
                                    R.string.calendar_sync_done_message,
                                    result.inserted,
                                    result.updated,
                                    result.deleted
                                )
                            }
                        )
                    }
                    SyncSheetStage.FAILED -> SyncSheetResult(
                        icon = WseiIcons.Info,
                        isError = true,
                        title = stringResource(R.string.calendar_sync_failed_title),
                        message = (status as? CalendarSyncStatus.Failed)?.message?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.calendar_sync_failed_generic)
                    )
                }
            }
            // Fixed-height action row: buttons change, the sheet height does not.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(WseiSpacing.s, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (stage) {
                    SyncSheetStage.LOADING, SyncSheetStage.LIST -> {
                        TextButton(onClick = animateDismiss, shapes = ButtonDefaults.shapes()) {
                            Text(text = stringResource(R.string.calendar_sync_cancel))
                        }
                        Button(
                            onClick = { selectedId?.let(onConfirm) },
                            enabled = stage == SyncSheetStage.LIST && selectedId != null,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text(text = stringResource(R.string.calendar_sync_confirm))
                        }
                    }
                    SyncSheetStage.EMPTY, SyncSheetStage.DONE, SyncSheetStage.FAILED -> {
                        FilledTonalButton(onClick = animateDismiss, shapes = ButtonDefaults.shapes()) {
                            Text(text = stringResource(R.string.calendar_sync_close))
                        }
                    }
                    SyncSheetStage.SYNCING -> Unit
                }
            }
        }
    }
}

@Composable
private fun SyncSheetBusy(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SyncSheetBodyMinHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.l, Alignment.CenterVertically)
    ) {
        LoadingIndicator(modifier = Modifier.size(56.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SyncSheetResult(icon: ImageVector, isError: Boolean, title: String, message: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SyncSheetBodyMinHeight)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WseiSpacing.m, Alignment.CenterVertically)
    ) {
        ScheduleAccentBadge(
            icon = icon,
            containerColor = if (isError) colors.errorContainer else colors.tertiaryContainer,
            contentColor = if (isError) colors.onErrorContainer else colors.onTertiaryContainer,
            size = 72.dp
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarPickerList(
    calendars: List<DeviceCalendar>,
    selectedId: Long?,
    isLecturerHidden: Boolean,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(WseiSpacing.m)) {
        Text(
            text = stringResource(R.string.calendar_sync_picker_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isLecturerHidden) {
            Text(
                text = stringResource(R.string.calendar_sync_hidden_lecturer_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = stringResource(R.string.calendar_sync_picker_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(WseiSpacing.inGroup)
        ) {
            itemsIndexed(
                items = calendars,
                key = { _, calendar -> calendar.id },
                contentType = { _, _ -> "calendar_option" }
            ) { index, calendar ->
                CalendarOptionRow(
                    calendar = calendar,
                    selected = calendar.id == selectedId,
                    shape = wseiGroupItemShape(index, calendars.size),
                    onSelect = { onSelect(calendar.id) }
                )
            }
        }
    }
}

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
            TextButton(
                onClick = if (permanentlyDenied) onOpenSettings else onRetry,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = stringResource(
                        if (permanentlyDenied) R.string.calendar_permission_open_settings else R.string.calendar_permission_retry
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(text = stringResource(R.string.calendar_sync_cancel))
            }
        }
    )
}
