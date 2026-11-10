package pl.lublin.wsei.plan.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import pl.lublin.wsei.plan.data.remote.ScheduleNormalizer
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.ScheduleFilterType
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.ScholarshipPayoutItem
import pl.lublin.wsei.plan.domain.model.StudentGroupItem
import pl.lublin.wsei.plan.domain.model.StudentSemesterGroups
import pl.lublin.wsei.plan.domain.model.StudentSession
import pl.lublin.wsei.plan.domain.model.StudyPlanOverview
import pl.lublin.wsei.plan.domain.model.StudyPlanSemester
import pl.lublin.wsei.plan.domain.model.StudyPlanSubjectItem
import pl.lublin.wsei.plan.domain.model.TimeSlot
import pl.lublin.wsei.plan.domain.model.formatPolishBankAccount
import pl.lublin.wsei.plan.domain.repository.WseiRemoteDataSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Primary data source: APR System "Mobilny Student" REST API (`MobilnyStudent.ApiPa`) for
 * Uniwersytet WSEI w Lublinie (`https://dziekanat.wsei.lublin.pl:20830/`).
 *
 * Endpoint paths and DTO shapes are checked against the official app
 * (`APRSystem.MobilnyStudent.Application.Shared.dll`, `MobilnyStudent.ApiPa.dll`).
 *
 * Every call goes through [getAbp]/[sendAbp], which validate the HTTP status, unwrap the ABP envelope
 * and turn failures into typed [AprApiException]s (instead of `body()` blowing up on an empty 401
 * or an HTML page).
 */
@Singleton
class ArpApiDataSource @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionCookieJar: ArpSessionCookieJar,
    private val json: Json
) : WseiRemoteDataSource {

    override val sourceOrigin: DataSourceOrigin = DataSourceOrigin.APR_WD_API

    companion object {
        const val WSEI_WD_BASE_URL = "https://dziekanat.wsei.lublin.pl:20830"
        const val APR_TENANT_DIRECTORY_URL =
            "https://api-proklucznik.apr.pl/api/services/app/Tenant/GetMSCustomers"
        private const val PERSON_TYPE_STUDENT = "Student"
        private val SERVER_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    // ---------------------------------------------------------------------------------------------
    // HTTP plumbing
    // ---------------------------------------------------------------------------------------------

    private suspend inline fun <reified T> getAbp(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): T? = httpClient.get("$WSEI_WD_BASE_URL$path") { block() }.decodeAbp(json, serializer<T>())

    private suspend inline fun <reified B : Any, reified T> sendAbp(
        method: String,
        path: String,
        body: B
    ): T? {
        val url = "$WSEI_WD_BASE_URL$path"
        val response = when (method) {
            "PUT" -> httpClient.put(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            else -> httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        return response.decodeAbp(json, serializer<T>())
    }

    /** Like [runCatching] but never swallows coroutine cancellation. */
    private inline fun <T> apiCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

    /** Optional sub-request: failures are tolerated, but cancellation and expired sessions are not. */
    private suspend inline fun <T> optional(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AprSessionExpiredException) {
        throw e
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------------------------------------
    // Authentication & profile
    // ---------------------------------------------------------------------------------------------

    /**
     * `MobilnyStudent.ApiPa.Services.Session.Login`:
     * 1. `POST /api/MobileAuth/Authenticate` (`UserNameOrEmailAddress`, `Password`, `Token`)
     * 2. `GET /api/services/app/Login/GetCoursesOfStudy` → `StudId`
     * 3. `GET ProfilePages/GetStudentData`, `Dashboard/GetDashboard`, `Login/GetCourseOfStudyDetails`, photo
     */
    override suspend fun authenticate(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String?
    ): Result<StudentSession> = apiCall {
        val login = usernameOrAlbum.trim()
        sessionCookieJar.bearerToken = null

        val authResponse = httpClient.post("$WSEI_WD_BASE_URL${ArpTokenRefresher.AUTH_PATH}") {
            header(ArpAutoReauthInterceptor.HEADER_SKIP_REAUTH, "1")
            contentType(ContentType.Application.Json)
            setBody(
                MobileAuthRequestDto(
                    userNameOrEmailAddress = login,
                    password = password,
                    token = twoFactorToken?.takeIf { it.isNotBlank() }
                )
            )
        }
        val decodedAuth: MobileAuthResponseDto? = try {
            authResponse.decodeAbp(json, MobileAuthResponseDto.serializer())
        } catch (e: AprSessionExpiredException) {
            // On the login call a 401 means "wrong credentials", not "session expired".
            throw AprServerException(401, "Nieprawidłowy login lub hasło.", null)
        }
        val authResult = decodedAuth ?: error("Serwer nie zwrócił danych logowania. Spróbuj ponownie.")

        if (authResult.shouldResetPassword) {
            error("Konto wymaga zmiany hasła w Wirtualnym Dziekanacie.")
        }
        if (authResult.requiresTwoFactorVerification && authResult.accessToken.isNullOrBlank()) {
            error("Konto wymaga kodu weryfikacji dwuetapowej.")
        }
        val accessToken = authResult.accessToken?.takeIf { it.isNotBlank() }
            ?: error("Serwer nie zwrócił tokenu dostępu. Spróbuj ponownie.")

        sessionCookieJar.bearerToken = accessToken
        sessionCookieJar.savedLogin = login
        sessionCookieJar.savedPassword = password

        val courses = optional {
            getAbp<MsCoursesOfStudyDto>("/api/services/app/Login/GetCoursesOfStudy")
        }?.courseOfStudies.orEmpty()
        val activeCourse = courses.firstOrNull { it.aktualny } ?: courses.firstOrNull()
        val studId = activeCourse?.id?.takeIf { it != 0 }
            ?: authResult.idLoggedPersonType?.takeIf { it != 0 }
            ?: authResult.personId

        val profile = optional {
            getAbp<MsStudentDataDto>("/api/services/app/ProfilePages/GetStudentData") {
                parameter("Id", studId)
                parameter("PersonType", PERSON_TYPE_STUDENT)
                parameter("IdLoggedPersonType", studId)
            }
        }
        val dashboard = optional {
            getAbp<MsDashboardDto>("/api/services/app/Dashboard/GetDashboard") {
                parameter("Id", studId)
                parameter("IdLoggedPersonType", studId)
                parameter("PersonType", PERSON_TYPE_STUDENT)
            }
        }
        val loginDetails = optional {
            getAbp<MsStudentLoginDetailsDto>("/api/services/app/Login/GetCourseOfStudyDetails") {
                parameter("Id", studId)
            }
        }
        val photoBase64 = fetchPersonPictureBase64(studId = studId, personId = authResult.personId)
            ?: authResult.personPicture?.ifBlank { null }
            ?: profile?.personalData?.picture?.ifBlank { null }

        val personal = profile?.personalData
        val courseDash = dashboard?.courseOfStudy
        val courseCode = personal?.courseOfStudyName?.ifBlank { null }
            ?: activeCourse?.name?.ifBlank { null }
            ?: "Tok podstawowy"
        val mode = personal?.nameOfStudies?.ifBlank { null }
            ?: activeCourse?.nameOfStudies?.ifBlank { null }
            ?: if (courseCode.contains("/N", ignoreCase = true)) "niestacjonarne" else "stacjonarne"

        StudentSession(
            userId = authResult.userId,
            personId = authResult.personId,
            studId = studId,
            login = login,
            firstName = personal?.firstName?.ifBlank { null } ?: authResult.firstName.ifBlank { "Student" },
            lastName = personal?.surname?.ifBlank { null } ?: authResult.surname.ifBlank { "WSEI" },
            albumNumber = personal?.albumNumber?.ifBlank { null } ?: login,
            fieldOfStudy = personal?.fieldOfStudy?.ifBlank { null }
                ?: activeCourse?.fieldOfStudy?.ifBlank { null }
                ?: "Kierunek WSEI",
            department = personal?.department?.ifBlank { null }
                ?: activeCourse?.department?.ifBlank { null }
                ?: "Wydział Transportu i Informatyki",
            courseOfStudyCode = courseCode,
            studyMode = mode,
            obtainedTitle = personal?.obtainedTitle?.ifBlank { null }
                ?: activeCourse?.obtainedTitle?.ifBlank { null }
                ?: "inż.",
            specialization = activeCourse?.nameOfStudies.orEmpty(),
            semesterNumber = courseDash?.semestr?.takeIf { it > 0 } ?: personal?.semestr?.takeIf { it > 0 } ?: 1,
            academicYear = courseDash?.year?.takeIf { it > 0 } ?: personal?.year?.takeIf { it > 0 } ?: 1,
            statusName = courseDash?.status?.ifBlank { null } ?: personal?.status?.ifBlank { null } ?: "Studiuje",
            averageCurrentSemester = courseDash?.averageCurrentSemester ?: 0.0,
            averagePreviousSemester = courseDash?.averagePreviousSemester ?: 0.0,
            averageGrade = courseDash?.studyAverage ?: 0.0,
            personPictureBase64 = photoBase64,
            allowPasswordChanges = personal?.allowPasswordChanges ?: true,
            unreadAnnouncementCount = loginDetails?.unreadAnnouncementCount ?: 0,
            accessToken = accessToken,
            isDebugAccount = false
        )
    }

    /** `GET /api/UserPhotos/GetPersonPictureAsync?Id=&IdLoggedPersonType=&PersonType=Student` */
    suspend fun fetchPersonPictureBase64(studId: Int, personId: Int): String? {
        for (targetId in listOf(studId, personId).distinct().filter { it != 0 }) {
            val dto = optional {
                getAbp<MsPictureDto>("/api/UserPhotos/GetPersonPictureAsync") {
                    parameter("Id", targetId)
                    parameter("IdLoggedPersonType", studId)
                    parameter("PersonType", PERSON_TYPE_STUDENT)
                }
            }
            val base64 = dto?.picture?.ifBlank { null }
                ?: dto?.imageBytes?.ifBlank { null }
                ?: dto?.fileBytes?.ifBlank { null }
            if (!base64.isNullOrBlank()) return base64
        }
        return null
    }

    /** `POST /api/services/app/Profile/ChangePassword` */
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = apiCall {
        sendAbp<ChangePasswordInputDto, JsonElement>(
            "POST",
            "/api/services/app/Profile/ChangePassword",
            ChangePasswordInputDto(currentPassword = currentPassword, newPassword = newPassword)
        )
        sessionCookieJar.savedPassword = newPassword
    }

    private suspend fun fetchStudentData(session: StudentSession): MsStudentDataDto? =
        getAbp<MsStudentDataDto>("/api/services/app/ProfilePages/GetStudentData") {
            parameter("Id", session.studId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        }

    override suspend fun fetchStudentGroups(
        session: StudentSession
    ): Result<List<StudentSemesterGroups>> = apiCall {
        fetchStudentData(session)?.groups?.collegeSemesters.orEmpty().reversed().map { sem ->
            StudentSemesterGroups(
                semesterId = sem.id,
                semesterLabel = sem.name.ifBlank { "Semestr ${sem.number}" },
                semesterNumber = sem.number,
                groups = sem.groups.map { grp ->
                    StudentGroupItem(
                        id = grp.id,
                        groupCode = grp.name,
                        formName = grp.type.ifBlank { "Grupa" },
                        groupNumber = grp.editionName.ifBlank { "1" },
                        isPlanVisible = grp.isPlanVisible
                    )
                }
            )
        }
    }

    /**
     * 1. `ProfilePages/GetStudentData` → `StudentPlans.StudentPlans`
     * 2. `ProfilePages/GetPlansSemesters?Id={planId}&PlanProfile={plan.PlanProfile}`
     *    (previously hard-coded to `NoDefined`, which returns an empty plan for other profiles)
     */
    override suspend fun fetchStudyPlans(
        session: StudentSession
    ): Result<List<StudyPlanOverview>> = apiCall {
        val plans = fetchStudentData(session)?.studentPlans?.studentPlans.orEmpty()
        plans.map { plan ->
            val planDto = optional {
                getAbp<MsStudyPlanDto>("/api/services/app/ProfilePages/GetPlansSemesters") {
                    parameter("Id", plan.id)
                    parameter("PlanProfile", plan.planProfile.ifBlank { "NoDefined" })
                }
            }
            StudyPlanOverview(
                planId = plan.id,
                planName = plan.name.ifBlank { session.courseOfStudyCode },
                semesters = planDto?.planSemesters.orEmpty().reversed().map { sem ->
                    StudyPlanSemester(
                        semesterId = sem.id,
                        semesterName = sem.name.ifBlank { "Semestr ${sem.number}" },
                        semesterNumber = sem.number,
                        subjects = sem.planSubjects.map { subj ->
                            StudyPlanSubjectItem(
                                id = subj.id,
                                subjectNumber = subj.subjectNumber,
                                subjectName = subj.subjectName.ifBlank { subj.name },
                                form = subj.form,
                                passingForm = subj.passingForm,
                                hours = subj.hours.takeIf { it > 0 }
                                    ?: subj.hoursString.filter(Char::isDigit).toIntOrNull() ?: 0,
                                ects = subj.ects.takeIf { it > 0.0 }?.toFloat()
                                    ?: subj.ectsString.replace(',', '.').toFloatOrNull() ?: 0f,
                                semesterNumber = subj.semestr
                            )
                        }
                    )
                }
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Timetable
    // ---------------------------------------------------------------------------------------------

    /**
     * `GET /api/services/app/ScheduleData/GetMyClassSchedule` (`ScheduleRequestIdDto` : `RequestIdDto`):
     * `Id`, `ScheduleType` (`Student` = 5 / `GroupsForStudent` = 100), `DateFrom`, `DateTo`,
     * `PersonType`, `IdLoggedPersonType`.
     *
     * `IdLoggedPersonType` was missing before; every other student endpoint sends it and the backend uses
     * it for the ownership check.
     */
    override suspend fun fetchSchedule(
        session: StudentSession,
        from: LocalDate,
        to: LocalDate,
        scheduleType: ScheduleFilterType
    ): Result<List<ClassEvent>> = apiCall {
        val dto = getAbp<MsMyClassScheduleDto>("/api/services/app/ScheduleData/GetMyClassSchedule") {
            parameter("Id", session.studId)
            parameter("ScheduleType", scheduleType.apiEnumName)
            parameter("DateFrom", "${from}T00:00:00")
            parameter("DateTo", "${to}T23:59:59")
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        } ?: return@apiCall emptyList()

        dto.classScheduleGroups
            .mapIndexedNotNull { index, item -> item.toDomainModel(index) }
            .distinctBy { it.id }
    }

    // ---------------------------------------------------------------------------------------------
    // Grades, scholarships, finances
    // ---------------------------------------------------------------------------------------------

    /** `GET /api/services/app/SubjectsData/GetSubjectsData?Id={studId}` */
    override suspend fun fetchGrades(session: StudentSession): Result<List<GradeItem>> = apiCall {
        val dto = getAbp<MsSubjectsSemestersDataDto>("/api/services/app/SubjectsData/GetSubjectsData") {
            parameter("Id", session.studId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        }
        dto?.subjectsSemesters.orEmpty().reversed().flatMap { sem ->
            val semLabel = sem.name.ifBlank { "Semestr ${sem.number}" }
            val sessionDate = ScheduleNormalizer.normalizeIsoDate(sem.sessionTo)
                ?: ScheduleNormalizer.normalizeIsoDate(sem.sessionFrom)
                ?: ""
            sem.subjects.mapIndexed { idx, subj ->
                val ectsValue = subj.ects.ifBlank { subj.ectsString }
                val fullLecturer = subj.subjectLecturerFullName.ifBlank {
                    listOf(subj.degreeSubjectLecturer, subj.subjectLecturer).filter { it.isNotBlank() }.joinToString(" ")
                }
                val classType = ClassType.fromRawCode(subj.form)
                GradeItem(
                    id = "${sem.id}_${subj.id}_$idx",
                    subjectId = subj.id,
                    semesterLabel = semLabel,
                    semesterNumber = sem.number,
                    subjectName = subj.name,
                    moduleName = "Moduł ogólny",
                    classType = classType,
                    formLabel = subj.form.ifBlank { classType.polishLabel },
                    gradeValue = subj.grade.ifBlank { "—" },
                    passingForm = subj.passingForm.ifBlank { "Zaliczenie ocena" },
                    ects = ectsValue.replace(',', '.').toDoubleOrNull()?.roundToInt() ?: 0,
                    lecturerName = fullLecturer.ifBlank { "Wykładowca WSEI" },
                    dateAwarded = sessionDate
                )
            }
        }
    }

    override suspend fun fetchScholarships(
        session: StudentSession
    ): Result<List<ScholarshipApplication>> = apiCall {
        val dto = getAbp<MsSchoolarshipsSemestersDto>(
            "/api/services/app/SchoolarshipData/GetSchoolarshipsSemestersData"
        ) {
            parameter("Id", session.studId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        }
        dto?.schoolarshipSemesters.orEmpty().reversed().flatMap { sem ->
            val semLabel = sem.name.ifBlank { "Semestr ${sem.number}" }
            sem.schoolarships.map { item ->
                val details = optional {
                    getAbp<MsScholarshipDetailsDto>("/api/services/app/SchoolarshipData/GetScholarshipDetailsAsync") {
                        parameter("Id", item.id)
                        parameter("PersonType", PERSON_TYPE_STUDENT)
                        parameter("IdLoggedPersonType", session.studId)
                    }
                }
                val firstRuling = details?.rulings?.firstOrNull() ?: details?.ruling
                ScholarshipApplication(
                    id = item.id,
                    semesterLabel = semLabel,
                    number = item.number,
                    title = details?.name?.ifBlank { null } ?: item.name,
                    typeVariantName = details?.typeVariantName.orEmpty(),
                    submissionDateIso = ScheduleNormalizer.isoToPolishDate(details?.applicationDate ?: item.date),
                    statusString = details?.applicationStatusString?.ifBlank { null }
                        ?: item.state.ifBlank { "Złożony" },
                    isGranted = item.schoolarshipGranted,
                    decisionNumber = firstRuling?.number?.ifBlank { null },
                    decisionDateIso = firstRuling?.date?.ifBlank { null }?.let(ScheduleNormalizer::isoToPolishDate),
                    acceptedRules = firstRuling?.acceptedRules ?: true,
                    payouts = details?.payments?.payments.orEmpty().map { p ->
                        ScholarshipPayoutItem(
                            id = p.id,
                            amountFormatted = p.amount,
                            dateIso = ScheduleNormalizer.isoToPolishDate(p.date),
                            status = p.status
                        )
                    }
                )
            }
        }
    }

    /** `GET /api/services/app/FinancesData/GetFinancesData?ChargeType=Normal&Id=&IdLoggedPersonType=` */
    override suspend fun fetchFinancials(session: StudentSession): Result<List<FinancialObligation>> = apiCall {
        val finances = getAbp<MsFinancesDto>("/api/services/app/FinancesData/GetFinancesData") {
            parameter("ChargeType", "Normal")
            parameter("Id", session.studId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        }
        val accountNumber = formatPolishBankAccount(finances?.arrearsState?.accountNumber.orEmpty())
        finances?.charges?.charges.orEmpty().mapIndexed { idx, charge ->
            val parsedAmount = parseMoneyAmount(charge.amount)
            val isPaid = charge.currentState == 0.0 ||
                charge.status.contains("Opłac", ignoreCase = true) ||
                charge.status.contains("Paid", ignoreCase = true)
            FinancialObligation(
                id = "${charge.id}_$idx",
                chargeId = charge.id,
                title = charge.name,
                amountPln = parsedAmount,
                amountFormatted = charge.amount.ifBlank {
                    String.format(Locale.US, "%.2f %s", parsedAmount, charge.currencyCode.ifBlank { "PLN" })
                },
                currentStatePln = charge.currentState,
                dueDateIso = ScheduleNormalizer.isoToPolishDate(charge.paymentTerm),
                paymentDateIso = charge.dateOfPayment?.ifBlank { null }?.let(ScheduleNormalizer::isoToPolishDate),
                statusLabel = charge.status.ifBlank { if (isPaid) "Opłacono" else "Do uregulowania" },
                interestFormatted = charge.interest.ifBlank { "0.00 PLN" },
                accumulatedInterestFormatted = charge.accumulatedInterest.ifBlank { "0.00 PLN" },
                amortizationFormatted = charge.amortization.ifBlank { "0.00 PLN" },
                discountFormatted = charge.discount.ifBlank { "0.00 PLN" },
                expiredFormatted = charge.expired.ifBlank { "0.00 PLN" },
                isPaid = isPaid,
                isExpired = parseMoneyAmount(charge.expired) > 0.0,
                payOnlineAvailable = charge.payOnlineAvailable,
                bankAccountNumber = accountNumber
            )
        }
    }

    /** `GET /api/services/app/BlueMedia/GetStartPaymentAsync?chargeId=&price=` */
    override suspend fun fetchOnlinePaymentUrl(
        chargeId: Int,
        amountPln: Double
    ): Result<String> = apiCall {
        val answer = getAbp<PaymentAnswerDto>("/api/services/app/BlueMedia/GetStartPaymentAsync") {
            parameter("chargeId", chargeId)
            parameter("price", String.format(Locale.US, "%.2f", kotlin.math.abs(amountPln)))
        }
        answer?.paymentUrl?.takeIf { it.isNotBlank() }
            ?: error(answer?.errorMessage?.ifBlank { null } ?: answer?.error?.ifBlank { null }
                ?: "Brak linku do płatności online BlueMedia.")
    }

    // ---------------------------------------------------------------------------------------------
    // Announcements
    // ---------------------------------------------------------------------------------------------

    override suspend fun fetchAnnouncements(session: StudentSession): Result<List<DeanAnnouncement>> = apiCall {
        val dto = getAbp<MsAnnouncementsListDto>("/api/services/app/AnnouncementsData/GetAnnouncementsList") {
            parameter("Id", session.studId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        }
        dto?.announcementsItems.orEmpty().mapIndexed { idx, item ->
            val cleanBody = ScheduleNormalizer.stripHtml(item.text)
            val senderName = item.name.ifBlank { "Dziekanat" }
            val priority = parseAnnouncementPriority(item.priority)
            DeanAnnouncement(
                id = "${item.id}_$idx",
                announcementId = item.id,
                title = item.subject.ifBlank { item.name },
                bodySnippet = cleanBody.replace('\n', ' ').take(180),
                fullText = cleanBody.ifBlank { item.subject },
                publishedAtIso = ScheduleNormalizer.isoToPolishDate(item.date),
                priorityLevel = priority,
                isImportant = priority == 0, // AnnouncementPriority.High = 0
                isRead = item.readed,
                hasAttachment = item.isAttachment || item.attachments.isNotEmpty(),
                attachments = item.attachments.map { att ->
                    AnnouncementAttachment(id = att.id, fileName = att.name)
                },
                authorDepartment = senderName,
                senderInitial = senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
            )
        }
    }

    /** `PUT /api/services/app/AnnouncementsData/UpdateAnnouncementReaded` */
    override suspend fun markAnnouncementRead(
        session: StudentSession,
        announcementId: Int
    ): Result<Unit> = apiCall {
        sendAbp<MsAnnouncementReadedUpdateInputDto, JsonElement>(
            "PUT",
            "/api/services/app/AnnouncementsData/UpdateAnnouncementReaded",
            MsAnnouncementReadedUpdateInputDto(
                id = announcementId,
                personId = session.personId,
                readed = true,
                // .NET DateTime binder: no nanoseconds (LocalDateTime.toString() produced 9 fraction digits)
                readedDate = LocalDateTime.now(ScheduleNormalizer.WARSAW_ZONE).format(SERVER_DATE_TIME)
            )
        )
        Unit
    }

    /** `GET /api/services/app/AnnouncementsData/GetAttachmentBytes?Id=` */
    override suspend fun downloadAnnouncementAttachment(
        session: StudentSession,
        attachmentId: Int
    ): Result<AnnouncementAttachment> = apiCall {
        val dto = getAbp<MsAnnouncementAttachmentDto>("/api/services/app/AnnouncementsData/GetAttachmentBytes") {
            parameter("Id", attachmentId)
            parameter("PersonType", PERSON_TYPE_STUDENT)
            parameter("IdLoggedPersonType", session.studId)
        } ?: error("Nie udało się pobrać załącznika.")
        AnnouncementAttachment(
            id = dto.id.takeIf { it != 0 } ?: attachmentId,
            fileName = dto.name.ifBlank { "zalacznik_$attachmentId.pdf" },
            fileBytesBase64 = dto.fileBytes?.ifBlank { null } ?: error("Załącznik jest pusty.")
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------------------------------

    /**
     * Returns null for items whose date/time can't be parsed. The previous version fell back to
     * `System.currentTimeMillis()`, which produced phantom "ongoing" classes in the live notification.
     */
    private fun MsClassScheduleItemDto.toDomainModel(fallbackIndex: Int): ClassEvent? {
        val dateIso = ScheduleNormalizer.normalizeIsoDate(date) ?: return null
        val range = ScheduleNormalizer.extractTimeRange(time) ?: ScheduleNormalizer.extractTimeRange(hours)
        val startHhMm = ScheduleNormalizer.extractTimeHhMm(hourFrom) ?: range?.first ?: return null
        val endHhMm = ScheduleNormalizer.extractTimeHhMm(hourTo) ?: range?.second
        val startMillis = ScheduleNormalizer.toEpochMillis(dateIso, startHhMm) ?: return null
        val endMillis = endHhMm?.let { ScheduleNormalizer.toEpochMillis(dateIso, it) }
            ?.takeIf { it > startMillis }
            ?: (startMillis + 90 * 60_000L)
        val endFormatted = java.time.Instant.ofEpochMilli(endMillis)
            .atZone(ScheduleNormalizer.WARSAW_ZONE).toLocalTime().toString().take(5)

        val subjectName = name.ifBlank { labelText.ifBlank { "Zajęcia dydaktyczne" } }
        val notesText = listOf(comments, comment, labelComments).map(String::trim).filter(String::isNotBlank).distinct()
        val placement = ScheduleNormalizer.resolvePlacement(
            rawRoom = location.ifBlank { room },
            form = form,
            extraMeta = "${notesText.joinToString(" ")} $labelText $name"
        )
        // `lecturerVisible` bywa false/null mimo wypełnionego pola `lecturer` (v0.5 przez to gubiła prowadzących);
        // jak w v0.4 zawsze bierzemy tekst, a braki uzupełnia ScheduleNormalizer.fillMissingLecturers.
        val lecturerText = lecturer

        return ClassEvent(
            id = if (id != 0) "wd_${id}_${dateIso}_$startHhMm" else "wd_${dateIso}_${startHhMm}_$fallbackIndex",
            dateIso = dateIso,
            subject = ScheduleNormalizer.buildSubject(subjectName),
            classType = ClassType.fromRawCode(form),
            lecturer = ScheduleNormalizer.parseLecturer(lecturerText, idLecturer.toString()),
            room = if (placement.room.isRemoteTeams || address.isBlank()) {
                placement.room
            } else {
                placement.room.copy(building = address.trim())
            },
            timeSlot = TimeSlot(
                startEpochMillis = startMillis,
                endEpochMillis = endMillis,
                startFormatted = startHhMm,
                endFormatted = endFormatted
            ),
            groupCode = group.ifBlank { "Grupa dziekanatowa" },
            deliveryMode = placement.deliveryMode,
            address = address.ifBlank { null },
            notes = notesText.joinToString(" • ").ifBlank { null },
            incidentAlert = scheduleIncident?.takeIf { it.isNotBlank() },
            origin = DataSourceOrigin.APR_WD_API
        )
    }

    /** `AnnouncementPriority`: High = 0, Normal = 1, Low = 2 — sent as int or as enum name. */
    private fun parseAnnouncementPriority(raw: String): Int {
        val v = raw.trim().lowercase()
        v.toIntOrNull()?.let { return it.coerceIn(0, 2) }
        return when {
            v.startsWith("high") || v.startsWith("wys") || v.startsWith("pil") -> 0
            v.startsWith("low") || v.startsWith("nis") -> 2
            else -> 1
        }
    }

    /** `AmountStringToMoney.MapFrom` — "1 234,56 PLN" → 1234.56 */
    private fun parseMoneyAmount(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val compact = raw.replace(" ", "").replace(" ", "")
        return Regex("""[-+]?\d+(?:[.,]\d+)?""").find(compact)?.value?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
    }
}

// =================================================================================================
// DTOs — shapes from `APRSystem.MobilnyStudent.Application.Shared.dll` (APRSystem.coreTest.MS.*)
// and `MobilnyStudent.ApiPa.dll`. ABP serializes them in camelCase.
//
// All scalar fields use the lenient L* aliases (see LenientSerializers.kt): C# decimals, enums,
// TimeSpan and DateTime arrive as numbers *or* strings depending on server version, and one strict
// field used to make the whole response — and therefore the whole screen — fail.
// =================================================================================================

@Serializable
data class AbpAjaxResponse<T>(
    @SerialName("result") val result: T? = null,
    @SerialName("targetUrl") val targetUrl: String? = null,
    @SerialName("success") val success: LBool = true,
    @SerialName("unAuthorizedRequest") val unAuthorizedRequest: LBool = false,
    @SerialName("error") val error: AbpErrorInfo? = null,
    @SerialName("__abp") val abp: LBool = true
)

@Serializable
data class AbpErrorInfo(
    @SerialName("code") val code: LInt = 0,
    @SerialName("message") val message: String? = null,
    @SerialName("details") val details: String? = null
)

/** Anonymous type `{ UserNameOrEmailAddress, Password, Token }` (PascalCase, System.Text.Json default). */
@Serializable
data class MobileAuthRequestDto(
    @SerialName("UserNameOrEmailAddress") val userNameOrEmailAddress: String,
    @SerialName("Password") val password: String,
    @SerialName("Token") val token: String? = null
)

/** `MobileAuthResponse` — `PersonType` is the `MsPersonType` enum (int or name). */
@Serializable
data class MobileAuthResponseDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("shouldResetPassword") val shouldResetPassword: LBool = false,
    @SerialName("requiresTwoFactorVerification") val requiresTwoFactorVerification: LBool = false,
    @SerialName("userId") val userId: LLong = 0L,
    @SerialName("personId") val personId: LInt = 0,
    @SerialName("personType") val personType: LString = "",
    @SerialName("idLoggedPersonType") val idLoggedPersonType: LInt? = null,
    @SerialName("personPicture") val personPicture: String? = null,
    @SerialName("firstName") val firstName: LString = "",
    @SerialName("surname") val surname: LString = ""
)

@Serializable
data class ChangePasswordInputDto(
    @SerialName("CurrentPassword") val currentPassword: String,
    @SerialName("NewPassword") val newPassword: String
)

@Serializable
data class MsCoursesOfStudyDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("courseOfStudies") val courseOfStudies: List<MsCourseOfStudyDto> = emptyList()
)

@Serializable
data class MsCourseOfStudyDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("department") val department: LString = "",
    @SerialName("fieldOfStudy") val fieldOfStudy: LString = "",
    @SerialName("obtainedTitle") val obtainedTitle: LString = "",
    @SerialName("nameOfStudies") val nameOfStudies: LString = "",
    @SerialName("aktualny") val aktualny: LBool = false,
    @SerialName("studyDateFrom") val studyDateFrom: LString? = null,
    @SerialName("studyDateTo") val studyDateTo: LString? = null
)

/** `MsPictureDto.ImageBytes` is `byte[]` → base64 string in JSON. */
@Serializable
data class MsPictureDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("pictureVisible") val pictureVisible: LBool = true,
    @SerialName("picture") val picture: String? = null,
    @SerialName("imageBytes") val imageBytes: String? = null,
    @SerialName("fileBytes") val fileBytes: String? = null
)

@Serializable
data class MsStudentLoginDetailsDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("unreadAnnouncementCount") val unreadAnnouncementCount: LInt = 0,
    @SerialName("studyDateFrom") val studyDateFrom: LString? = null,
    @SerialName("studyDateTo") val studyDateTo: LString? = null
)

@Serializable
data class MsDashboardDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("arrearsState") val arrearsState: MsArrearsStateDto? = null,
    @SerialName("courseOfStudy") val courseOfStudy: MsCourseOfStudyDashboardDto? = null,
    @SerialName("schoolarship") val schoolarship: MsSchoolarshipForDashboardDto? = null,
    @SerialName("arrearsIsCloseToLimit") val arrearsIsCloseToLimit: LBool = false,
    @SerialName("arrearsIsOverLimit") val arrearsIsOverLimit: LBool = false
)

/** Averages are C# `decimal`. */
@Serializable
data class MsCourseOfStudyDashboardDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("semestr") val semestr: LInt = 0,
    @SerialName("status") val status: LString = "",
    @SerialName("year") val year: LInt = 0,
    @SerialName("averageCurrentSemester") val averageCurrentSemester: LDouble = 0.0,
    @SerialName("averagePreviousSemester") val averagePreviousSemester: LDouble = 0.0,
    @SerialName("studyAverage") val studyAverage: LDouble = 0.0
)

@Serializable
data class MsSchoolarshipForDashboardDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("payments") val payments: List<MsPaymentDto> = emptyList()
)

@Serializable
data class MsStudentDataDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("personalData") val personalData: MsProfileDataDto? = null,
    @SerialName("groups") val groups: MsGroupsDto? = null,
    @SerialName("studentPlans") val studentPlans: MsStudentPlansDto? = null
)

@Serializable
data class MsProfileDataDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("firstName") val firstName: LString = "",
    @SerialName("surname") val surname: LString = "",
    @SerialName("albumNumber") val albumNumber: LString = "",
    @SerialName("courseOfStudyName") val courseOfStudyName: LString = "",
    @SerialName("department") val department: LString = "",
    @SerialName("fieldOfStudy") val fieldOfStudy: LString = "",
    @SerialName("obtainedTitle") val obtainedTitle: LString = "",
    @SerialName("nameOfStudies") val nameOfStudies: LString = "",
    @SerialName("semestr") val semestr: LInt = 0,
    @SerialName("year") val year: LInt = 0,
    @SerialName("status") val status: LString = "",
    @SerialName("picture") val picture: String? = null,
    @SerialName("allowPasswordChanges") val allowPasswordChanges: LBool = true
)

@Serializable
data class MsGroupsDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("collegeSemesters") val collegeSemesters: List<MsCollegeSemesterDto> = emptyList()
)

@Serializable
data class MsCollegeSemesterDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("number") val number: LInt = 0,
    @SerialName("groups") val groups: List<MsGroupDto> = emptyList()
)

@Serializable
data class MsGroupDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("type") val type: LString = "",
    @SerialName("isPlanVisible") val isPlanVisible: LBool = true,
    @SerialName("editionName") val editionName: LString = ""
)

@Serializable
data class MsStudentPlansDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("studentPlans") val studentPlans: List<MsStudentPlanDto> = emptyList()
)

/** `PlanProfile` enum must be echoed back to `GetPlansSemesters`. */
@Serializable
data class MsStudentPlanDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("planProfile") val planProfile: LString = ""
)

@Serializable
data class MsStudyPlanDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("planSemesters") val planSemesters: List<MsPlanSemesterDto> = emptyList()
)

@Serializable
data class MsPlanSemesterDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("number") val number: LInt = 0,
    @SerialName("planSubjects") val planSubjects: List<MsPlanSubjectDto> = emptyList()
)

@Serializable
data class MsPlanSubjectDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("subjectName") val subjectName: LString = "",
    @SerialName("subjectNumber") val subjectNumber: LInt = 0,
    @SerialName("semestr") val semestr: LInt = 1,
    @SerialName("planId") val planId: LInt = 0,
    @SerialName("form") val form: LString = "",
    @SerialName("passingForm") val passingForm: LString = "",
    @SerialName("hours") val hours: LInt = 0,
    @SerialName("hoursString") val hoursString: LString = "",
    @SerialName("ects") val ects: LDouble = 0.0,
    @SerialName("ectsString") val ectsString: LString = ""
)

@Serializable
data class MsMyClassScheduleDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("dateFrom") val dateFrom: LString? = null,
    @SerialName("dateTo") val dateTo: LString? = null,
    @SerialName("classScheduleGroups") val classScheduleGroups: List<MsClassScheduleItemDto> = emptyList()
)

/**
 * `MsClassScheduleItemDto`: `Date` is `DateTime`, `HourFrom`/`HourTo` are `TimeSpan` ("08:00:00"),
 * `ScheduleType`/`ScheduleIncidentType` are enums. `room`/`comment` are kept for older servers.
 */
@Serializable
data class MsClassScheduleItemDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("onlyHours") val onlyHours: LBool = false,
    @SerialName("form") val form: LString = "",
    @SerialName("group") val group: LString = "",
    @SerialName("date") val date: LString = "",
    @SerialName("hourFrom") val hourFrom: LString = "",
    @SerialName("hourTo") val hourTo: LString = "",
    @SerialName("time") val time: LString = "",
    @SerialName("hours") val hours: LString = "",
    @SerialName("lecturer") val lecturer: LString = "",
    @SerialName("idLecturer") val idLecturer: LInt = 0,
    @SerialName("room") val room: LString = "",
    @SerialName("location") val location: LString = "",
    @SerialName("address") val address: LString = "",
    @SerialName("scheduleIncident") val scheduleIncident: LString? = null,
    @SerialName("scheduleIncidentColor") val scheduleIncidentColor: LString? = null,
    @SerialName("lecturerVisible") val lecturerVisible: LBool = true,
    @SerialName("labelText") val labelText: LString = "",
    @SerialName("labelComments") val labelComments: LString = "",
    @SerialName("comment") val comment: LString = "",
    @SerialName("comments") val comments: LString = ""
)

@Serializable
data class MsSubjectsSemestersDataDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("subjectsSemesters") val subjectsSemesters: List<MsSubjectsSemesterDto> = emptyList()
)

@Serializable
data class MsSubjectsSemesterDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("number") val number: LInt = 0,
    @SerialName("averageGrade") val averageGrade: LDouble? = null,
    @SerialName("state") val state: LString = "",
    @SerialName("sessionFrom") val sessionFrom: LString? = null,
    @SerialName("sessionTo") val sessionTo: LString? = null,
    @SerialName("resitSessionFrom") val resitSessionFrom: LString? = null,
    @SerialName("resitSessionTo") val resitSessionTo: LString? = null,
    @SerialName("subjects") val subjects: List<MsSubjectDto> = emptyList()
)

@Serializable
data class MsSubjectDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("ects") val ects: LString = "",
    @SerialName("ectsString") val ectsString: LString = "",
    @SerialName("form") val form: LString = "",
    @SerialName("passingForm") val passingForm: LString = "",
    @SerialName("grade") val grade: LString = "",
    @SerialName("subjectLecturer") val subjectLecturer: LString = "",
    @SerialName("degreeSubjectLecturer") val degreeSubjectLecturer: LString = "",
    @SerialName("subjectLecturerPosition") val subjectLecturerPosition: LString = "",
    @SerialName("subjectLecturerFullName") val subjectLecturerFullName: LString = ""
)

@Serializable
data class MsSchoolarshipsSemestersDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("schoolarshipSemesters") val schoolarshipSemesters: List<MsSchoolarshipSemesterDto> = emptyList()
)

@Serializable
data class MsSchoolarshipSemesterDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("number") val number: LInt = 0,
    @SerialName("schoolarships") val schoolarships: List<MsSchoolarshipInSemesterDto> = emptyList()
)

@Serializable
data class MsSchoolarshipInSemesterDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("date") val date: LString? = null,
    @SerialName("number") val number: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("state") val state: LString = "",
    @SerialName("schoolarshipGranted") val schoolarshipGranted: LBool = false
)

@Serializable
data class MsScholarshipDetailsDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("typeName") val typeName: LString = "",
    @SerialName("typeVariantName") val typeVariantName: LString = "",
    @SerialName("number") val number: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("applicationDate") val applicationDate: LString? = null,
    @SerialName("applicationStatusString") val applicationStatusString: LString = "",
    @SerialName("ruling") val ruling: MsScholarshipRulingDto? = null,
    @SerialName("rulings") val rulings: List<MsScholarshipRulingDto> = emptyList(),
    @SerialName("accruals") val accruals: MsSchoolarshipPaymentsDto? = null,
    @SerialName("payments") val payments: MsSchoolarshipPaymentsDto? = null
)

@Serializable
data class MsScholarshipRulingDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("number") val number: LString = "",
    @SerialName("name") val name: LString = "",
    @SerialName("date") val date: LString = "",
    @SerialName("statusString") val statusString: LString = "",
    @SerialName("acceptedRules") val acceptedRules: LBool = false,
    @SerialName("acceptedRulesDate") val acceptedRulesDate: LString? = null
)

@Serializable
data class MsSchoolarshipPaymentsDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("number") val number: LInt = 0,
    @SerialName("payment") val payment: LBool = false,
    @SerialName("payments") val payments: List<MsPaymentDto> = emptyList()
)

@Serializable
data class MsPaymentDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("amount") val amount: LString = "",
    @SerialName("date") val date: LString = "",
    @SerialName("status") val status: LString = ""
)

@Serializable
data class MsFinancesDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("arrearsState") val arrearsState: MsArrearsStateDto? = null,
    @SerialName("charges") val charges: MsChargesDto? = null
)

@Serializable
data class MsArrearsStateDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("accountNumber") val accountNumber: LString = "",
    @SerialName("summary") val summary: MsArrearDto? = null
)

@Serializable
data class MsArrearDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("amount") val amount: LString = "0.00 PLN"
)

@Serializable
data class MsChargesDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("charges") val charges: List<MsChargeDto> = emptyList()
)

/** `CurrentState` is `decimal`; `PaymentTerm`/`DateOfPayment` are `DateTime`. */
@Serializable
data class MsChargeDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("amount") val amount: LString = "0.00 PLN",
    @SerialName("amortization") val amortization: LString = "0.00 PLN",
    @SerialName("discount") val discount: LString = "0.00 PLN",
    @SerialName("interest") val interest: LString = "0.00 PLN",
    @SerialName("accumulatedInterest") val accumulatedInterest: LString = "0.00 PLN",
    @SerialName("expired") val expired: LString = "0.00 PLN",
    @SerialName("paymentTerm") val paymentTerm: LString = "",
    @SerialName("dateOfPayment") val dateOfPayment: LString? = null,
    @SerialName("currencyCode") val currencyCode: LString = "PLN",
    @SerialName("currentState") val currentState: LDouble = 0.0,
    @SerialName("status") val status: LString = "",
    @SerialName("payOnlineAvailable") val payOnlineAvailable: LBool = false
)

@Serializable
data class PaymentAnswerDto(
    @SerialName("paymentUrl") val paymentUrl: String? = null,
    @SerialName("error") val error: LString? = null,
    @SerialName("errorMessage") val errorMessage: LString? = null
)

@Serializable
data class MsAnnouncementsListDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("announcementsItems") val announcementsItems: List<MsAnnouncementDto> = emptyList()
)

/** `Priority` is the `AnnouncementPriority` enum — int or name depending on server settings. */
@Serializable
data class MsAnnouncementDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("subject") val subject: LString = "",
    @SerialName("text") val text: LString = "",
    @SerialName("readed") val readed: LBool = false,
    @SerialName("date") val date: LString = "",
    @SerialName("priority") val priority: LString = "1",
    @SerialName("isAttachment") val isAttachment: LBool = false,
    @SerialName("attachments") val attachments: List<MsAnnouncementAttachmentDto> = emptyList()
)

@Serializable
data class MsAnnouncementAttachmentDto(
    @SerialName("id") val id: LInt = 0,
    @SerialName("name") val name: LString = "",
    @SerialName("fileBytes") val fileBytes: String? = null
)

@Serializable
data class MsAnnouncementReadedUpdateInputDto(
    @SerialName("Id") val id: Int,
    @SerialName("PersonId") val personId: Int,
    @SerialName("Readed") val readed: Boolean = true,
    @SerialName("ReadedDate") val readedDate: String
)
