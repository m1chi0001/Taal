package pl.lublin.wsei.plan.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
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

/**
 * Minimal contract every timetable source must fulfil. Implemented by:
 * - [pl.lublin.wsei.plan.data.remote.api.ArpApiDataSource] — primary REST/JSON API (`:20830`),
 * - [pl.lublin.wsei.plan.data.remote.html.WdHtmlDataSource] — fallback that logs in through the
 *   ASP.NET web form and parses the timetable DOM with Jsoup.
 */
interface TimetableDataSource {
    val sourceOrigin: DataSourceOrigin

    suspend fun authenticate(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String? = null
    ): Result<StudentSession>

    suspend fun fetchSchedule(
        session: StudentSession,
        from: LocalDate,
        to: LocalDate,
        scheduleType: ScheduleFilterType = ScheduleFilterType.MY_PLAN
    ): Result<List<ClassEvent>>
}

/**
 * Full REST API contract (grades, finances, announcements…) — only the JSON API exposes these.
 */
interface WseiRemoteDataSource : TimetableDataSource {

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>

    suspend fun fetchStudentGroups(
        session: StudentSession
    ): Result<List<StudentSemesterGroups>>

    suspend fun fetchStudyPlans(
        session: StudentSession
    ): Result<List<StudyPlanOverview>>

    suspend fun fetchGrades(session: StudentSession): Result<List<GradeItem>>

    suspend fun fetchScholarships(session: StudentSession): Result<List<ScholarshipApplication>>

    suspend fun fetchFinancials(session: StudentSession): Result<List<FinancialObligation>>

    suspend fun fetchOnlinePaymentUrl(
        chargeId: Int,
        amountPln: Double
    ): Result<String>

    suspend fun fetchAnnouncements(session: StudentSession): Result<List<DeanAnnouncement>>

    suspend fun markAnnouncementRead(
        session: StudentSession,
        announcementId: Int
    ): Result<Unit>

    suspend fun downloadAnnouncementAttachment(
        session: StudentSession,
        attachmentId: Int
    ): Result<AnnouncementAttachment>
}

data class SyncReport(
    val syncedEventsCount: Int,
    val usedOrigin: DataSourceOrigin,
    val syncedAtEpochMillis: Long
)

interface ScheduleRepository {
    val currentSession: StateFlow<StudentSession?>

    suspend fun restoreEncryptedSessionIfAvailable(): StudentSession?

    fun getDecryptedSavedLogin(): String?

    suspend fun loginWithCredentials(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String? = null,
        rememberCredentials: Boolean = true
    ): Result<StudentSession>

    suspend fun loginWithDebugTestAccount(
        rememberCredentials: Boolean = true
    ): StudentSession

    suspend fun logout()

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>

    fun observeAcademicDays(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<AcademicDay>>

    fun observeAllEvents(): Flow<List<ClassEvent>>

    fun observeActiveClassState(nowEpochMillis: Long): Flow<ActiveClassSnapshot?>

    suspend fun syncSchedule(
        startDate: LocalDate,
        endDate: LocalDate,
        scheduleType: ScheduleFilterType = ScheduleFilterType.MY_PLAN,
        forceRefresh: Boolean = false
    ): Result<SyncReport>

    suspend fun fetchStudentGroups(): Result<List<StudentSemesterGroups>>

    suspend fun fetchStudyPlans(): Result<List<StudyPlanOverview>>

    suspend fun fetchGrades(): Result<List<GradeItem>>

    suspend fun fetchScholarships(): Result<List<ScholarshipApplication>>

    suspend fun fetchFinancials(): Result<List<FinancialObligation>>

    suspend fun fetchOnlinePaymentUrl(
        chargeId: Int,
        amountPln: Double
    ): Result<String>

    suspend fun fetchAnnouncements(): Result<List<DeanAnnouncement>>

    suspend fun markAnnouncementRead(announcementId: Int): Result<Unit>

    suspend fun downloadAnnouncementAttachment(attachmentId: Int): Result<AnnouncementAttachment>

    /** Last cached academic modules (offline-first prefill for the UI). Never touches the network. */
    suspend fun loadCachedAcademicModules(): CachedAcademicModules

    /** Network refresh of every academic module into the local cache (used by the background worker). */
    suspend fun refreshAcademicSnapshots(): Boolean

    suspend fun getEventById(eventId: String): ClassEvent?

    suspend fun getEventsOnDay(dateIso: String): List<ClassEvent>

    suspend fun getNextEventAfter(eventId: String): ClassEvent?
}

data class CachedAcademicModules(
    val studentGroups: List<StudentSemesterGroups>? = null,
    val studyPlans: List<StudyPlanOverview>? = null,
    val grades: List<GradeItem>? = null,
    val scholarships: List<ScholarshipApplication>? = null,
    val financials: List<FinancialObligation>? = null,
    val announcements: List<DeanAnnouncement>? = null,
    val lastUpdatedEpochMillis: Long? = null
)

data class ActiveClassSnapshot(
    val activeEvent: ClassEvent,
    val nextEvent: ClassEvent?,
    val breakMinutesUntilNext: Int?
)
