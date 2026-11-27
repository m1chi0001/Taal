package pl.lublin.wsei.plan.data.local

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Base64
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
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
import pl.lublin.wsei.plan.domain.schedule.ScheduleFocusResolver
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Distinct internal debug/test account factory (`Piotr Testowy • Album 99901 • Cyberbezpieczeństwo i AI`).
 *
 * Intentionally uses a distinct field of study, semester (Semestr 5), album number (`99901`),
 * groups (`LAB-CSI-5A`), lecturers, classrooms, and subjects so that the debug account is
 * immediately distinguishable from real student accounts.
 */
object DebugDemoDataFactory {

    private val cachedDemoAvatarBase64: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        generateDemoStudentPortraitBase64()
    }

    private fun warsawLocalDate(nowMillis: Long = System.currentTimeMillis()): LocalDate {
        val jt = Instant.ofEpochMilli(nowMillis).atZone(ScheduleFocusResolver.WARSAW).toLocalDate()
        return LocalDate(jt.year, jt.monthValue, jt.dayOfMonth)
    }

    private fun generateDemoStudentPortraitBase64(): String = runCatching {
        val size = 240
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
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

            // Student monogram badge
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
        } finally {
            bmp.recycle()
        }
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
     * Generates a multi-month demo schedule anchored to [nowMillis] so that:
     * 1. Today always has an active 4-block day for testing the Live Notification and Start Screen progress bar.
     * 2. Multiple days across adjacent weeks and months have classes for calendar navigation testing.
     */
    fun createDemoAcademicCalendarEvents(
        nowMillis: Long,
        scheduleType: ScheduleFilterType = ScheduleFilterType.MY_PLAN
    ): List<ClassEvent> {
        val minuteMs = 60_000L
        val today = warsawLocalDate(nowMillis)
        val todayIso = today.toString()

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
        val today = warsawLocalDate()
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
