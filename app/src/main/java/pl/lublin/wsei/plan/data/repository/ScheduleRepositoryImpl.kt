package pl.lublin.wsei.plan.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.SerializationException
import pl.lublin.wsei.plan.data.local.AcademicSnapshotStore
import pl.lublin.wsei.plan.data.local.EncryptedCredentialStore
import pl.lublin.wsei.plan.data.local.SnapshotKey
import pl.lublin.wsei.plan.data.local.ScheduleDao
import pl.lublin.wsei.plan.data.local.toEntity
import pl.lublin.wsei.plan.data.remote.api.ArpApiDataSource
import pl.lublin.wsei.plan.data.remote.api.AprHttpException
import pl.lublin.wsei.plan.data.remote.api.AprMalformedResponseException
import pl.lublin.wsei.plan.data.remote.api.AprServerException
import pl.lublin.wsei.plan.data.remote.api.AprSessionExpiredException
import pl.lublin.wsei.plan.data.remote.api.ArpSessionCookieJar
import pl.lublin.wsei.plan.data.remote.api.ArpTokenRefresher
import pl.lublin.wsei.plan.data.remote.html.WdHtmlDataSource
import pl.lublin.wsei.plan.domain.model.AcademicDay
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.StudentSemesterGroups
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.model.StudyPlanOverview
import pl.lublin.wsei.plan.domain.repository.ActiveClassSnapshot
import pl.lublin.wsei.plan.domain.repository.CachedAcademicModules
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import pl.lublin.wsei.plan.domain.repository.SyncReport
import pl.lublin.wsei.plan.service.ClassAlarmScheduler
import pl.lublin.wsei.plan.service.ScheduleSyncScheduler
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException
import pl.lublin.wsei.plan.ui.schedule.SampleWseiScheduleFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository — Room is the single source of truth:
 *
 * - **Timetable**: UI observes Room (`observeAcademicDays`, `observeAllEvents`); [syncSchedule] fetches
 *   from the REST API ([ArpApiDataSource]) and, when the API itself is unavailable/broken, from the web
 *   portal parser ([WdHtmlDataSource]); the window is replaced atomically.
 * - **Grades, finances, announcements, groups, plans, scholarships**: network first, persisted as
 *   snapshots; on failure the last snapshot is returned, so screens work offline.
 * - **Session**: silent re-login is done by [ArpTokenRefresher]; the refreshed token is propagated to
 *   [currentSession] and the encrypted vault.
 * - **Background**: [ScheduleSyncScheduler] keeps a periodic WorkManager sync while logged in.
 *
 * Internal Debug Test Account (`debug@wsei.lublin.pl` / `99901` + `debug123`) stays isolated.
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val arpApiDataSource: ArpApiDataSource,
    private val htmlDataSource: WdHtmlDataSource,
    private val sessionCookieJar: ArpSessionCookieJar,
    private val tokenRefresher: ArpTokenRefresher,
    private val scheduleDao: ScheduleDao,
    private val snapshotStore: AcademicSnapshotStore,
    private val encryptedCredentialStore: EncryptedCredentialStore,
    private val alarmScheduler: ClassAlarmScheduler,
    private val syncScheduler: ScheduleSyncScheduler
) : ScheduleRepository {

    private val _currentSession = MutableStateFlow<StudentSession?>(null)
    override val currentSession: StateFlow<StudentSession?> = _currentSession.asStateFlow()

    companion object {
        private val DEBUG_LOGINS = setOf("debug", "debug@wsei.lublin.pl", "99901", "test")
        private const val DEBUG_PASSWORD = "debug123"
    }

    init {
        tokenRefresher.onTokenRefreshed = { auth ->
            val token = auth.accessToken.orEmpty()
            val refreshed = _currentSession.value?.copy(accessToken = token)
            if (refreshed != null && !refreshed.isDebugAccount) {
                _currentSession.update { it?.copy(accessToken = token) }
                val login = sessionCookieJar.savedLogin
                val password = sessionCookieJar.savedPassword
                if (login != null && password != null && encryptedCredentialStore.isRememberMeEnabled()) {
                    encryptedCredentialStore.saveEncryptedCredentials(login, password, refreshed, rememberCredentials = true)
                }
            }
        }
    }

    override fun getDecryptedSavedLogin(): String? {
        return encryptedCredentialStore.loadDecryptedCredentials()?.login
    }

    override suspend fun restoreEncryptedSessionIfAvailable(): StudentSession? {
        val vault = encryptedCredentialStore.loadDecryptedCredentials() ?: return null
        if (vault.isDebugAccount) {
            return loginWithDebugTestAccount(rememberCredentials = true)
        }
        // Szybki start: gdy mamy zaszyfrowaną sesję, pokazujemy ją od razu (bez czekania na sieć);
        // token odświeża w tle ArpAutoReauthInterceptor na podstawie zapisanych poświadczeń.
        vault.savedSession?.let { cached ->
            sessionCookieJar.bearerToken = cached.accessToken
            sessionCookieJar.savedLogin = vault.login
            sessionCookieJar.savedPassword = vault.password
            _currentSession.value = cached
            syncScheduler.schedulePeriodic()
            return cached
        }
        // Try silent re-authentication with decrypted credentials for fresh token & photo
        val liveAuth = arpApiDataSource.authenticate(
            usernameOrAlbum = vault.login,
            password = vault.password
        )
        if (liveAuth.isSuccess) {
            val freshSession = liveAuth.getOrThrow()
            encryptedCredentialStore.saveEncryptedCredentials(
                login = vault.login,
                password = vault.password,
                session = freshSession,
                rememberCredentials = true
            )
            _currentSession.value = freshSession
            syncScheduler.schedulePeriodic()
            return freshSession
        }
        // Offline fallback using encrypted cached session
        val cachedSession = vault.savedSession ?: return null
        sessionCookieJar.bearerToken = cachedSession.accessToken
        sessionCookieJar.savedLogin = vault.login
        sessionCookieJar.savedPassword = vault.password
        _currentSession.value = cachedSession
        return cachedSession
    }

    override suspend fun loginWithCredentials(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String?,
        rememberCredentials: Boolean
    ): Result<StudentSession> {
        val cleanLogin = usernameOrAlbum.trim()
        if (cleanLogin.lowercase() in DEBUG_LOGINS && (password == DEBUG_PASSWORD || password == "debug")) {
            return Result.success(loginWithDebugTestAccount(rememberCredentials = rememberCredentials))
        }

        val previousStudId = _currentSession.value?.studId
            ?: encryptedCredentialStore.loadDecryptedCredentials()?.savedSession?.studId
        sessionCookieJar.clearSession()
        htmlDataSource.clearSession()

        val wdResult = arpApiDataSource.authenticate(
            usernameOrAlbum = cleanLogin,
            password = password,
            twoFactorToken = twoFactorToken
        )

        if (wdResult.isSuccess) {
            val session = wdResult.getOrThrow()
            // Strict account isolation: wipe another student's cache only once the new login succeeded
            // (a failed login used to destroy the offline cache).
            if (previousStudId == null || previousStudId != session.studId) {
                scheduleDao.clearAllEvents()
                snapshotStore.clearAll()
            }
            encryptedCredentialStore.saveEncryptedCredentials(
                login = cleanLogin,
                password = password,
                session = session,
                rememberCredentials = rememberCredentials
            )
            _currentSession.value = session
            syncScheduler.schedulePeriodic()
        }

        return wdResult
    }

    override suspend fun loginWithDebugTestAccount(
        rememberCredentials: Boolean
    ): StudentSession {
        sessionCookieJar.clearSession()
        htmlDataSource.clearSession()
        scheduleDao.clearAllEvents()
        snapshotStore.clearAll()
        syncScheduler.cancelAll()

        val debugSession = SampleWseiScheduleFactory.createDebugStudentSession()
        encryptedCredentialStore.saveEncryptedCredentials(
            login = debugSession.login,
            password = DEBUG_PASSWORD,
            session = debugSession,
            rememberCredentials = rememberCredentials
        )
        _currentSession.value = debugSession

        val demoEvents = SampleWseiScheduleFactory.createDemoAcademicCalendarEvents(
            nowMillis = System.currentTimeMillis()
        )
        scheduleDao.upsertEvents(demoEvents.map { it.toEntity(ownerStudId = debugSession.studId) })
        alarmScheduler.scheduleUpcomingClassAlarms(demoEvents)
        return debugSession
    }

    override suspend fun logout() {
        syncScheduler.cancelAll()
        sessionCookieJar.clearSession()
        htmlDataSource.clearSession()
        encryptedCredentialStore.clearVault()
        scheduleDao.clearAllEvents()
        snapshotStore.clearAll()
        _currentSession.value = null
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val session = _currentSession.value ?: return Result.failure(IllegalStateException("Nie jesteś zalogowany."))
        if (session.isDebugAccount) {
            return Result.success(Unit)
        }
        val res = arpApiDataSource.changePassword(currentPassword, newPassword)
        if (res.isSuccess && encryptedCredentialStore.isRememberMeEnabled()) {
            encryptedCredentialStore.saveEncryptedCredentials(
                login = session.login,
                password = newPassword,
                session = session,
                rememberCredentials = true
            )
        }
        return res
    }

    override fun observeAcademicDays(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<AcademicDay>> {
        return scheduleDao.observeEventsBetween(
            startDateIso = startDate.toString(),
            endDateIso = endDate.toString()
        ).map { entities ->
            val currentStudId = _currentSession.value?.studId
            val domainEvents = entities
                .filter { currentStudId == null || it.ownerStudId == 0 || it.ownerStudId == currentStudId }
                .map { it.toDomain() }
            val groupedByDate = domainEvents.groupBy { it.dateIso }

            buildDateRange(startDate, endDate).map { date ->
                val dayEvents = groupedByDate[date.toString()].orEmpty()
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
    }

    override fun observeAllEvents(): Flow<List<ClassEvent>> {
        return scheduleDao.observeAllEvents().map { entities ->
            val currentStudId = _currentSession.value?.studId
            entities
                .filter { currentStudId == null || it.ownerStudId == 0 || it.ownerStudId == currentStudId }
                .map { it.toDomain() }
        }
    }

    override fun observeActiveClassState(nowEpochMillis: Long): Flow<ActiveClassSnapshot?> {
        return scheduleDao.observeActiveEvent(nowEpochMillis).map { activeEntity ->
            val active = activeEntity?.toDomain() ?: return@map null
            val next = scheduleDao.getNextEventOnDay(
                dateIso = active.dateIso,
                afterEpochMillis = active.timeSlot.endEpochMillis,
                excludeId = active.id
            )?.toDomain()

            val breakMinutes = next?.let {
                ((it.timeSlot.startEpochMillis - active.timeSlot.endEpochMillis) / 60_000L)
                    .toInt()
                    .coerceAtLeast(0)
            }

            ActiveClassSnapshot(
                activeEvent = active,
                nextEvent = next,
                breakMinutesUntilNext = breakMinutes
            )
        }
    }

    override suspend fun syncSchedule(
        startDate: LocalDate,
        endDate: LocalDate,
        scheduleType: ScheduleFilterType,
        forceRefresh: Boolean
    ): Result<SyncReport> = runCatching {
        val session = _currentSession.value
            ?: error("Nie jesteś zalogowany.")

        if (session.isDebugAccount) {
            val demoEvents = SampleWseiScheduleFactory.createDemoAcademicCalendarEvents(
                nowMillis = System.currentTimeMillis(),
                scheduleType = scheduleType
            )
            scheduleDao.clearAllEvents()
            scheduleDao.upsertEvents(demoEvents.map { it.toEntity(ownerStudId = session.studId) })
            alarmScheduler.scheduleUpcomingClassAlarms(demoEvents)
            return@runCatching SyncReport(
                syncedEventsCount = demoEvents.size,
                usedOrigin = DataSourceOrigin.DEBUG_TEST_ACCOUNT,
                syncedAtEpochMillis = System.currentTimeMillis()
            )
        }

        val (events, origin) = fetchScheduleWithFallback(session, startDate, endDate, scheduleType)

        scheduleDao.replaceEventsInRange(
            ownerStudId = session.studId,
            startDateIso = startDate.toString(),
            endDateIso = endDate.toString(),
            events = events.map { it.toEntity(ownerStudId = session.studId) }
        )
        alarmScheduler.scheduleUpcomingClassAlarms(events)

        SyncReport(
            syncedEventsCount = events.size,
            usedOrigin = origin,
            syncedAtEpochMillis = System.currentTimeMillis()
        )
    }

    /**
     * REST API first. Falls back to the HTML portal only when the API itself is the problem
     * (5xx/404, changed JSON, port 20830 blocked/timing out, silent re-auth rejected) and credentials
     * are available. Plain "no internet" (UnknownHost) is not retried via HTML.
     */
    private suspend fun fetchScheduleWithFallback(
        session: StudentSession,
        startDate: LocalDate,
        endDate: LocalDate,
        scheduleType: ScheduleFilterType
    ): Pair<List<ClassEvent>, DataSourceOrigin> {
        val apiResult = arpApiDataSource.fetchSchedule(session, startDate, endDate, scheduleType)
        apiResult.getOrNull()?.let { return it to DataSourceOrigin.APR_WD_API }

        val apiError = apiResult.exceptionOrNull() ?: IllegalStateException("Nieznany błąd API.")
        if (!shouldUseHtmlFallback(apiError) || !sessionCookieJar.hasCredentials ||
            scheduleType != ScheduleFilterType.MY_PLAN
        ) {
            throw apiError
        }
        val htmlResult = htmlDataSource.fetchSchedule(session, startDate, endDate, scheduleType)
        htmlResult.getOrNull()?.let { return it to DataSourceOrigin.HTML_FALLBACK }
        htmlResult.exceptionOrNull()?.let { apiError.addSuppressed(it) }
        throw apiError
    }

    private fun shouldUseHtmlFallback(error: Throwable): Boolean = when (error) {
        is AprServerException -> true
        is AprMalformedResponseException -> true
        is SerializationException -> true
        is AprHttpException -> error.statusCode == 404 || error.statusCode >= 500 || error.statusCode in 300..399
        is AprSessionExpiredException -> true
        is ConnectException, is SocketTimeoutException, is SSLException -> true
        else -> false
    }

    // ---------------------------------------------------------------------------------------------
    // Academic modules — network first, Room snapshot as offline fallback
    // ---------------------------------------------------------------------------------------------

    private suspend fun <T> networkWithSnapshot(
        key: SnapshotKey<T>,
        fetch: suspend (StudentSession) -> Result<T>
    ): Result<T> {
        val session = _currentSession.value ?: return Result.failure(IllegalStateException("Nie jesteś zalogowany."))
        val remote = fetch(session)
        remote.getOrNull()?.let { value ->
            runCatching { snapshotStore.save(session.studId, key, value) }
            return remote
        }
        val cached = snapshotStore.load(session.studId, key) ?: return remote
        return Result.success(cached.value)
    }

    override suspend fun loadCachedAcademicModules(): CachedAcademicModules {
        val session = _currentSession.value ?: return CachedAcademicModules()
        if (session.isDebugAccount) return CachedAcademicModules()
        val id = session.studId
        val groups = snapshotStore.load(id, SnapshotKey.Groups)
        val plans = snapshotStore.load(id, SnapshotKey.StudyPlans)
        val grades = snapshotStore.load(id, SnapshotKey.Grades)
        val scholarships = snapshotStore.load(id, SnapshotKey.Scholarships)
        val finances = snapshotStore.load(id, SnapshotKey.Finances)
        val announcements = snapshotStore.load(id, SnapshotKey.Announcements)
        return CachedAcademicModules(
            studentGroups = groups?.value,
            studyPlans = plans?.value,
            grades = grades?.value,
            scholarships = scholarships?.value,
            financials = finances?.value,
            announcements = announcements?.value,
            lastUpdatedEpochMillis = listOfNotNull(groups, plans, grades, scholarships, finances, announcements)
                .maxOfOrNull { it.updatedAtEpochMillis }
        )
    }

    override suspend fun refreshAcademicSnapshots(): Boolean {
        val session = _currentSession.value ?: return false
        if (session.isDebugAccount) return true
        val results = listOf(
            fetchStudentGroups().isSuccess,
            fetchStudyPlans().isSuccess,
            fetchGrades().isSuccess,
            fetchScholarships().isSuccess,
            fetchFinancials().isSuccess,
            fetchAnnouncements().isSuccess
        )
        return results.all { it }
    }

    override suspend fun fetchStudentGroups(): Result<List<StudentSemesterGroups>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoStudentGroups())
        }
        return networkWithSnapshot(SnapshotKey.Groups) { arpApiDataSource.fetchStudentGroups(it) }
    }

    override suspend fun fetchStudyPlans(): Result<List<StudyPlanOverview>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoStudyPlans())
        }
        return networkWithSnapshot(SnapshotKey.StudyPlans) { arpApiDataSource.fetchStudyPlans(it) }
    }

    override suspend fun fetchGrades(): Result<List<GradeItem>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoGrades())
        }
        return networkWithSnapshot(SnapshotKey.Grades) { arpApiDataSource.fetchGrades(it) }
    }

    override suspend fun fetchScholarships(): Result<List<ScholarshipApplication>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoScholarships())
        }
        return networkWithSnapshot(SnapshotKey.Scholarships) { arpApiDataSource.fetchScholarships(it) }
    }

    override suspend fun fetchFinancials(): Result<List<FinancialObligation>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoFinancials())
        }
        return networkWithSnapshot(SnapshotKey.Finances) { arpApiDataSource.fetchFinancials(it) }
    }

    override suspend fun fetchOnlinePaymentUrl(
        chargeId: Int,
        amountPln: Double
    ): Result<String> {
        val session = _currentSession.value ?: return Result.failure(IllegalStateException("Nie jesteś zalogowany."))
        if (session.isDebugAccount) {
            return Result.success("https://platnosci.bm.pl/wsei-lublin?chargeId=$chargeId&price=$amountPln")
        }
        return arpApiDataSource.fetchOnlinePaymentUrl(chargeId, amountPln)
    }

    override suspend fun fetchAnnouncements(): Result<List<DeanAnnouncement>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoAnnouncements())
        }
        return networkWithSnapshot(SnapshotKey.Announcements) { arpApiDataSource.fetchAnnouncements(it) }
    }

    override suspend fun markAnnouncementRead(announcementId: Int): Result<Unit> {
        val session = _currentSession.value ?: return Result.success(Unit)
        if (session.isDebugAccount) {
            return Result.success(Unit)
        }
        val result = arpApiDataSource.markAnnouncementRead(session, announcementId)
        if (result.isSuccess) {
            snapshotStore.load(session.studId, SnapshotKey.Announcements)?.let { cached ->
                snapshotStore.save(
                    session.studId,
                    SnapshotKey.Announcements,
                    cached.value.map { if (it.announcementId == announcementId) it.copy(isRead = true) else it }
                )
            }
        }
        return result
    }

    override suspend fun downloadAnnouncementAttachment(
        attachmentId: Int
    ): Result<AnnouncementAttachment> {
        val session = _currentSession.value ?: return Result.failure(IllegalStateException("Nie jesteś zalogowany."))
        if (session.isDebugAccount) {
            return Result.success(
                AnnouncementAttachment(
                    id = attachmentId,
                    fileName = "Regulamin_Laboratorium_CyberSec_AI_DEMO.pdf",
                    fileBytesBase64 = "JVBERi0xLjQK"
                )
            )
        }
        return arpApiDataSource.downloadAnnouncementAttachment(session, attachmentId)
    }

    override suspend fun getEventById(eventId: String): ClassEvent? {
        return scheduleDao.getEventById(eventId)?.toDomain()
    }

    override suspend fun getEventsOnDay(dateIso: String): List<ClassEvent> {
        return scheduleDao.getEventsOnDay(dateIso).map { it.toDomain() }
    }

    override suspend fun getNextEventAfter(eventId: String): ClassEvent? {
        val current = getEventById(eventId) ?: return null
        return scheduleDao.getNextEventOnDay(
            dateIso = current.dateIso,
            afterEpochMillis = current.timeSlot.endEpochMillis,
            excludeId = current.id
        )?.toDomain()
    }

    private fun buildDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= end) {
            dates += cursor
            cursor = cursor.plus(DatePeriod(days = 1))
        }
        return dates
    }
}

fun DayOfWeek.toPolishFullName(): String = when (this) {
    DayOfWeek.MONDAY -> "Poniedziałek"
    DayOfWeek.TUESDAY -> "Wtorek"
    DayOfWeek.WEDNESDAY -> "Środa"
    DayOfWeek.THURSDAY -> "Czwartek"
    DayOfWeek.FRIDAY -> "Piątek"
    DayOfWeek.SATURDAY -> "Sobota"
    DayOfWeek.SUNDAY -> "Niedziela"
}

fun DayOfWeek.toPolishShortName(): String = when (this) {
    DayOfWeek.MONDAY -> "Pon"
    DayOfWeek.TUESDAY -> "Wt"
    DayOfWeek.WEDNESDAY -> "Śr"
    DayOfWeek.THURSDAY -> "Czw"
    DayOfWeek.FRIDAY -> "Pt"
    DayOfWeek.SATURDAY -> "Sob"
    DayOfWeek.SUNDAY -> "Ndz"
}
