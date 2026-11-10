package pl.lublin.wsei.plan.ui.schedule

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Base64
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pl.lublin.wsei.plan.data.repository.toPolishFullName
import pl.lublin.wsei.plan.data.repository.toPolishShortName
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.NextClassPreview
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.ScholarshipPayoutItem
import pl.lublin.wsei.plan.domain.model.StudentGroupItem
import pl.lublin.wsei.plan.domain.model.StudentSemesterGroups
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.model.StudyPlanOverview
import pl.lublin.wsei.plan.domain.model.StudyPlanSemester
import pl.lublin.wsei.plan.domain.model.StudyPlanSubjectItem
import pl.lublin.wsei.plan.domain.model.Subject
import pl.lublin.wsei.plan.domain.model.TimeSlot
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class AppThemeMode(val polishLabel: String) {
    SYSTEM("Zgodny z systemem"),
    LIGHT("Jasny"),
    DARK("Ciemny"),
    DYNAMIC("Dynamiczny (Material You)")
}

@Immutable
data class ScheduleUiState(
    val currentSession: StudentSession? = null,
    val savedEncryptedLoginHint: String? = null,
    val isAuthenticating: Boolean = false,
    val isSessionRestoring: Boolean = true,
    val authError: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val nowEpochMillis: Long = System.currentTimeMillis(),
    val selectedDate: LocalDate = currentWarsawLocalDate(),
    val displayedYear: Int = selectedDate.year,
    val displayedMonth: Int = selectedDate.monthNumber,
    val isCalendarExpanded: Boolean = false,
    val allEvents: List<ClassEvent> = emptyList(),
    val scheduleFilterType: ScheduleFilterType = ScheduleFilterType.MY_PLAN,
    val isHeroExpanded: Boolean = true,
    val syncOrigin: DataSourceOrigin = DataSourceOrigin.APR_WD_API,
    val studentGroups: List<StudentSemesterGroups> = emptyList(),
    val studyPlans: List<StudyPlanOverview> = emptyList(),
    val grades: List<GradeItem> = emptyList(),
    val scholarships: List<ScholarshipApplication> = emptyList(),
    val financials: List<FinancialObligation> = emptyList(),
    val announcements: List<DeanAnnouncement> = emptyList(),
    val statusBannerMessage: String? = null,
    val errorMessage: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isLiveNotificationEnabled: Boolean = true,
    val isHideSensitiveData: Boolean = false
) {
    /**
     * Zwraca zamaskowaną wartość (••••••), jeśli użytkownik włączył tryb ochrony danych wrażliwych.
     */
    fun maskSensitive(value: String): String = if (isHideSensitiveData) "••••••" else value

    /** Liczba nieprzeczytanych ogłoszeń — liczona raz na stan, nie przy każdej rekompozycji. */
    val unreadAnnouncementsCount: Int by lazy(LazyThreadSafetyMode.NONE) { announcements.count { !it.isRead } }
    /**
     * Dynamically builds the 7-day Monday..Sunday week containing [selectedDate].
     */
    val days: List<AcademicDay> by lazy(LazyThreadSafetyMode.NONE) {
            val daysFromMonday = selectedDate.dayOfWeek.ordinal
            val weekMonday = selectedDate.minus(DatePeriod(days = daysFromMonday))
            val eventsByDate = allEvents.groupBy { it.dateIso }
            return@lazy (0..6).map { offset ->
                val date = weekMonday.plus(DatePeriod(days = offset))
                val dayEvents = eventsByDate[date.toString()]
                    .orEmpty()
                    .sortedBy { it.timeSlot.startEpochMillis }
                AcademicDay(
                    date = date,
                    dayOfWeekLabel = date.dayOfWeek.toPolishFullName(),
                    shortDayLabel = date.dayOfWeek.toPolishShortName(),
                    dayOfMonthNumber = date.dayOfMonth.toString().padStart(2, '0'),
                    events = dayEvents,
                    isWeekendExtramuralSession = date.dayOfWeek == DayOfWeek.SATURDAY ||
                        date.dayOfWeek == DayOfWeek.SUNDAY
                )
            }
    }

    val selectedDayIndex: Int
        get() = selectedDate.dayOfWeek.ordinal

    val selectedDay: AcademicDay? by lazy(LazyThreadSafetyMode.NONE) { days.getOrNull(selectedDayIndex) }

    /**
     * All day-of-month numbers (1..31) in [displayedYear]-[displayedMonth] that have scheduled classes.
     */
    val scheduledMonthDays: Set<Int> by lazy(LazyThreadSafetyMode.NONE) {
            val prefix = "%04d-%02d-".format(displayedYear, displayedMonth)
            return@lazy allEvents
                .asSequence()
                .filter { it.dateIso.startsWith(prefix) }
                .mapNotNull { it.dateIso.takeLast(2).toIntOrNull() }
                .toSet()
    }

    val activeClass: ClassEvent? by lazy(LazyThreadSafetyMode.NONE) { allEvents.firstOrNull { it.isOngoing(nowEpochMillis) } }

    /**
     * Zajęcia, dla których powiadomienie na żywo ma działać: trwające teraz albo następne po przerwie
     * (gdy dziś coś już się odbyło). Dzięki temu powiadomienie nie znika w przerwie między zajęciami.
     */
    val liveTrackingEventId: String? by lazy(LazyThreadSafetyMode.NONE) {
        activeClass?.id ?: run {
            val todayIso = currentWarsawLocalDate().toString()
            val today = allEvents.filter { it.dateIso == todayIso }
            val hasFinishedToday = today.any { it.timeSlot.endEpochMillis <= nowEpochMillis }
            if (!hasFinishedToday) null
            else today
                .filter { it.timeSlot.startEpochMillis > nowEpochMillis }
                .minByOrNull { it.timeSlot.startEpochMillis }
                ?.id
        }
    }

    val activeDayEvents: List<ClassEvent> by lazy(LazyThreadSafetyMode.NONE) {
            val active = activeClass
            val targetDateIso = active?.dateIso ?: selectedDate.toString()
            return@lazy allEvents
                .filter { it.dateIso == targetDateIso }
                .sortedBy { it.timeSlot.startEpochMillis }
    }

    val nextClassPreview: NextClassPreview? by lazy(LazyThreadSafetyMode.NONE) {
            val active = activeClass ?: return@lazy null
            val sameDayEvents = allEvents
                .filter { it.dateIso == active.dateIso }
                .sortedBy { it.timeSlot.startEpochMillis }
            val next = sameDayEvents.firstOrNull {
                it.timeSlot.startEpochMillis >= active.timeSlot.endEpochMillis && it.id != active.id
            } ?: return@lazy null
            val breakMinutes = ((next.timeSlot.startEpochMillis - active.timeSlot.endEpochMillis) / 60_000L)
                .toInt()
                .coerceAtLeast(0)
            return@lazy NextClassPreview(
                nextEvent = next,
                breakDurationMinutes = breakMinutes
            )
    }

    val primaryStudentGroupLabel: String
        get() {
            val firstGroup = studentGroups
                .firstOrNull()
                ?.groups
                ?.firstOrNull { !it.formName.equals("Tok", ignoreCase = true) }
                ?.groupCode
            return firstGroup ?: currentSession?.courseOfStudyCode ?: "Brak grupy"
        }
}

private fun currentWarsawLocalDate(nowMillis: Long = System.currentTimeMillis()): LocalDate {
    val jt = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.of("Europe/Warsaw")).toLocalDate()
    return LocalDate(jt.year, jt.monthValue, jt.dayOfMonth)
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            savedEncryptedLoginHint = scheduleRepository.getDecryptedSavedLogin()
        )
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        startLiveMinuteTicker()
        observeSessionAndSchedule()
        restoreEncryptedSessionOnStart()
    }

    private fun restoreEncryptedSessionOnStart() {
        viewModelScope.launch {
            val restored = try {
                scheduleRepository.restoreEncryptedSessionIfAvailable()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
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
                    // Interfejs pokazuje czas z dokładnością do minuty, więc stan zmieniamy raz na minutę.
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
            scheduleRepository.currentSession.collect { session ->
                if (session == null) {
                    _uiState.update {
                        ScheduleUiState(
                            savedEncryptedLoginHint = scheduleRepository.getDecryptedSavedLogin(),
                            nowEpochMillis = System.currentTimeMillis(),
                            isSessionRestoring = it.isSessionRestoring,
                            themeMode = it.themeMode,
                            isLiveNotificationEnabled = it.isLiveNotificationEnabled,
                            isHideSensitiveData = it.isHideSensitiveData
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
                    refreshSchedule()
                    loadAllAcademicModules()
                }
            }
        }

        viewModelScope.launch {
            scheduleRepository.observeAllEvents().collect { events ->
                _uiState.update { state ->
                    // If the currently selected date has no events on initial load, but another date in the current week has events, keep selectedDate or snap to active class date
                    val activeEventDate = events.firstOrNull { it.isOngoing(state.nowEpochMillis) }
                        ?.dateIso
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                    val targetDate = if (state.allEvents.isEmpty() && activeEventDate != null) {
                        activeEventDate
                    } else {
                        state.selectedDate
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
                            authError = error.message
                                ?: "Nie udało się zalogować. Sprawdź dane i połączenie z internetem."
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
                    // Domyślnie true — bez tego po wylogowaniu zostawał ekran startowy zamiast logowania.
                    isSessionRestoring = false,
                    themeMode = it.themeMode,
                    isLiveNotificationEnabled = it.isLiveNotificationEnabled,
                    isHideSensitiveData = it.isHideSensitiveData
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
                        onSuccess = { "Hasło zostało zmienione." },
                        onFailure = { err -> err.message ?: "Nie udało się zmienić hasła." }
                    )
                )
            }
        }
    }

    fun selectDay(indexInWeek: Int) {
        val currentDays = _uiState.value.days
        val clickedDay = currentDays.getOrNull(indexInWeek) ?: return
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
            val newDate = LocalDate(ym.year, ym.monthValue, newDay)
            selectDate(newDate)
        } else {
            val prevWeekDate = state.selectedDate.minus(DatePeriod(days = 7))
            selectDate(prevWeekDate)
        }
    }

    fun navigateNextCalendarPeriod() {
        val state = _uiState.value
        if (state.isCalendarExpanded) {
            val ym = YearMonth.of(state.displayedYear, state.displayedMonth).plusMonths(1)
            val newDay = state.selectedDate.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
            val newDate = LocalDate(ym.year, ym.monthValue, newDay)
            selectDate(newDate)
        } else {
            val nextWeekDate = state.selectedDate.plus(DatePeriod(days = 7))
            selectDate(nextWeekDate)
        }
    }

    fun selectScheduleFilter(filterType: ScheduleFilterType) {
        if (_uiState.value.scheduleFilterType == filterType) return
        _uiState.update { it.copy(scheduleFilterType = filterType) }
        refreshSchedule()
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

    fun refreshSchedule() {
        viewModelScope.launch {
            val state = _uiState.value
            val session = state.currentSession ?: return@launch
            val currentFilter = state.scheduleFilterType
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }

            // Fetch a wide 4-month window around the currently displayed month so all weeks and months are populated
            val baseYm = YearMonth.of(state.displayedYear, state.displayedMonth)
            val startYm = baseYm.minusMonths(1)
            val endYm = baseYm.plusMonths(2)
            val start = LocalDate(startYm.year, startYm.monthValue, 1)
            val end = LocalDate(endYm.year, endYm.monthValue, endYm.lengthOfMonth())

            val result = scheduleRepository.syncSchedule(
                startDate = start,
                endDate = end,
                scheduleType = currentFilter,
                forceRefresh = true
            )
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
                            errorMessage = if (session.isDebugAccount) null else err.message
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
                        it.copy(statusBannerMessage = "Otwieram płatność: ${charge.title}")
                    }
                    onOpenUrl(url)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(statusBannerMessage = err.message ?: "Nie udało się rozpocząć płatności.")
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

    fun downloadAttachment(attachment: AnnouncementAttachment) {
        viewModelScope.launch {
            val res = scheduleRepository.downloadAnnouncementAttachment(attachment.id)
            _uiState.update {
                it.copy(
                    statusBannerMessage = res.fold(
                        onSuccess = { att -> "Pobrano załącznik: ${att.fileName}" },
                        onFailure = { err -> err.message ?: "Nie udało się pobrać załącznika." }
                    )
                )
            }
        }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun selectThemeMode(mode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLiveNotificationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isLiveNotificationEnabled = enabled) }
    }

    fun setHideSensitiveData(hidden: Boolean) {
        _uiState.update { it.copy(isHideSensitiveData = hidden) }
    }

    private fun loadAllAcademicModules() {
        viewModelScope.launch {
            // Offline-first: show the last cached modules immediately, then refresh from the network.
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
            coroutineScope {
                val groupsDef = async { scheduleRepository.fetchStudentGroups() }
                val plansDef = async { scheduleRepository.fetchStudyPlans() }
                val gradesDef = async { scheduleRepository.fetchGrades() }
                val scholarshipsDef = async { scheduleRepository.fetchScholarships() }
                val financialsDef = async { scheduleRepository.fetchFinancials() }
                val announcementsDef = async { scheduleRepository.fetchAnnouncements() }

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
}

/**
 * Distinct Internal Debug / Test Account Factory (`Piotr Testowy • Album 99901 • Cyberbezpieczeństwo i AI`).
 *
 * Intentionally uses a completely different field of study, semester (Semestr 5), album number (`99901`),
 * groups (`LAB-CSI-5A`), lecturers, classrooms, and subjects so that the Debug Account is 100%
 * distinguishable from real 1st-year Computer Science (`KI/N`) student accounts.
 */
object SampleWseiScheduleFactory {

    private val cachedDemoAvatarBase64: String by lazy {
        generateDemoStudentPortraitBase64()
    }

    private fun generateDemoStudentPortraitBase64(): String = runCatching {
        val size = 240
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Emerald-Teal studio background
        paint.color = 0xFF005142.toInt()
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Subtle decorative circle
        paint.color = 0xFF006B58.toInt()
        canvas.drawCircle(size * 0.5f, size * 0.45f, size * 0.42f, paint)

        // Stylized student shoulders & head silhouette
        paint.color = 0xFF7BF8D8.toInt()
        canvas.drawOval(RectF(size * 0.18f, size * 0.62f, size * 0.82f, size * 1.18f), paint)
        canvas.drawCircle(size * 0.5f, size * 0.38f, size * 0.19f, paint)

        // Elegant student monogram badge
        paint.color = 0xFF002019.toInt()
        canvas.drawRoundRect(
            RectF(size * 0.28f, size * 0.78f, size * 0.72f, size * 0.94f),
            16f,
            16f,
            paint
        )
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 22f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STUDENT", size * 0.5f, size * 0.885f, paint)

        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }.getOrDefault("")

    fun createDebugStudentSession(): StudentSession = StudentSession(
        userId = 99901L,
        personId = 99901,
        studId = 99901,
        login = "student@wsei.lublin.pl",
        firstName = "Piotr",
        lastName = "Testowy",
        albumNumber = "99901",
        fieldOfStudy = "Cyberbezpieczeństwo i Sztuczna Inteligencja",
        department = "Wydział Transportu i Informatyki",
        courseOfStudyCode = "CSI/S 2026/27 Z",
        studyMode = "stacjonarne",
        obtainedTitle = "inż.",
        specialization = "Bezpieczeństwo Systemów Chmurowych i Modele AI",
        semesterNumber = 5,
        academicYear = 3,
        statusName = "Studiuje",
        averageCurrentSemester = 4.88,
        averagePreviousSemester = 4.79,
        averageGrade = 4.84,
        personPictureBase64 = cachedDemoAvatarBase64,
        allowPasswordChanges = true,
        unreadAnnouncementCount = 2,
        accessToken = "debug-ms-api-token-wsei-99901",
        isDebugAccount = true
    )

    fun createDemoStudentGroups(): List<StudentSemesterGroups> = listOf(
        StudentSemesterGroups(
            semesterId = 5,
            semesterLabel = "Semestr 5 • 2026/27 Z",
            semesterNumber = 5,
            groups = listOf(
                StudentGroupItem(501, "LAB-CSI-5A", "Laboratorium CyberSec", "5A"),
                StudentGroupItem(502, "PROJ-AI-5B", "Projekt Sztuczna Inteligencja", "5B"),
                StudentGroupItem(503, "WYK-CSI-5", "Wykład kierunkowy", "5"),
                StudentGroupItem(504, "REDTEAM-5A", "Ćwiczenia Pentest", "5A"),
                StudentGroupItem(505, "ELRN-RODO-5", "E-learning", "5")
            )
        )
    )

    fun createDemoStudyPlans(): List<StudyPlanOverview> = listOf(
        StudyPlanOverview(
            planId = 99905,
            planName = "CSI/S 2026/27 Z • Cyberbezpieczeństwo i AI (Semestr 5)",
            semesters = listOf(
                StudyPlanSemester(
                    semesterId = 5,
                    semesterName = "Semestr 5 (2026/2027 Z)",
                    semesterNumber = 5,
                    subjects = listOf(
                        StudyPlanSubjectItem(1, 1, "Kryptografia stosowana i analiza zagrożeń", "Wykład + Laboratorium", "Egzamin", 45, 6f, 5),
                        StudyPlanSubjectItem(2, 2, "Głębokie sieci neuronowe i modele językowe", "Wykład + Projekt", "Egzamin", 45, 6f, 5),
                        StudyPlanSubjectItem(3, 3, "Bezpieczeństwo chmury obliczeniowej i DevSecOps", "Wykład + Laboratorium", "Zaliczenie ocena", 30, 5f, 5),
                        StudyPlanSubjectItem(4, 4, "Testy penetracyjne aplikacji mobilnych", "Ćwiczenia", "Zaliczenie ocena", 30, 5f, 5),
                        StudyPlanSubjectItem(5, 5, "Inżynieria wsteczna i analiza złośliwego oprogramowania", "Laboratorium", "Zaliczenie ocena", 30, 5f, 5),
                        StudyPlanSubjectItem(6, 6, "Prawo nowych technologii i ochrona danych (AI Act)", "E-learning", "Zaliczenie", 15, 3f, 5)
                    )
                )
            )
        )
    )

    /**
     * Generates a multi-month demo schedule anchored to `nowMillis` so that:
     * 1. Today ALWAYS has an active 4-block day (`Zajęcia -> Przerwa 15m -> Zajęcia (TRWA TERAZ) -> Przerwa 15m -> Zajęcia -> Przerwa 10m -> Zajęcia`)
     *    for testing the Live Notification and Start Screen progress bar!
     * 2. Multiple days in the current week, previous/next weeks, and adjacent months have classes
     *    so calendar navigation (days, weeks, months) can be thoroughly tested.
     */
    fun createDemoAcademicCalendarEvents(
        nowMillis: Long,
        scheduleType: ScheduleFilterType = ScheduleFilterType.MY_PLAN
    ): List<ClassEvent> {
        val minuteMs = 60_000L
        val today = currentWarsawLocalDate(nowMillis)
        val todayIso = today.toString()

        // Active day timeline:
        // Block 1 (finished): ended 15m before Block 2
        // Break 1: 15 min
        // Block 2 (ONGOING NOW): 38 min elapsed out of 90 min
        // Break 2: 15 min
        // Block 3 (upcoming): 90 min
        // Break 3: 10 min
        // Block 4 (upcoming): 90 min
        val activeStart = nowMillis - (38 * minuteMs)
        val activeEnd = activeStart + (90 * minuteMs)

        val prevStart = activeStart - (105 * minuteMs)
        val prevEnd = activeStart - (15 * minuteMs)

        val nextStart = activeEnd + (15 * minuteMs)
        val nextEnd = nextStart + (90 * minuteMs)

        val lastStart = nextEnd + (10 * minuteMs)
        val lastEnd = lastStart + (90 * minuteMs)

        val groupLabel = if (scheduleType == ScheduleFilterType.MY_GROUP_PLAN) {
            "LAB-CSI-5A + 5B (Rocznik 3)"
        } else {
            "LAB-CSI-5A"
        }

        val events = mutableListOf<ClassEvent>()

        // Today's 4 blocks (for live progress & notification testing)
        events += listOf(
            ClassEvent(
                id = "demo_${todayIso}_1",
                dateIso = todayIso,
                subject = Subject("krypto_5", "Kryptografia stosowana i analiza zagrożeń", "KRYP", 6),
                classType = ClassType.LAB,
                lecturer = Lecturer("d1", "dr hab. inż.", "Wiktor", "Bezpieczny"),
                room = Room("312C (Lab Cyber)", "Kampus WSEI • Projektowa 4", floorLabel = "III piętro"),
                timeSlot = TimeSlot(prevStart, prevEnd, "09:45", "11:15"),
                groupCode = groupLabel,
                deliveryMode = DeliveryMode.ON_SITE,
                passingForm = "Egzamin",
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            ),
            ClassEvent(
                id = "demo_${todayIso}_2_active",
                dateIso = todayIso,
                subject = Subject("ai_llm_5", "Głębokie sieci neuronowe i modele językowe", "AI-LLM", 6),
                classType = ClassType.PROJECT,
                lecturer = Lecturer("d2", "prof. dr hab.", "Joanna", "Sztuczna"),
                room = Room("408A (AI Lab)", "Kampus WSEI • Projektowa 4", floorLabel = "IV piętro"),
                timeSlot = TimeSlot(activeStart, activeEnd, "11:30", "13:00"),
                groupCode = groupLabel,
                deliveryMode = DeliveryMode.ON_SITE,
                passingForm = "Egzamin",
                notes = "Projekt zaawansowanych modeli transformatorowych i LLM.",
                incidentAlert = null,
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            ),
            ClassEvent(
                id = "demo_${todayIso}_3_next",
                dateIso = todayIso,
                subject = Subject("cloud_sec_5", "Bezpieczeństwo chmury obliczeniowej i DevSecOps", "CLOUD", 5),
                classType = ClassType.LECTURE,
                lecturer = Lecturer("d3", "dr inż.", "Kamil", "Chmurowy"),
                room = Room("Aula B2", "Kampus WSEI • Projektowa 4", floorLabel = "I piętro"),
                timeSlot = TimeSlot(nextStart, nextEnd, "13:15", "14:45"),
                groupCode = "WYK-CSI-5",
                deliveryMode = DeliveryMode.ON_SITE,
                passingForm = "Zaliczenie ocena",
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            ),
            ClassEvent(
                id = "demo_${todayIso}_4",
                dateIso = todayIso,
                subject = Subject("pentest_mob_5", "Testy penetracyjne aplikacji mobilnych", "PENT", 5),
                classType = ClassType.EXERCISE,
                lecturer = Lecturer("d4", "mgr inż.", "Alicja", "Hakerska"),
                room = Room("314C (RedTeam)", "Kampus WSEI • Projektowa 4", floorLabel = "III piętro"),
                timeSlot = TimeSlot(lastStart, lastEnd, "14:55", "16:25"),
                groupCode = "REDTEAM-5A",
                deliveryMode = DeliveryMode.ON_SITE,
                passingForm = "Zaliczenie ocena",
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            )
        )

        // Additional classes across other days, weeks, and adjacent months for calendar navigation testing
        val otherOffsets = listOf(-28, -21, -14, -7, -2, 1, 3, 7, 14, 21, 28, 35)
        otherOffsets.forEachIndexed { idx, dayOffset ->
            val d = today.plus(DatePeriod(days = dayOffset))
            val dIso = d.toString()
            val baseMs = nowMillis + (dayOffset * 86_400_000L)
            events += ClassEvent(
                id = "demo_extra_${dIso}_a_$idx",
                dateIso = dIso,
                subject = Subject("rev_eng_5", "Inżynieria wsteczna i analiza złośliwego oprogramowania", "REV", 5),
                classType = ClassType.LAB,
                lecturer = Lecturer("d5", "dr inż.", "Robert", "Analityk"),
                room = Room("312C (Lab Cyber)", "Kampus WSEI • Projektowa 4"),
                timeSlot = TimeSlot(baseMs, baseMs + 90 * minuteMs, "10:00", "11:30"),
                groupCode = groupLabel,
                deliveryMode = DeliveryMode.ON_SITE,
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            )
            events += ClassEvent(
                id = "demo_extra_${dIso}_b_$idx",
                dateIso = dIso,
                subject = Subject("ai_law_5", "Prawo nowych technologii i ochrona danych (AI Act)", "RODO", 3),
                classType = ClassType.ELEARNING,
                lecturer = Lecturer("d6", "dr", "Tomasz", "Prawniczy"),
                room = Room("E-learning", "Platforma WSEI Online", isRemoteTeams = true),
                timeSlot = TimeSlot(baseMs + 105 * minuteMs, baseMs + 195 * minuteMs, "11:45", "13:15"),
                groupCode = "ELRN-RODO-5",
                deliveryMode = DeliveryMode.ELEARNING,
                origin = DataSourceOrigin.DEBUG_TEST_ACCOUNT
            )
        }

        return events.sortedBy { it.timeSlot.startEpochMillis }
    }

    fun createDemoWeek(
        nowMillis: Long,
        scheduleType: ScheduleFilterType = ScheduleFilterType.MY_PLAN
    ): List<AcademicDay> {
        val all = createDemoAcademicCalendarEvents(nowMillis, scheduleType)
        val today = currentWarsawLocalDate(nowMillis)
        val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val byDate = all.groupBy { it.dateIso }
        return (0..6).map { offset ->
            val date = monday.plus(DatePeriod(days = offset))
            AcademicDay(
                date = date,
                dayOfWeekLabel = date.dayOfWeek.toPolishFullName(),
                shortDayLabel = date.dayOfWeek.toPolishShortName(),
                dayOfMonthNumber = date.dayOfMonth.toString().padStart(2, '0'),
                events = byDate[date.toString()].orEmpty(),
                isWeekendExtramuralSession = date.dayOfWeek == DayOfWeek.SATURDAY ||
                    date.dayOfWeek == DayOfWeek.SUNDAY
            )
        }
    }

    fun createDemoGrades(): List<GradeItem> = listOf(
        GradeItem(
            id = "dg1",
            subjectId = 501,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Kryptografia stosowana i analiza zagrożeń",
            moduleName = "Moduł Cyberbezpieczeństwo",
            classType = ClassType.LAB,
            formLabel = "Wykład + Laboratorium",
            gradeValue = "5.0",
            partialGrades = listOf("5.0", "5.0"),
            passingForm = "Egzamin",
            ects = 6,
            hours = 45,
            lecturerName = "dr hab. inż. Wiktor Bezpieczny",
            dateAwarded = "05.11.2026"
        ),
        GradeItem(
            id = "dg2",
            subjectId = 502,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Głębokie sieci neuronowe i modele językowe",
            moduleName = "Moduł Sztuczna Inteligencja",
            classType = ClassType.PROJECT,
            formLabel = "Wykład + Projekt",
            gradeValue = "5.0",
            partialGrades = listOf("5.0", "4.5"),
            passingForm = "Egzamin",
            ects = 6,
            hours = 45,
            lecturerName = "prof. dr hab. Joanna Sztuczna",
            dateAwarded = "06.11.2026"
        ),
        GradeItem(
            id = "dg3",
            subjectId = 503,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Bezpieczeństwo chmury obliczeniowej i DevSecOps",
            moduleName = "Moduł Cloud & Security",
            classType = ClassType.LECTURE,
            formLabel = "Wykład + Laboratorium",
            gradeValue = "4.5",
            partialGrades = listOf("4.5"),
            passingForm = "Zaliczenie ocena",
            ects = 5,
            hours = 30,
            lecturerName = "dr inż. Kamil Chmurowy",
            dateAwarded = "02.11.2026"
        ),
        GradeItem(
            id = "dg4",
            subjectId = 504,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Testy penetracyjne aplikacji mobilnych",
            moduleName = "Moduł RedTeam",
            classType = ClassType.EXERCISE,
            formLabel = "Ćwiczenia",
            gradeValue = "5.0",
            partialGrades = listOf("5.0"),
            passingForm = "Zaliczenie ocena",
            ects = 5,
            hours = 30,
            lecturerName = "mgr inż. Alicja Hakerska",
            dateAwarded = "29.10.2026"
        ),
        GradeItem(
            id = "dg5",
            subjectId = 505,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Inżynieria wsteczna i analiza złośliwego oprogramowania",
            moduleName = "Moduł Cyberbezpieczeństwo",
            classType = ClassType.LAB,
            formLabel = "Laboratorium",
            gradeValue = "4.5",
            partialGrades = listOf("4.5"),
            passingForm = "Zaliczenie ocena",
            ects = 5,
            hours = 30,
            lecturerName = "dr inż. Robert Analityk",
            dateAwarded = "25.10.2026"
        ),
        GradeItem(
            id = "dg6",
            subjectId = 506,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            semesterNumber = 5,
            subjectName = "Prawo nowych technologii i ochrona danych (AI Act)",
            moduleName = "Moduł Prawny",
            classType = ClassType.ELEARNING,
            formLabel = "E-learning",
            gradeValue = "ZAL",
            partialGrades = emptyList(),
            passingForm = "Zaliczenie",
            ects = 3,
            hours = 15,
            lecturerName = "dr Tomasz Prawniczy",
            dateAwarded = "20.10.2026"
        )
    )

    fun createDemoScholarships(): List<ScholarshipApplication> = listOf(
        ScholarshipApplication(
            id = 991,
            semesterLabel = "Semestr 5 • 2026/2027 Z",
            number = 1,
            title = "Stypendium Rektora za wyniki w nauce",
            typeVariantName = "Stypendium Rektora – Kategoria I (Średnia 4.84 + Koło Naukowe CyberSec)",
            submissionDateIso = "05.10.2026",
            statusString = "Rozpatrzony pozytywnie (Przyznane)",
            isGranted = true,
            decisionNumber = "SR/WSEI/99901/2026",
            decisionDateIso = "28.10.2026",
            acceptedRules = true,
            payouts = listOf(
                ScholarshipPayoutItem(1, "1 450.00 PLN", "25.11.2026", "Przekazano do wypłaty"),
                ScholarshipPayoutItem(2, "1 450.00 PLN", "20.12.2026", "Zatwierdzone"),
                ScholarshipPayoutItem(3, "1 450.00 PLN", "25.01.2027", "Zatwierdzone")
            )
        )
    )

    fun createDemoFinancials(): List<FinancialObligation> {
        val today = currentWarsawLocalDate()
        val currentMonthStr = today.monthNumber.toString().padStart(2, '0')
        val currentYearStr = today.year.toString()
        val nextMonthDate = today.plus(DatePeriod(months = 1))
        val nextMonthStr = nextMonthDate.monthNumber.toString().padStart(2, '0')
        val nextMonthYearStr = nextMonthDate.year.toString()
        return listOf(
            FinancialObligation(
                id = "df1",
                chargeId = 9901,
                title = "CSI/5/Z Czesne – Rata bieżąca ($currentMonthStr.$currentYearStr)",
                amountPln = 720.00,
                amountFormatted = "720.00 PLN",
                currentStatePln = -720.00,
                dueDateIso = "25.$currentMonthStr.$currentYearStr",
                paymentDateIso = null,
                statusLabel = "Do uregulowania w tym miesiącu",
                interestFormatted = "0.00 PLN",
                accumulatedInterestFormatted = "0.00 PLN",
                amortizationFormatted = "0.00 PLN",
                discountFormatted = "0.00 PLN",
                expiredFormatted = "0.00 PLN",
                isPaid = false,
                isExpired = false,
                payOnlineAvailable = true,
                bankAccountNumber = "11 1020 3147 0000 9990 1000 9901"
            ),
            FinancialObligation(
                id = "df3",
                chargeId = 9903,
                title = "CSI/5/Z Czesne – Kolejna rata ($nextMonthStr.$nextMonthYearStr)",
                amountPln = 720.00,
                amountFormatted = "720.00 PLN",
                currentStatePln = -720.00,
                dueDateIso = "25.$nextMonthStr.$nextMonthYearStr",
                paymentDateIso = null,
                statusLabel = "Przyszła rata czesnego",
                interestFormatted = "0.00 PLN",
                accumulatedInterestFormatted = "0.00 PLN",
                amortizationFormatted = "0.00 PLN",
                discountFormatted = "0.00 PLN",
                expiredFormatted = "0.00 PLN",
                isPaid = false,
                isExpired = false,
                payOnlineAvailable = true,
                bankAccountNumber = "11 1020 3147 0000 9990 1000 9901"
            ),
            FinancialObligation(
                id = "df2",
                chargeId = 9902,
                title = "CSI/5/Z Czesne – Rata październikowa (10.$currentYearStr)",
                amountPln = 720.00,
                amountFormatted = "720.00 PLN",
                currentStatePln = 0.00,
                dueDateIso = "15.10.$currentYearStr",
                paymentDateIso = "10.10.$currentYearStr",
                statusLabel = "Opłacono",
                interestFormatted = "0.00 PLN",
                accumulatedInterestFormatted = "0.00 PLN",
                amortizationFormatted = "0.00 PLN",
                discountFormatted = "0.00 PLN",
                expiredFormatted = "0.00 PLN",
                isPaid = true,
                isExpired = false,
                payOnlineAvailable = false,
                bankAccountNumber = "11 1020 3147 0000 9990 1000 9901"
            )
        )
    }

    fun createDemoAnnouncements(): List<DeanAnnouncement> = listOf(
        DeanAnnouncement(
            id = "da1",
            announcementId = 9501,
            title = "Hackathon CyberSecurity & AI – Rejestracja zespołów",
            bodySnippet = "Studenci specjalności Cyberbezpieczeństwo i Sztuczna Inteligencja mogą zgłaszać zespoły do akademickiego turnieju CTF.",
            fullText = "Komunikat Dziekanatu: Studenci specjalności Cyberbezpieczeństwo i Sztuczna Inteligencja (grupa LAB-CSI-5A) mogą zgłaszać zespoły do akademickiego turnieju Capture The Flag (CTF) w laboratorium 312C.",
            publishedAtIso = "06.11.2026",
            priorityLevel = 0,
            isImportant = true,
            isRead = false,
            hasAttachment = true,
            attachments = listOf(
                AnnouncementAttachment(
                    id = 9801,
                    fileName = "Regulamin_Turnieju_CTF_WSEI.pdf"
                )
            ),
            authorDepartment = "Centrum Cyberbezpieczeństwa",
            senderInitial = "C"
        ),
        DeanAnnouncement(
            id = "da2",
            announcementId = 9502,
            title = "Dostęp do klastra GPU w laboratorium 408A",
            bodySnippet = "Przyznano zasoby obliczeniowe dla grupy projektowej PROJ-AI-5B na potrzeby trenowania modeli językowych.",
            fullText = "Studenci grupy PROJ-AI-5B posiadają aktywny dostęp zdalny VPN do uczelnianego klastra obliczeniowego w laboratorium 408A.",
            publishedAtIso = "28.10.2026",
            priorityLevel = 1,
            isImportant = false,
            isRead = true,
            hasAttachment = false,
            attachments = emptyList(),
            authorDepartment = "Laboratorium Sztucznej Inteligencji",
            senderInitial = "L"
        )
    )
}
