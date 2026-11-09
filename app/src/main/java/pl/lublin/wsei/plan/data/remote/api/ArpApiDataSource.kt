package pl.lublin.wsei.plan.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.lublin.wsei.plan.domain.model.AnnouncementAttachment
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.Lecturer
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
import pl.lublin.wsei.plan.domain.model.formatPolishBankAccount
import pl.lublin.wsei.plan.domain.repository.WseiRemoteDataSource
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 100% Reverse-Engineered APR System Sp. z o.o. ("Mobilny Student" / `MobilnyStudent.ApiPa`)
 * REST Client for Lubelska Akademia WSEI w Lublinie (`https://dziekanat.wsei.lublin.pl:20830/`).
 *
 * Implements the exact endpoints, query parameters, ABP response envelopes, and C# DTOs from
 * `MobilnyStudent.ApiPa.dll` and `APRSystem.MobilnyStudent.Application.Shared.dll`.
 */
@Singleton
class ArpApiDataSource @Inject constructor(
    private val httpClient: HttpClient,
    private val sessionCookieJar: ArpSessionCookieJar
) : WseiRemoteDataSource {

    override val sourceOrigin: DataSourceOrigin = DataSourceOrigin.APR_WD_API

    companion object {
        const val WSEI_WD_BASE_URL = "https://dziekanat.wsei.lublin.pl:20830"
        const val APR_TENANT_DIRECTORY_URL =
            "https://api-proklucznik.apr.pl/api/services/app/Tenant/GetMSCustomers"
        private val WARSAW_ZONE = ZoneId.of("Europe/Warsaw")
    }

    /**
     * Authenticates with APR System Wirtualny Dziekanat following `MobilnyStudent.ApiPa.Services.Session.Login`:
     * 1. `POST /api/MobileAuth/Authenticate` (`UserNameOrEmailAddress`, `Password`, `Token`)
     * 2. `GET /api/services/app/Login/GetCoursesOfStudy` -> resolves `StudId` (`CourseOfStudies[0].Id`)
     * 3. `GET /api/services/app/ProfilePages/GetStudentData?Id={studId}&PersonType=Student` -> personal profile, groups & study plans
     * 4. `GET /api/services/app/Dashboard/GetDashboard?Id={studId}&IdLoggedPersonType={studId}&PersonType=Student` -> semester & study averages
     */
    override suspend fun authenticate(
        usernameOrAlbum: String,
        password: String,
        twoFactorToken: String?
    ): Result<StudentSession> = runCatching {
        val authWrapper: AbpAjaxResponse<MobileAuthResponseDto> =
            httpClient.post("$WSEI_WD_BASE_URL/api/MobileAuth/Authenticate") {
                contentType(ContentType.Application.Json)
                setBody(
                    MobileAuthRequestDto(
                        userNameOrEmailAddress = usernameOrAlbum.trim(),
                        password = password,
                        token = twoFactorToken
                    )
                )
            }.body()

        val authResult = authWrapper.result
            ?: error(authWrapper.error?.message ?: "Nie udało się zalogować.")

        if (authResult.shouldResetPassword) {
            error("Konto wymaga zmiany hasła w Wirtualnym Dziekanacie.")
        }
        if (authResult.requiresTwoFactorVerification && twoFactorToken.isNullOrBlank()) {
            error("Konto wymaga kodu weryfikacji dwuetapowej.")
        }

        val accessToken = authResult.accessToken
            ?: error("Serwer nie zwrócił danych logowania. Spróbuj ponownie.")

        sessionCookieJar.bearerToken = accessToken
        sessionCookieJar.savedLogin = usernameOrAlbum.trim()
        sessionCookieJar.savedPassword = password

        // 1. Fetch student's courses of study (`GetCoursesOfStudy`)
        val coursesWrapper: AbpAjaxResponse<MsCoursesOfStudyDto>? = runCatching {
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/Login/GetCoursesOfStudy")
                .body<AbpAjaxResponse<MsCoursesOfStudyDto>>()
        }.getOrNull()

        val allCourses = coursesWrapper?.result?.courseOfStudies.orEmpty()
        val activeCourse = allCourses.firstOrNull { it.aktualny } ?: allCourses.firstOrNull()
        val studId = activeCourse?.id ?: authResult.idLoggedPersonType ?: authResult.personId

        // 2. Fetch student profile details (`GetStudentData`)
        val profileWrapper: AbpAjaxResponse<MsStudentDataDto>? = runCatching {
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/ProfilePages/GetStudentData") {
                parameter("Id", studId)
                parameter("PersonType", "Student")
            }.body<AbpAjaxResponse<MsStudentDataDto>>()
        }.getOrNull()

        // 3. Fetch student dashboard (`GetDashboard`) for exact GPA metrics
        val dashboardWrapper: AbpAjaxResponse<MsDashboardDto>? = runCatching {
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/Dashboard/GetDashboard") {
                parameter("Id", studId)
                parameter("IdLoggedPersonType", studId)
                parameter("PersonType", "Student")
            }.body<AbpAjaxResponse<MsDashboardDto>>()
        }.getOrNull()

        // 4. Fetch course details (`GetCourseOfStudyDetails`) for unread announcements count
        val loginDetailsWrapper: AbpAjaxResponse<MsStudentLoginDetailsDto>? = runCatching {
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/Login/GetCourseOfStudyDetails") {
                parameter("Id", studId)
            }.body<AbpAjaxResponse<MsStudentLoginDetailsDto>>()
        }.getOrNull()

        // 5. Fetch student profile photo from `/api/UserPhotos/GetPersonPictureAsync`
        val fetchedPhotoBase64 = fetchPersonPictureBase64(
            studId = studId,
            personId = authResult.personId
        ) ?: authResult.personPicture?.ifBlank { null }
            ?: profileWrapper?.result?.personalData?.picture?.ifBlank { null }

        val personal = profileWrapper?.result?.personalData
        val courseDash = dashboardWrapper?.result?.courseOfStudy
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
            login = usernameOrAlbum.trim(),
            firstName = personal?.firstName?.ifBlank { null } ?: authResult.firstName.ifBlank { "Student" },
            lastName = personal?.surname?.ifBlank { null } ?: authResult.surname.ifBlank { "WSEI" },
            albumNumber = personal?.albumNumber?.ifBlank { null } ?: usernameOrAlbum.trim(),
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
            semesterNumber = courseDash?.semestr ?: personal?.semestr ?: 1,
            academicYear = courseDash?.year ?: personal?.year ?: 1,
            statusName = courseDash?.status?.ifBlank { null }
                ?: personal?.status?.ifBlank { null }
                ?: "Studiuje",
            averageCurrentSemester = courseDash?.averageCurrentSemester ?: 0.0,
            averagePreviousSemester = courseDash?.averagePreviousSemester ?: 0.0,
            averageGrade = courseDash?.studyAverage ?: 0.0,
            personPictureBase64 = fetchedPhotoBase64,
            allowPasswordChanges = personal?.allowPasswordChanges ?: true,
            unreadAnnouncementCount = loginDetailsWrapper?.result?.unreadAnnouncementCount ?: 0,
            accessToken = accessToken,
            isDebugAccount = false
        )
    }

    /**
     * Fetches the student's profile photo from Wirtualny Dziekanat (`IPhotosController`):
     * `GET /api/UserPhotos/GetPersonPictureAsync?Id={id}&IdLoggedPersonType={studId}&PersonType=Student`
     */
    suspend fun fetchPersonPictureBase64(studId: Int, personId: Int): String? {
        val candidateIds = listOf(studId, personId).distinct().filter { it != 0 }
        for (targetId in candidateIds) {
            val dto = runCatching {
                httpClient.get("$WSEI_WD_BASE_URL/api/UserPhotos/GetPersonPictureAsync") {
                    parameter("Id", targetId)
                    parameter("IdLoggedPersonType", studId)
                    parameter("PersonType", "Student")
                }.body<AbpAjaxResponse<MsPictureDto>>().result
            }.getOrNull()

            val base64 = dto?.picture?.ifBlank { null }
                ?: dto?.imageBytes?.ifBlank { null }
                ?: dto?.fileBytes?.ifBlank { null }
            if (!base64.isNullOrBlank()) {
                return base64
            }
        }
        return null
    }

    /**
     * Changes the student's password via `POST /api/services/app/Profile/ChangePassword`.
     * Matches `MobilnyStudent.ApiPa.Services.Students.Profiles.ChangePassword`.
     */
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = runCatching {
        val wrapper: AbpAjaxResponse<Unit> =
            httpClient.post("$WSEI_WD_BASE_URL/api/services/app/Profile/ChangePassword") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChangePasswordInputDto(
                        currentPassword = currentPassword,
                        newPassword = newPassword
                    )
                )
            }.body()

        if (!wrapper.success && wrapper.error != null) {
            error(wrapper.error.message ?: "Nie udało się zmienić hasła w systemie WD.")
        }
    }

    /**
     * Fetches assigned student groups per semester from `GET /api/services/app/ProfilePages/GetStudentData`.
     * Matches `MobilnyStudent.ApiPa.Services.Students.Groups.GetGroups`.
     */
    override suspend fun fetchStudentGroups(
        session: StudentSession
    ): Result<List<StudentSemesterGroups>> = runCatching {
        val wrapper: AbpAjaxResponse<MsStudentDataDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/ProfilePages/GetStudentData") {
                parameter("Id", session.studId)
                parameter("PersonType", "Student")
            }.body()

        val collegeSemesters = wrapper.result?.groups?.collegeSemesters.orEmpty().reversed()
        collegeSemesters.map { sem ->
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
     * Fetches study plans (`Plany studiów`) and their semester subject breakdown:
     * 1. `GET /api/services/app/ProfilePages/GetStudentData` -> `StudentPlans.StudentPlans`
     * 2. `GET /api/services/app/ProfilePages/GetPlansSemesters?Id={planId}&PlanProfile=NoDefined` -> `MsStudyPlanDto`
     * Matches `MobilnyStudent.ApiPa.Services.Students.Syllabuses.GetSyllabuses` & `GetSyllabusById`.
     */
    override suspend fun fetchStudyPlans(
        session: StudentSession
    ): Result<List<StudyPlanOverview>> = runCatching {
        val studentDataWrapper: AbpAjaxResponse<MsStudentDataDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/ProfilePages/GetStudentData") {
                parameter("Id", session.studId)
                parameter("PersonType", "Student")
            }.body()

        val plans = studentDataWrapper.result?.studentPlans?.studentPlans.orEmpty()
        plans.map { plan ->
            val semestersWrapper: AbpAjaxResponse<MsStudyPlanDto>? = runCatching {
                httpClient.get("$WSEI_WD_BASE_URL/api/services/app/ProfilePages/GetPlansSemesters") {
                    parameter("Id", plan.id)
                    parameter("PlanProfile", "NoDefined")
                }.body<AbpAjaxResponse<MsStudyPlanDto>>()
            }.getOrNull()

            val planSemesters = semestersWrapper?.result?.planSemesters.orEmpty().reversed().map { sem ->
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
                            hours = subj.hours,
                            ects = subj.ects,
                            semesterNumber = subj.semestr
                        )
                    }
                )
            }

            StudyPlanOverview(
                planId = plan.id,
                planName = plan.name.ifBlank { session.courseOfStudyCode },
                semesters = planSemesters
            )
        }
    }

    /**
     * Fetches the student's timetable from `/api/services/app/ScheduleData/GetMyClassSchedule`.
     * Matches `MobilnyStudent.ApiPa.Services.ClassSchedules.GetSchedule`:
     * - `Id`: `session.studId`
     * - `ScheduleType`: `Student` (`5`) or `GroupsForStudent` (`100`)
     * - `DateFrom`, `DateTo`: ISO-8601 `yyyy-MM-ddTHH:mm:ss`
     * - `PersonType`: `Student` (`1`)
     */
    override suspend fun fetchSchedule(
        session: StudentSession,
        from: LocalDate,
        to: LocalDate,
        scheduleType: ScheduleFilterType
    ): Result<List<ClassEvent>> = runCatching {
        val wrapper: AbpAjaxResponse<MsMyClassScheduleDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/ScheduleData/GetMyClassSchedule") {
                parameter("Id", session.studId)
                parameter("ScheduleType", scheduleType.apiEnumName)
                parameter("DateFrom", "${from}T00:00:00")
                parameter("DateTo", "${to}T23:59:59")
                parameter("PersonType", "Student")
            }.body()

        val scheduleDto = wrapper.result
            ?: error(wrapper.error?.message ?: "Nie udało się pobrać planu zajęć z WD.")

        scheduleDto.classScheduleGroups
            .mapIndexed { index, item -> item.toDomainModel(index) }
    }

    /**
     * Fetches subjects, ECTS, and grades from `/api/services/app/SubjectsData/GetSubjectsData`.
     * Matches `MobilnyStudent.ApiPa.Services.Students.Subjects.GetSubjects`.
     */
    override suspend fun fetchGrades(session: StudentSession): Result<List<GradeItem>> = runCatching {
        val wrapper: AbpAjaxResponse<MsSubjectsSemestersDataDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/SubjectsData/GetSubjectsData") {
                parameter("Id", session.studId)
            }.body()

        val semesters = wrapper.result?.subjectsSemesters.orEmpty().reversed()
        semesters.flatMap { sem ->
            val semLabel = sem.name.ifBlank { "Semestr ${sem.number}" }
            val sessionDateFormatted = sem.sessionTo?.take(10) ?: sem.sessionFrom?.take(10) ?: ""
            sem.subjects.mapIndexed { idx, subj ->
                val ectsInt = subj.ects.replace(",", ".").toDoubleOrNull()?.roundToInt() ?: 0
                val fullLecturer = listOf(subj.degreeSubjectLecturer, subj.subjectLecturer)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                GradeItem(
                    id = "${sem.id}_${subj.id}_$idx",
                    subjectId = subj.id,
                    semesterLabel = semLabel,
                    semesterNumber = sem.number,
                    subjectName = subj.name,
                    moduleName = "Moduł ogólny",
                    classType = ClassType.fromRawCode(subj.form),
                    formLabel = subj.form.ifBlank { ClassType.fromRawCode(subj.form).polishLabel },
                    gradeValue = subj.grade.ifBlank { "—" },
                    passingForm = subj.passingForm.ifBlank { "Zaliczenie ocena" },
                    ects = ectsInt,
                    lecturerName = fullLecturer.ifBlank { "Wykładowca WSEI" },
                    dateAwarded = sessionDateFormatted
                )
            }
        }
    }

    /**
     * Fetches scholarships per semester and their detailed rulings/payouts from:
     * 1. `GET /api/services/app/SchoolarshipData/GetSchoolarshipsSemestersData?Id={studId}`
     * 2. `GET /api/services/app/SchoolarshipData/GetScholarshipDetailsAsync?Id={id}&PersonType=Student&IdLoggedPersonType={studId}`
     * Matches `MobilnyStudent.ApiPa.Services.Students.Scholarships.GetScholarships` & `GetScholarshipDetails`.
     */
    override suspend fun fetchScholarships(
        session: StudentSession
    ): Result<List<ScholarshipApplication>> = runCatching {
        val wrapper: AbpAjaxResponse<MsSchoolarshipsSemestersDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/SchoolarshipData/GetSchoolarshipsSemestersData") {
                parameter("Id", session.studId)
            }.body()

        val semesters = wrapper.result?.schoolarshipSemesters.orEmpty().reversed()
        semesters.flatMap { sem ->
            val semLabel = sem.name.ifBlank { "Semestr ${sem.number}" }
            sem.schoolarships.map { item ->
                val details: MsScholarshipDetailsDto? = runCatching {
                    httpClient.get("$WSEI_WD_BASE_URL/api/services/app/SchoolarshipData/GetScholarshipDetailsAsync") {
                        parameter("Id", item.id)
                        parameter("PersonType", "Student")
                        parameter("IdLoggedPersonType", session.studId)
                    }.body<AbpAjaxResponse<MsScholarshipDetailsDto>>().result
                }.getOrNull()

                val firstRuling = details?.rulings?.firstOrNull()
                val payouts = details?.payments?.payments.orEmpty().map { p ->
                    ScholarshipPayoutItem(
                        id = p.id,
                        amountFormatted = p.amount,
                        dateIso = formatIsoDateToPolish(p.date),
                        status = p.status
                    )
                }

                ScholarshipApplication(
                    id = item.id,
                    semesterLabel = semLabel,
                    number = item.number,
                    title = details?.name?.ifBlank { null } ?: item.name,
                    typeVariantName = details?.typeVariantName.orEmpty(),
                    submissionDateIso = formatIsoDateToPolish(details?.applicationDate ?: item.date ?: ""),
                    statusString = details?.applicationStatusString?.ifBlank { null }
                        ?: item.state.ifBlank { "Złożony" },
                    isGranted = item.schoolarshipGranted,
                    decisionNumber = firstRuling?.number,
                    decisionDateIso = firstRuling?.date?.let(::formatIsoDateToPolish),
                    acceptedRules = firstRuling?.acceptedRules ?: true,
                    payouts = payouts
                )
            }
        }
    }

    /**
     * Fetches tuition charges, installment breakdown, and individual virtual bank account number from
     * `GET /api/services/app/FinancesData/GetFinancesData`.
     * Matches `MobilnyStudent.ApiPa.Services.Students.Finances.GetSummary` & `GetCharges`.
     */
    override suspend fun fetchFinancials(session: StudentSession): Result<List<FinancialObligation>> =
        runCatching {
            val wrapper: AbpAjaxResponse<MsFinancesDto> =
                httpClient.get("$WSEI_WD_BASE_URL/api/services/app/FinancesData/GetFinancesData") {
                    parameter("ChargeType", "Normal")
                    parameter("Id", session.studId)
                    parameter("IdLoggedPersonType", session.studId)
                }.body()

            val finances = wrapper.result
            val accountNumber = formatPolishBankAccount(finances?.arrearsState?.accountNumber.orEmpty())
            val chargesList = finances?.charges?.charges.orEmpty()

            chargesList.mapIndexed { idx, charge ->
                val parsedAmount = parseMoneyAmount(charge.amount)
                val isPaid = charge.currentState == 0.0 ||
                    charge.status.contains("Opłac", ignoreCase = true) ||
                    charge.status.contains("Paid", ignoreCase = true)
                val expiredMoney = parseMoneyAmount(charge.expired)

                FinancialObligation(
                    id = "${charge.id}_$idx",
                    chargeId = charge.id,
                    title = charge.name,
                    amountPln = parsedAmount,
                    amountFormatted = charge.amount.ifBlank { String.format(Locale.US, "%.2f %s", parsedAmount, charge.currencyCode) },
                    currentStatePln = charge.currentState,
                    dueDateIso = formatIsoDateToPolish(charge.paymentTerm),
                    paymentDateIso = charge.dateOfPayment?.let(::formatIsoDateToPolish),
                    statusLabel = charge.status.ifBlank { if (isPaid) "Opłacono" else "Do uregulowania" },
                    interestFormatted = charge.interest.ifBlank { "0.00 PLN" },
                    accumulatedInterestFormatted = charge.accumulatedInterest.ifBlank { "0.00 PLN" },
                    amortizationFormatted = charge.amortization.ifBlank { "0.00 PLN" },
                    discountFormatted = charge.discount.ifBlank { "0.00 PLN" },
                    expiredFormatted = charge.expired.ifBlank { "0.00 PLN" },
                    isPaid = isPaid,
                    isExpired = expiredMoney > 0.0,
                    payOnlineAvailable = charge.payOnlineAvailable,
                    bankAccountNumber = accountNumber
                )
            }
        }

    /**
     * Initiates an online payment (`Zapłać online`) via BlueMedia / Autopay gateway:
     * `GET /api/services/app/BlueMedia/GetStartPaymentAsync?chargeId={chargeId}&price={amount}`
     * Matches `MobilnyStudent.ApiPa.Services.Students.Finances.GetPaymentUri`.
     */
    override suspend fun fetchOnlinePaymentUrl(
        chargeId: Int,
        amountPln: Double
    ): Result<String> = runCatching {
        val formattedPrice = String.format(Locale.US, "%.2f", kotlin.math.abs(amountPln))
        val wrapper: AbpAjaxResponse<PaymentAnswerDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/BlueMedia/GetStartPaymentAsync") {
                parameter("chargeId", chargeId)
                parameter("price", formattedPrice)
            }.body()

        val paymentUrl = wrapper.result?.paymentUrl
        if (paymentUrl.isNullOrBlank()) {
            error(wrapper.result?.errorMessage ?: wrapper.error?.message ?: "Brak linku do płatności online BlueMedia.")
        }
        paymentUrl
    }

    /**
     * Fetches dean's office announcements from `/api/services/app/AnnouncementsData/GetAnnouncementsList`.
     * Matches `MobilnyStudent.ApiPa.Services.Inbox.GetMessages`.
     */
    override suspend fun fetchAnnouncements(session: StudentSession): Result<List<DeanAnnouncement>> =
        runCatching {
            val wrapper: AbpAjaxResponse<MsAnnouncementsListDto> =
                httpClient.get("$WSEI_WD_BASE_URL/api/services/app/AnnouncementsData/GetAnnouncementsList") {
                    parameter("PersonType", "Student")
                    parameter("IdLoggedPersonType", session.studId)
                }.body()

            wrapper.result?.announcementsItems.orEmpty().mapIndexed { idx, item ->
                val cleanBody = item.text
                    .replace(Regex("<[^>]*>"), " ")
                    .replace("&nbsp;", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                val senderName = item.name.ifBlank { "Dziekanat" }
                DeanAnnouncement(
                    id = "${item.id}_$idx",
                    announcementId = item.id,
                    title = item.subject.ifBlank { item.name },
                    bodySnippet = cleanBody.take(180),
                    fullText = cleanBody.ifBlank { item.subject },
                    publishedAtIso = formatIsoDateToPolish(item.date),
                    priorityLevel = item.priority,
                    isImportant = item.priority == 0, // AnnouncementPriority.High = 0
                    isRead = item.readed,
                    hasAttachment = item.isAttachment,
                    attachments = item.attachments.map { att ->
                        AnnouncementAttachment(
                            id = att.id,
                            fileName = att.name
                        )
                    },
                    authorDepartment = senderName,
                    senderInitial = senderName.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
                )
            }
        }

    /**
     * Marks an announcement as read via `PUT /api/services/app/AnnouncementsData/UpdateAnnouncementReaded`.
     * Matches `MobilnyStudent.ApiPa.Services.Inbox.GetMessageById`.
     */
    override suspend fun markAnnouncementRead(
        session: StudentSession,
        announcementId: Int
    ): Result<Unit> = runCatching {
        val nowIso = LocalDateTime.now(WARSAW_ZONE).toString()
        httpClient.put("$WSEI_WD_BASE_URL/api/services/app/AnnouncementsData/UpdateAnnouncementReaded") {
            contentType(ContentType.Application.Json)
            setBody(
                MsAnnouncementReadedUpdateInputDto(
                    id = announcementId,
                    personId = session.personId,
                    readed = true,
                    readedDate = nowIso
                )
            )
        }
    }

    /**
     * Downloads binary attachment bytes from `GET /api/services/app/AnnouncementsData/GetAttachmentBytes`.
     * Matches `MobilnyStudent.ApiPa.Services.Inbox.GetAttachmentById`.
     */
    override suspend fun downloadAnnouncementAttachment(
        session: StudentSession,
        attachmentId: Int
    ): Result<AnnouncementAttachment> = runCatching {
        val wrapper: AbpAjaxResponse<MsAnnouncementAttachmentDto> =
            httpClient.get("$WSEI_WD_BASE_URL/api/services/app/AnnouncementsData/GetAttachmentBytes") {
                parameter("Id", attachmentId)
                parameter("PersonType", "Student")
                parameter("IdLoggedPersonType", session.studId)
            }.body()

        val dto = wrapper.result ?: error("Nie udało się pobrać załącznika.")
        AnnouncementAttachment(
            id = dto.id,
            fileName = dto.name.ifBlank { "zalacznik_$attachmentId.pdf" },
            fileBytesBase64 = dto.fileBytes
        )
    }

    private fun MsClassScheduleItemDto.toDomainModel(fallbackIndex: Int): ClassEvent {
        val dateOnlyIso = date.take(10)
        val startHhMm = extractTimeHhMm(hourFrom, fallback = time.substringBefore("-").trim().ifBlank { "08:00" })
        val endHhMm = extractTimeHhMm(hourTo, fallback = time.substringAfter("-", "09:30").trim().ifBlank { "09:30" })

        val startMillis = parseToEpochMillis(dateOnlyIso, startHhMm)
        val endMillis = parseToEpochMillis(dateOnlyIso, endHhMm)
            .let { if (it <= startMillis) startMillis + 90 * 60_000L else it }

        val subjectDisplayName = name.ifBlank { labelText.ifBlank { "Zajęcia dydaktyczne" } }
        val rawLocation = location
            .ifBlank { room }
            .removePrefix("Sala")
            .removePrefix("sala")
            .removePrefix("s.")
            .trim()

        val combinedMeta = "$rawLocation $form $comments $comment $labelText $name"
        val isElearning = combinedMeta.contains("Elrn", ignoreCase = true) ||
            combinedMeta.contains("E-learn", ignoreCase = true) ||
            combinedMeta.contains("Elearning", ignoreCase = true) ||
            combinedMeta.contains("Moodle", ignoreCase = true)

        // Na Uniwersytecie WSEI zajęcia stacjonarne mają przypisaną konkretną salę (np. AULA A, C209K, 205K),
        // natomiast zajęcia streamingowe mają puste pole sali lub adnotację Streaming / Teams / Zdalnie / Online.
        val isStreaming = !isElearning && (
            rawLocation.isBlank() ||
                rawLocation == "-" ||
                rawLocation.equals("Brak", ignoreCase = true) ||
                combinedMeta.contains("Stream", ignoreCase = true) ||
                combinedMeta.contains("Teams", ignoreCase = true) ||
                combinedMeta.contains("Zdaln", ignoreCase = true) ||
                combinedMeta.contains("Online", ignoreCase = true) ||
                combinedMeta.contains("Webinar", ignoreCase = true) ||
                combinedMeta.contains("Transmisja", ignoreCase = true)
            )

        val isRemote = isElearning || isStreaming

        val deliveryMode = when {
            isElearning -> DeliveryMode.ELEARNING
            isStreaming -> DeliveryMode.REMOTE
            else -> DeliveryMode.ON_SITE
        }

        val roomClean = when {
            isElearning -> "E-learning"
            isStreaming -> "Streaming"
            else -> rawLocation
        }

        val roomBuildingLabel = when {
            isElearning -> "Platforma e-learningowa Uniwersytetu WSEI"
            isStreaming -> "Zajęcia zdalne (Streaming)"
            else -> "Kampus Uniwersytetu WSEI • Projektowa 4"
        }

        val lecturerParts = lecturer.trim().split(" ").filter { it.isNotBlank() }
        val parsedLecturer = if (lecturerParts.size >= 2) {
            Lecturer(
                id = idLecturer.toString(),
                academicTitle = lecturerParts.dropLast(2).joinToString(" "),
                firstName = lecturerParts[lecturerParts.size - 2],
                lastName = lecturerParts.last()
            )
        } else {
            Lecturer(
                id = idLecturer.toString(),
                academicTitle = "",
                firstName = lecturer,
                lastName = ""
            )
        }

        val mergedNotes = listOf(comments.trim(), comment.trim())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" • ")
            .ifBlank { null }

        return ClassEvent(
            id = if (id != 0) "wd_${id}_${dateOnlyIso}_$startHhMm" else "wd_${dateOnlyIso}_${startHhMm}_$fallbackIndex",
            dateIso = dateOnlyIso,
            subject = Subject(
                id = subjectDisplayName.lowercase().replace(" ", "_"),
                name = subjectDisplayName,
                shortCode = subjectDisplayName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                    .take(4)
            ),
            classType = ClassType.fromRawCode(form),
            lecturer = parsedLecturer,
            room = Room(
                number = roomClean,
                building = roomBuildingLabel,
                isRemoteTeams = isRemote
            ),
            timeSlot = TimeSlot(
                startEpochMillis = startMillis,
                endEpochMillis = endMillis,
                startFormatted = startHhMm,
                endFormatted = endHhMm
            ),
            groupCode = group.ifBlank { "Grupa dziekanatowa" },
            deliveryMode = deliveryMode,
            address = address.ifBlank { null },
            notes = mergedNotes,
            incidentAlert = scheduleIncident?.takeIf { it.isNotBlank() },
            origin = DataSourceOrigin.APR_WD_API
        )
    }

    /**
     * Matches `AmountStringToMoney.MapFrom(string? value)` from `MobilnyStudent.ApiPa.Mappings`.
     */
    private fun parseMoneyAmount(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val token = raw.trim().split(" ").firstOrNull().orEmpty().replace(",", ".")
        return token.toDoubleOrNull() ?: 0.0
    }

    private fun formatIsoDateToPolish(rawIso: String): String {
        val datePart = rawIso.take(10)
        val parts = datePart.split("-")
        return if (parts.size == 3) {
            "${parts[2]}.${parts[1]}.${parts[0]}"
        } else {
            datePart
        }
    }

    private fun extractTimeHhMm(raw: String, fallback: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 16 && trimmed.contains("T")) {
            return trimmed.substringAfter("T").take(5)
        }
        if (trimmed.length >= 5 && trimmed[2] == ':') {
            return trimmed.take(5)
        }
        return fallback
    }

    private fun parseToEpochMillis(dateIso: String, hhMm: String): Long = runCatching {
        LocalDateTime.parse("${dateIso}T${hhMm}:00")
            .atZone(WARSAW_ZONE)
            .toInstant()
            .toEpochMilli()
    }.getOrDefault(System.currentTimeMillis())
}

// =========================================================================================
// Exact 1:1 ABP & APR System Mobilny Student DTOs (`APRSystem.coreTest.MS.*` & `ApiPa`)
// =========================================================================================

@Serializable
data class AbpAjaxResponse<T>(
    @SerialName("result") val result: T? = null,
    @SerialName("targetUrl") val targetUrl: String? = null,
    @SerialName("success") val success: Boolean = true,
    @SerialName("unAuthorizedRequest") val unAuthorizedRequest: Boolean = false,
    @SerialName("error") val error: AbpErrorInfo? = null,
    @SerialName("__abp") val abp: Boolean = true
)

@Serializable
data class AbpErrorInfo(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String? = null,
    @SerialName("details") val details: String? = null
)

@Serializable
data class MobileAuthRequestDto(
    @SerialName("UserNameOrEmailAddress") val userNameOrEmailAddress: String,
    @SerialName("Password") val password: String,
    @SerialName("Token") val token: String? = null
)

@Serializable
data class MobileAuthResponseDto(
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("shouldResetPassword") val shouldResetPassword: Boolean = false,
    @SerialName("requiresTwoFactorVerification") val requiresTwoFactorVerification: Boolean = false,
    @SerialName("userId") val userId: Long = 0L,
    @SerialName("personId") val personId: Int = 0,
    @SerialName("personType") val personType: Int = 1,
    @SerialName("idLoggedPersonType") val idLoggedPersonType: Int? = null,
    @SerialName("personPicture") val personPicture: String? = null,
    @SerialName("firstName") val firstName: String = "",
    @SerialName("surname") val surname: String = ""
)

@Serializable
data class ChangePasswordInputDto(
    @SerialName("CurrentPassword") val currentPassword: String,
    @SerialName("NewPassword") val newPassword: String
)

@Serializable
data class MsCoursesOfStudyDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("courseOfStudies") val courseOfStudies: List<MsCourseOfStudyDto> = emptyList()
)

@Serializable
data class MsCourseOfStudyDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("fieldOfStudy") val fieldOfStudy: String = "",
    @SerialName("obtainedTitle") val obtainedTitle: String = "",
    @SerialName("nameOfStudies") val nameOfStudies: String = "",
    @SerialName("aktualny") val aktualny: Boolean = false,
    @SerialName("studyDateFrom") val studyDateFrom: String? = null,
    @SerialName("studyDateTo") val studyDateTo: String? = null
)

@Serializable
data class MsPictureDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("pictureVisible") val pictureVisible: Boolean = true,
    @SerialName("picture") val picture: String? = null,
    @SerialName("imageBytes") val imageBytes: String? = null,
    @SerialName("fileBytes") val fileBytes: String? = null
)

@Serializable
data class MsStudentLoginDetailsDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("unreadAnnouncementCount") val unreadAnnouncementCount: Int = 0,
    @SerialName("studyDateFrom") val studyDateFrom: String? = null,
    @SerialName("studyDateTo") val studyDateTo: String? = null
)

@Serializable
data class MsDashboardDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("arrearsState") val arrearsState: MsArrearsStateDto? = null,
    @SerialName("courseOfStudy") val courseOfStudy: MsCourseOfStudyDashboardDto? = null,
    @SerialName("schoolarship") val schoolarship: MsSchoolarshipForDashboardDto? = null,
    @SerialName("classScheduleGroups") val classScheduleGroups: List<MsClassScheduleItemDto> = emptyList(),
    @SerialName("arrearsIsCloseToLimit") val arrearsIsCloseToLimit: Boolean = false,
    @SerialName("arrearsIsOverLimit") val arrearsIsOverLimit: Boolean = false
)

@Serializable
data class MsCourseOfStudyDashboardDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("semestr") val semestr: Int = 1,
    @SerialName("status") val status: String = "",
    @SerialName("year") val year: Int = 1,
    @SerialName("averageCurrentSemester") val averageCurrentSemester: Double = 0.0,
    @SerialName("averagePreviousSemester") val averagePreviousSemester: Double = 0.0,
    @SerialName("studyAverage") val studyAverage: Double = 0.0
)

@Serializable
data class MsSchoolarshipForDashboardDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("payments") val payments: List<MsPaymentDto> = emptyList()
)

@Serializable
data class MsStudentDataDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("personalData") val personalData: MsProfileDataDto? = null,
    @SerialName("groups") val groups: MsGroupsDto? = null,
    @SerialName("studentPlans") val studentPlans: MsStudentPlansDto? = null
)

@Serializable
data class MsProfileDataDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("firstName") val firstName: String = "",
    @SerialName("surname") val surname: String = "",
    @SerialName("albumNumber") val albumNumber: String = "",
    @SerialName("courseOfStudyName") val courseOfStudyName: String = "",
    @SerialName("department") val department: String = "",
    @SerialName("fieldOfStudy") val fieldOfStudy: String = "",
    @SerialName("obtainedTitle") val obtainedTitle: String = "",
    @SerialName("nameOfStudies") val nameOfStudies: String = "",
    @SerialName("semestr") val semestr: Int = 1,
    @SerialName("year") val year: Int = 1,
    @SerialName("status") val status: String = "",
    @SerialName("picture") val picture: String? = null,
    @SerialName("allowPasswordChanges") val allowPasswordChanges: Boolean = true
)

@Serializable
data class MsGroupsDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("collegeSemesters") val collegeSemesters: List<MsCollegeSemesterDto> = emptyList()
)

@Serializable
data class MsCollegeSemesterDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("groups") val groups: List<MsGroupDto> = emptyList()
)

@Serializable
data class MsGroupDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("isPlanVisible") val isPlanVisible: Boolean = true,
    @SerialName("editionName") val editionName: String = ""
)

@Serializable
data class MsStudentPlansDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("studentPlans") val studentPlans: List<MsStudentPlanDto> = emptyList()
)

@Serializable
data class MsStudentPlanDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = ""
)

@Serializable
data class MsStudyPlanDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("planSemesters") val planSemesters: List<MsPlanSemesterDto> = emptyList()
)

@Serializable
data class MsPlanSemesterDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("planSubjects") val planSubjects: List<MsPlanSubjectDto> = emptyList()
)

@Serializable
data class MsPlanSubjectDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("subjectName") val subjectName: String = "",
    @SerialName("subjectNumber") val subjectNumber: Int = 0,
    @SerialName("semestr") val semestr: Int = 1,
    @SerialName("planId") val planId: Int = 0,
    @SerialName("form") val form: String = "",
    @SerialName("passingForm") val passingForm: String = "",
    @SerialName("hours") val hours: Int = 0,
    @SerialName("ects") val ects: Float = 0f
)

@Serializable
data class MsPlanModuleDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("planSubject") val planSubject: MsPlanSubjectDto? = null,
    @SerialName("moduleForms") val moduleForms: List<MsPlanSubjectDto> = emptyList()
)

@Serializable
data class MsMyClassScheduleDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("dateFrom") val dateFrom: String? = null,
    @SerialName("dateTo") val dateTo: String? = null,
    @SerialName("classScheduleGroups") val classScheduleGroups: List<MsClassScheduleItemDto> = emptyList()
)

@Serializable
data class MsClassScheduleItemDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("onlyHours") val onlyHours: Boolean = false,
    @SerialName("form") val form: String = "",
    @SerialName("group") val group: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("hourFrom") val hourFrom: String = "",
    @SerialName("hourTo") val hourTo: String = "",
    @SerialName("time") val time: String = "",
    @SerialName("lecturer") val lecturer: String = "",
    @SerialName("idLecturer") val idLecturer: Int = 0,
    @SerialName("room") val room: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("address") val address: String = "",
    @SerialName("scheduleIncident") val scheduleIncident: String? = null,
    @SerialName("scheduleIncidentColor") val scheduleIncidentColor: String? = null,
    @SerialName("lecturerVisible") val lecturerVisible: Boolean = true,
    @SerialName("labelText") val labelText: String = "",
    @SerialName("comment") val comment: String = "",
    @SerialName("comments") val comments: String = ""
)

@Serializable
data class MsSubjectsSemestersDataDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("subjectsSemesters") val subjectsSemesters: List<MsSubjectsSemesterDto> = emptyList()
)

@Serializable
data class MsSubjectsSemesterDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("averageGrade") val averageGrade: Double? = null,
    @SerialName("state") val state: String = "",
    @SerialName("sessionFrom") val sessionFrom: String? = null,
    @SerialName("sessionTo") val sessionTo: String? = null,
    @SerialName("resitSessionFrom") val resitSessionFrom: String? = null,
    @SerialName("resitSessionTo") val resitSessionTo: String? = null,
    @SerialName("subjects") val subjects: List<MsSubjectDto> = emptyList()
)

@Serializable
data class MsSubjectDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("ects") val ects: String = "0",
    @SerialName("form") val form: String = "",
    @SerialName("passingForm") val passingForm: String = "",
    @SerialName("grade") val grade: String = "",
    @SerialName("subjectLecturer") val subjectLecturer: String = "",
    @SerialName("degreeSubjectLecturer") val degreeSubjectLecturer: String = "",
    @SerialName("subjectLecturerPosition") val subjectLecturerPosition: String = ""
)

@Serializable
data class MsSchoolarshipsSemestersDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("schoolarshipSemesters") val schoolarshipSemesters: List<MsSchoolarshipSemesterDto> = emptyList()
)

@Serializable
data class MsSchoolarshipSemesterDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("schoolarships") val schoolarships: List<MsSchoolarshipInSemesterDto> = emptyList()
)

@Serializable
data class MsSchoolarshipInSemesterDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("date") val date: String? = null,
    @SerialName("number") val number: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("schoolarshipGranted") val schoolarshipGranted: Boolean = false
)

@Serializable
data class MsScholarshipDetailsDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("typeName") val typeName: String = "",
    @SerialName("typeVariantName") val typeVariantName: String = "",
    @SerialName("number") val number: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("applicationDate") val applicationDate: String? = null,
    @SerialName("applicationStatusString") val applicationStatusString: String = "",
    @SerialName("rulings") val rulings: List<MsScholarshipRulingDto> = emptyList(),
    @SerialName("accruals") val accruals: MsSchoolarshipPaymentsDto? = null,
    @SerialName("payments") val payments: MsSchoolarshipPaymentsDto? = null
)

@Serializable
data class MsScholarshipRulingDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("number") val number: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("statusString") val statusString: String = "",
    @SerialName("acceptedRules") val acceptedRules: Boolean = false,
    @SerialName("acceptedRulesDate") val acceptedRulesDate: String? = null
)

@Serializable
data class MsSchoolarshipPaymentsDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("number") val number: Int = 0,
    @SerialName("payment") val payment: Boolean = false,
    @SerialName("payments") val payments: List<MsPaymentDto> = emptyList()
)

@Serializable
data class MsPaymentDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("amount") val amount: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("status") val status: String = ""
)

@Serializable
data class MsFinancesDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("arrearsState") val arrearsState: MsArrearsStateDto? = null,
    @SerialName("charges") val charges: MsChargesDto? = null
)

@Serializable
data class MsArrearsStateDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("accountNumber") val accountNumber: String = "",
    @SerialName("summary") val summary: MsArrearDto? = null
)

@Serializable
data class MsArrearDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("amount") val amount: String = "0.00 PLN"
)

@Serializable
data class MsChargesDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("charges") val charges: List<MsChargeDto> = emptyList()
)

@Serializable
data class MsChargeDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("amount") val amount: String = "0.00 PLN",
    @SerialName("amortization") val amortization: String = "0.00 PLN",
    @SerialName("discount") val discount: String = "0.00 PLN",
    @SerialName("interest") val interest: String = "0.00 PLN",
    @SerialName("accumulatedInterest") val accumulatedInterest: String = "0.00 PLN",
    @SerialName("expired") val expired: String = "0.00 PLN",
    @SerialName("paymentTerm") val paymentTerm: String = "",
    @SerialName("dateOfPayment") val dateOfPayment: String? = null,
    @SerialName("currencyCode") val currencyCode: String = "PLN",
    @SerialName("currentState") val currentState: Double = 0.0,
    @SerialName("status") val status: String = "",
    @SerialName("payOnlineAvailable") val payOnlineAvailable: Boolean = false
)

@Serializable
data class PaymentAnswerDto(
    @SerialName("paymentUrl") val paymentUrl: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null
)

@Serializable
data class MsAnnouncementsListDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("announcementsItems") val announcementsItems: List<MsAnnouncementDto> = emptyList()
)

@Serializable
data class MsAnnouncementDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("subject") val subject: String = "",
    @SerialName("text") val text: String = "",
    @SerialName("readed") val readed: Boolean = false,
    @SerialName("date") val date: String = "",
    @SerialName("priority") val priority: Int = 1,
    @SerialName("isAttachment") val isAttachment: Boolean = false,
    @SerialName("attachments") val attachments: List<MsAnnouncementAttachmentDto> = emptyList()
)

@Serializable
data class MsAnnouncementAttachmentDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("fileBytes") val fileBytes: String? = null
)

@Serializable
data class MsAnnouncementReadedUpdateInputDto(
    @SerialName("Id") val id: Int,
    @SerialName("PersonId") val personId: Int,
    @SerialName("Readed") val readed: Boolean = true,
    @SerialName("ReadedDate") val readedDate: String
)
