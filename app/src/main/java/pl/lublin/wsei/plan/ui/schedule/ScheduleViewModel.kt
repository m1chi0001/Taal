package pl.lublin.wsei.plan.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pl.lublin.wsei.plan.R
import pl.lublin.wsei.plan.data.local.AppPreferencesStore
import pl.lublin.wsei.plan.data.local.LocalizedStrings
import pl.lublin.wsei.plan.data.local.describeError
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpenResult
import pl.lublin.wsei.plan.domain.attachments.AttachmentOpener
import pl.lublin.wsei.plan.domain.calendar.CalendarSyncManager
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.AppColorPalette
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesStore: AppPreferencesStore,
    private val calendarSyncManager: CalendarSyncManager,
    private val attachmentOpener: AttachmentOpener,
    private val strings: LocalizedStrings
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            savedEncryptedLoginHint = scheduleRepository.getDecryptedSavedLogin()
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        startLiveMinuteTicker()
        observeSessionAndSchedule()
        restoreEncryptedSessionOnStart()
    }

    private fun restoreEncryptedSessionOnStart() {
        viewModelScope.launch {
            val restored = try {
                scheduleRepository.restoreEncryptedSessionIfAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (restored == null) {
                _uiState.update { it.copy(isSessionRestoring = false) }
            } else {
                val initialDate = currentWarsawLocalDate()
                _uiState.update {
                    it.copy(
                        currentSession = restored,
                        isSessionRestoring = false,
                        selectedDate = initialDate,
                        displayedYear = initialDate.year,
                        displayedMonth = initialDate.monthNumber,
                        syncOrigin = if (restored.isDebugAccount) {
                            DataSourceOrigin.DEBUG_TEST_ACCOUNT
                        } else {
                            DataSourceOrigin.APR_WD_API
                        }
                    )
                }
            }
        }
    }

    private fun startLiveMinuteTicker() {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (_uiState.value.currentSession != null) {
                    // UI displays minute-granularity progress, so update once per minute boundary.
                    val minuteStart = now - (now % 60_000L)
                    _uiState.update { state ->
                        if (state.nowEpochMillis == minuteStart) state else state.copy(nowEpochMillis = minuteStart)
                    }
                }
                delay((60_000L - (now % 60_000L)).coerceAtLeast(1_000L))
            }
        }
    }

    private fun observeSessionAndSchedule() {
        viewModelScope.launch {
            // Identity of the student whose data is currently loaded. Background token refresh
            // re-emits the session and must not reset the selected day or clear loaded modules.
            var loadedStudentKey: String? = null
            scheduleRepository.currentSession.collect { session ->
                val studentKey = session?.let { "${it.userId}:${it.studId}:${it.isDebugAccount}" }
                if (session != null && studentKey == loadedStudentKey) {
                    _uiState.update { it.copy(currentSession = session) }
                    return@collect
                }
                loadedStudentKey = studentKey
                if (session == null) {
                    _uiState.update {
                        ScheduleUiState(
                            savedEncryptedLoginHint = scheduleRepository.getDecryptedSavedLogin(),
                            nowEpochMillis = System.currentTimeMillis(),
                            isSessionRestoring = it.isSessionRestoring,
                            themeMode = it.themeMode,
                            colorPalette = it.colorPalette,
                            isLiveNotificationEnabled = it.isLiveNotificationEnabled,
                            isHideSensitiveData = it.isHideSensitiveData,
                            syncCalendarId = it.syncCalendarId
                        )
                    }
                } else {
                    val today = currentWarsawLocalDate()
                    _uiState.update {
                        it.copy(
                            currentSession = session,
                            selectedDate = today,
                            displayedYear = today.year,
                            displayedMonth = today.monthNumber,
                            allEvents = emptyList(),
                            studentGroups = emptyList(),
                            studyPlans = emptyList(),
                            grades = emptyList(),
                            scholarships = emptyList(),
                            financials = emptyList(),
                            announcements = emptyList(),
                            syncOrigin = if (session.isDebugAccount) {
                                DataSourceOrigin.DEBUG_TEST_ACCOUNT
                            } else {
                                DataSourceOrigin.APR_WD_API
                            }
                        )
                    }
                    prefillCachedAcademicModules()
                    refreshScheduleInternal(includeModules = true)
                }
            }
        }

        viewModelScope.launch {
            scheduleRepository.observeAllEvents().collect { events ->
                _uiState.update { state ->
                    val activeEventDate = events.firstOrNull { it.isOngoing(state.nowEpochMillis) }
                        ?.dateIso
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                    // First load: open Plan zajęć on the focus day — today while classes are
                    // ongoing/upcoming, otherwise the next class day (tomorrow / next zjazd).
                    val focusDate = if (state.allEvents.isEmpty()) {
                        ScheduleFocusResolver.resolve(events, state.nowEpochMillis).dateIso
                            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    } else {
                        null
                    }
                    val targetDate = when {
                        focusDate != null -> focusDate
                        state.allEvents.isEmpty() && activeEventDate != null -> activeEventDate
                        else -> state.selectedDate
                    }

                    state.copy(
                        allEvents = events,
                        selectedDate = targetDate,
                        displayedYear = targetDate.year,
                        displayedMonth = targetDate.monthNumber,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun login(username: String, password: String, rememberCredentials: Boolean = true) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthenticating = true,
                    authError = null,
                    allEvents = emptyList(),
                    studentGroups = emptyList(),
                    studyPlans = emptyList(),
                    grades = emptyList(),
                    scholarships = emptyList(),
                    financials = emptyList(),
                    announcements = emptyList()
                )
            }
            val result = scheduleRepository.loginWithCredentials(
                usernameOrAlbum = username,
                password = password,
                rememberCredentials = rememberCredentials
            )
            result.fold(
                onSuccess = { session ->
                    _uiState.update {
                        it.copy(
                            currentSession = session,
                            savedEncryptedLoginHint = if (rememberCredentials) username.trim() else null,
                            isAuthenticating = false,
                            authError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            authError = strings.describeError(error, R.string.error_login_generic)
                        )
                    }
                }
            )
        }
    }

    fun loginDebug(rememberCredentials: Boolean = true) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthenticating = true,
                    authError = null,
                    allEvents = emptyList(),
                    studentGroups = emptyList(),
                    studyPlans = emptyList(),
                    grades = emptyList(),
                    scholarships = emptyList(),
                    financials = emptyList(),
                    announcements = emptyList()
                )
            }
            val debugSession = scheduleRepository.loginWithDebugTestAccount(
                rememberCredentials = rememberCredentials
            )
            _uiState.update {
                it.copy(
                    currentSession = debugSession,
                    isAuthenticating = false,
                    authError = null,
                    syncOrigin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            scheduleRepository.logout()
            _uiState.update {
                ScheduleUiState(
                    savedEncryptedLoginHint = null,
                    nowEpochMillis = System.currentTimeMillis(),
                    isSessionRestoring = false,
                    themeMode = it.themeMode,
                    colorPalette = it.colorPalette,
                    isLiveNotificationEnabled = it.isLiveNotificationEnabled,
                    isHideSensitiveData = it.isHideSensitiveData,
                    syncCalendarId = it.syncCalendarId
                )
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val res = scheduleRepository.changePassword(currentPassword, newPassword)
            _uiState.update {
                it.copy(
                    statusBannerMessage = res.fold(
                        onSuccess = { strings.get(R.string.status_password_changed) },
                        onFailure = { err -> strings.describeError(err, R.string.error_password_change) }
                    )
                )
            }
        }
    }

    fun selectDay(indexInWeek: Int) {
        val clickedDay = _uiState.value.days.getOrNull(indexInWeek) ?: return
        selectDate(clickedDay.date)
    }

    fun selectDate(date: LocalDate) {
        val prevMonth = _uiState.value.displayedMonth
        val prevYear = _uiState.value.displayedYear
        _uiState.update {
            it.copy(
                selectedDate = date,
                displayedYear = date.year,
                displayedMonth = date.monthNumber
            )
        }
        if (date.monthNumber != prevMonth || date.year != prevYear) {
            syncMonthWindow(date.year, date.monthNumber)
        }
    }

    fun toggleCalendarExpanded() {
        _uiState.update { it.copy(isCalendarExpanded = !it.isCalendarExpanded) }
    }

    fun navigatePreviousCalendarPeriod() {
        val state = _uiState.value
        if (state.isCalendarExpanded) {
            val ym = YearMonth.of(state.displayedYear, state.displayedMonth).minusMonths(1)
            val newDay = state.selectedDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
            selectDate(LocalDate(ym.year, ym.monthValue, newDay))
        } else {
            selectDate(state.selectedDate.minus(DatePeriod(days = 7)))
        }
    }

    fun navigateNextCalendarPeriod() {
        val state = _uiState.value
        if (state.isCalendarExpanded) {
            val ym = YearMonth.of(state.displayedYear, state.displayedMonth).plusMonths(1)
            val newDay = state.selectedDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
            selectDate(LocalDate(ym.year, ym.monthValue, newDay))
        } else {
            selectDate(state.selectedDate.plus(DatePeriod(days = 7)))
        }
    }

    fun selectScheduleFilter(filterType: ScheduleFilterType) {
        if (_uiState.value.scheduleFilterType == filterType) return
        _uiState.update { it.copy(scheduleFilterType = filterType) }
        refreshScheduleInternal(includeModules = false)
    }

    fun toggleHeroExpanded() {
        _uiState.update { it.copy(isHeroExpanded = !it.isHeroExpanded) }
    }

    private fun syncMonthWindow(year: Int, month: Int) {
        val session = _uiState.value.currentSession ?: return
        if (session.isDebugAccount) return
        viewModelScope.launch {
            val ym = YearMonth.of(year, month)
            val start = LocalDate(ym.year, ym.monthValue, 1)
            val end = LocalDate(ym.year, ym.monthValue, ym.lengthOfMonth())
            scheduleRepository.syncSchedule(
                startDate = start,
                endDate = end,
                scheduleType = _uiState.value.scheduleFilterType,
                forceRefresh = true
            )
        }
    }

    /**
     * Universal pull-to-refresh handler across Start, Schedule, Subjects, Finances, Announcements,
     * More and Profile: refreshes the 4-month timetable window, student profile (including photo
     * and averages) and all academic modules in parallel.
     */
    fun refreshSchedule() {
        refreshScheduleInternal(includeModules = true)
    }

    private fun refreshScheduleInternal(includeModules: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val session = state.currentSession ?: return@launch
            val currentFilter = state.scheduleFilterType
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            val baseYm = YearMonth.of(state.displayedYear, state.displayedMonth)
            val startYm = baseYm.minusMonths(1)
            val endYm = baseYm.plusMonths(2)
            val start = LocalDate(startYm.year, startYm.monthValue, 1)
            val end = LocalDate(endYm.year, endYm.monthValue, endYm.lengthOfMonth())

            val result = coroutineScope {
                val scheduleDef = async {
                    scheduleRepository.syncSchedule(
                        startDate = start,
                        endDate = end,
                        scheduleType = currentFilter,
                        forceRefresh = true
                    )
                }
                if (includeModules) {
                    launch { refreshAcademicModulesAndProfile() }
                }
                scheduleDef.await()
            }
            result.fold(
                onSuccess = { report ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            syncOrigin = report.usedOrigin,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { s ->
                        s.copy(
                            isRefreshing = false,
                            errorMessage = if (session.isDebugAccount) {
                                null
                            } else {
                                strings.describeError(err, R.string.error_schedule_refresh)
                            }
                        )
                    }
                }
            )
        }
    }

    fun initiateOnlinePayment(
        charge: FinancialObligation,
        onOpenUrl: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = scheduleRepository.fetchOnlinePaymentUrl(
                chargeId = charge.chargeId,
                amountPln = charge.amountPln
            )
            res.fold(
                onSuccess = { url ->
                    _uiState.update {
                        it.copy(statusBannerMessage = strings.get(R.string.status_payment_opening, charge.title))
                    }
                    onOpenUrl(url)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusBannerMessage = strings.describeError(err, R.string.error_payment_start))
                    }
                }
            )
        }
    }

    fun markAnnouncementRead(announcement: DeanAnnouncement) {
        viewModelScope.launch {
            scheduleRepository.markAnnouncementRead(announcement.announcementId)
            _uiState.update { state ->
                state.copy(
                    announcements = state.announcements.map {
                        if (it.id == announcement.id) it.copy(isRead = true) else it
                    }
                )
            }
        }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    /** Persisted settings are the single source of truth; the UI state mirrors them. */
    private fun observeSettings() {
        viewModelScope.launch {
            preferencesStore.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        themeMode = settings.themeMode,
                        colorPalette = settings.colorPalette,
                        isLiveNotificationEnabled = settings.isLiveNotificationEnabled,
                        isHideSensitiveData = settings.hideSensitiveData,
                        syncCalendarId = settings.syncCalendarId
                    )
                }
            }
        }
    }

    fun selectThemeMode(mode: AppThemeMode) = preferencesStore.setThemeMode(mode)

    fun selectColorPalette(palette: AppColorPalette) = preferencesStore.setColorPalette(palette)

    fun setLiveNotificationEnabled(enabled: Boolean) = preferencesStore.setLiveNotificationEnabled(enabled)

    fun setHideSensitiveData(hidden: Boolean) = preferencesStore.setHideSensitiveData(hidden)

    /** Loads writable device calendars for the picker. Requires calendar permissions. */
    fun loadDeviceCalendars() {
        _uiState.update { it.copy(calendarSyncStatus = CalendarSyncStatus.LoadingCalendars) }
        viewModelScope.launch {
            calendarSyncManager.listWritableCalendars().fold(
                onSuccess = { calendars ->
                    _uiState.update {
                        it.copy(deviceCalendars = calendars, calendarSyncStatus = CalendarSyncStatus.Idle)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            calendarSyncStatus = CalendarSyncStatus.Failed(
                                strings.describeError(err, R.string.calendar_sync_failed_generic)
                            )
                        )
                    }
                }
            )
        }
    }

    /** Exports the whole loaded schedule into [calendarId] (idempotent upsert). */
    fun syncScheduleToCalendar(calendarId: Long) {
        preferencesStore.setSyncCalendarId(calendarId)
        _uiState.update { it.copy(calendarSyncStatus = CalendarSyncStatus.Syncing) }
        viewModelScope.launch {
            val state = _uiState.value
            val result = calendarSyncManager.syncEvents(
                calendarId = calendarId,
                events = state.allEvents,
                hideLecturer = state.isHideSensitiveData
            )
            _uiState.update {
                it.copy(
                    calendarSyncStatus = result.fold(
                        onSuccess = { r -> CalendarSyncStatus.Done(r) },
                        onFailure = { err ->
                            CalendarSyncStatus.Failed(
                                strings.describeError(err, R.string.calendar_sync_failed_generic)
                            )
                        }
                    )
                )
            }
        }
    }

    fun dismissCalendarSyncStatus() {
        _uiState.update { it.copy(calendarSyncStatus = CalendarSyncStatus.Idle) }
    }

    /**
     * Downloads [attachment] with the active session and opens it in the default viewer app.
     * Progress is exposed via [ScheduleUiState.openingAttachmentId]; failures via the status banner.
     */
    fun openAttachment(attachment: AnnouncementAttachment) {
        if (_uiState.value.openingAttachmentId != null) return
        _uiState.update { it.copy(openingAttachmentId = attachment.id) }
        viewModelScope.launch {
            val result = attachmentOpener.downloadAndOpen(attachment)
            _uiState.update {
                it.copy(
                    openingAttachmentId = null,
                    statusBannerMessage = result.fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is AttachmentOpenResult.Opened -> null
                                is AttachmentOpenResult.NoViewerApp ->
                                    strings.get(R.string.error_attachment_no_viewer, outcome.fileName)
                            }
                        },
                        onFailure = { err -> strings.describeError(err, R.string.error_attachment_download) }
                    ) ?: it.statusBannerMessage
                )
            }
        }
    }

    private fun prefillCachedAcademicModules() {
        viewModelScope.launch {
            val cached = scheduleRepository.loadCachedAcademicModules()
            _uiState.update { state ->
                state.copy(
                    studentGroups = cached.studentGroups ?: state.studentGroups,
                    studyPlans = cached.studyPlans ?: state.studyPlans,
                    grades = cached.grades ?: state.grades,
                    scholarships = cached.scholarships ?: state.scholarships,
                    financials = cached.financials ?: state.financials,
                    announcements = cached.announcements ?: state.announcements
                )
            }
        }
    }

    private suspend fun refreshAcademicModulesAndProfile() {
        coroutineScope {
            val profileDef = async { scheduleRepository.refreshStudentProfile() }
            val groupsDef = async { scheduleRepository.fetchStudentGroups() }
            val plansDef = async { scheduleRepository.fetchStudyPlans() }
            val gradesDef = async { scheduleRepository.fetchGrades() }
            val scholarshipsDef = async { scheduleRepository.fetchScholarships() }
            val financialsDef = async { scheduleRepository.fetchFinancials() }
            val announcementsDef = async { scheduleRepository.fetchAnnouncements() }

            profileDef.await()
            val groupsRes = groupsDef.await()
            val plansRes = plansDef.await()
            val gradesRes = gradesDef.await()
            val scholarshipsRes = scholarshipsDef.await()
            val financialsRes = financialsDef.await()
            val announcementsRes = announcementsDef.await()

            _uiState.update { state ->
                state.copy(
                    studentGroups = groupsRes.getOrDefault(state.studentGroups),
                    studyPlans = plansRes.getOrDefault(state.studyPlans),
                    grades = gradesRes.getOrDefault(state.grades),
                    scholarships = scholarshipsRes.getOrDefault(state.scholarships),
                    financials = financialsRes.getOrDefault(state.financials),
                    announcements = announcementsRes.getOrDefault(state.announcements)
                )
            }
        }
    }
}
