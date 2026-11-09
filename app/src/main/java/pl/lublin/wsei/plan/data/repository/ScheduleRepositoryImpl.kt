package pl.lublin.wsei.plan.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import pl.lublin.wsei.plan.data.local.EncryptedCredentialStore
import pl.lublin.wsei.plan.data.local.ScheduleDao
import pl.lublin.wsei.plan.data.local.toEntity
import pl.lublin.wsei.plan.data.remote.api.ArpApiDataSource
import pl.lublin.wsei.plan.data.remote.api.ArpSessionCookieJar
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
import pl.lublin.wsei.plan.domain.repository.ScheduleRepository
import pl.lublin.wsei.plan.domain.repository.SyncReport
import pl.lublin.wsei.plan.service.ClassAlarmScheduler
import pl.lublin.wsei.plan.ui.schedule.SampleWseiScheduleFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure REST API + Offline-First Room Repository with strict multi-account isolation
 * and AES-256-GCM hardware-backed credential encryption ([EncryptedCredentialStore]):
 *
 * 1. Primary: [ArpApiDataSource] (100% reverse-engineered APR System Wirtualny Dziekanat API `:20830`)
 * 2. Internal Debug Test Account (`debug@wsei.lublin.pl` / `99901` + `debug123`)
 *    strictly isolated from real student accounts.
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val arpApiDataSource: ArpApiDataSource,
    private val sessionCookieJar: ArpSessionCookieJar,
    private val scheduleDao: ScheduleDao,
    private val encryptedCredentialStore: EncryptedCredentialStore,
    private val alarmScheduler: ClassAlarmScheduler
) : ScheduleRepository {

    private val _currentSession = MutableStateFlow<StudentSession?>(null)
    override val currentSession: StateFlow<StudentSession?> = _currentSession.asStateFlow()

    companion object {
        private val DEBUG_LOGINS = setOf("debug", "debug@wsei.lublin.pl", "99901", "test")
        private const val DEBUG_PASSWORD = "debug123"
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

        // Strict account isolation: wipe any previous user's cached events before authenticating
        sessionCookieJar.clearSession()
        scheduleDao.clearAllEvents()

        val wdResult = arpApiDataSource.authenticate(
            usernameOrAlbum = cleanLogin,
            password = password,
            twoFactorToken = twoFactorToken
        )

        if (wdResult.isSuccess) {
            val session = wdResult.getOrThrow()
            encryptedCredentialStore.saveEncryptedCredentials(
                login = cleanLogin,
                password = password,
                session = session,
                rememberCredentials = rememberCredentials
            )
            _currentSession.value = session
        }

        return wdResult
    }

    override suspend fun loginWithDebugTestAccount(
        rememberCredentials: Boolean
    ): StudentSession {
        sessionCookieJar.clearSession()
        scheduleDao.clearAllEvents()

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
        sessionCookieJar.clearSession()
        encryptedCredentialStore.clearVault()
        scheduleDao.clearAllEvents()
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

        val events = arpApiDataSource.fetchSchedule(
            session = session,
            from = startDate,
            to = endDate,
            scheduleType = scheduleType
        ).getOrThrow()

        scheduleDao.deleteRange(startDate.toString(), endDate.toString())
        scheduleDao.upsertEvents(events.map { it.toEntity(ownerStudId = session.studId) })
        alarmScheduler.scheduleUpcomingClassAlarms(events)

        SyncReport(
            syncedEventsCount = events.size,
            usedOrigin = DataSourceOrigin.APR_WD_API,
            syncedAtEpochMillis = System.currentTimeMillis()
        )
    }

    override suspend fun fetchStudentGroups(): Result<List<StudentSemesterGroups>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoStudentGroups())
        }
        return arpApiDataSource.fetchStudentGroups(session)
    }

    override suspend fun fetchStudyPlans(): Result<List<StudyPlanOverview>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoStudyPlans())
        }
        return arpApiDataSource.fetchStudyPlans(session)
    }

    override suspend fun fetchGrades(): Result<List<GradeItem>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoGrades())
        }
        return arpApiDataSource.fetchGrades(session)
    }

    override suspend fun fetchScholarships(): Result<List<ScholarshipApplication>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoScholarships())
        }
        return arpApiDataSource.fetchScholarships(session)
    }

    override suspend fun fetchFinancials(): Result<List<FinancialObligation>> {
        val session = _currentSession.value ?: return Result.success(emptyList())
        if (session.isDebugAccount) {
            return Result.success(SampleWseiScheduleFactory.createDemoFinancials())
        }
        return arpApiDataSource.fetchFinancials(session)
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
        return arpApiDataSource.fetchAnnouncements(session)
    }

    override suspend fun markAnnouncementRead(announcementId: Int): Result<Unit> {
        val session = _currentSession.value ?: return Result.success(Unit)
        if (session.isDebugAccount) {
            return Result.success(Unit)
        }
        return arpApiDataSource.markAnnouncementRead(session, announcementId)
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
