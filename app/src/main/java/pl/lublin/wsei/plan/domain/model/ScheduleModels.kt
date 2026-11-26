package pl.lublin.wsei.plan.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Clean, @Immutable domain models for Lubelska Akademia WSEI w Lublinie
 * powered by APR System Sp. z o.o. (`MobilnyStudent.ApiPa` & `APRSystem.MobilnyStudent.Application.Shared`).
 */

@Immutable
@Serializable
enum class ClassType(
    val polishLabel: String,
    val shortBadge: String
) {
    LECTURE(polishLabel = "Wykład", shortBadge = "WYK"),
    LAB(polishLabel = "Laboratorium", shortBadge = "LAB"),
    EXERCISE(polishLabel = "Ćwiczenia", shortBadge = "ĆW"),
    PROJECT(polishLabel = "Projekt", shortBadge = "PROJ"),
    ELEARNING(polishLabel = "E-learning", shortBadge = "ELRN"),
    PRACTICE(polishLabel = "Prakt zaw zajęcia", shortBadge = "PZZ"),
    SEMINAR(polishLabel = "Seminarium", shortBadge = "SEM"),
    EXAM(polishLabel = "Egzamin / Zaliczenie", shortBadge = "EGZ");

    companion object {
        fun fromRawCode(raw: String): ClassType {
            val normalized = raw.trim().lowercase()
            return when {
                normalized.contains("elrn") || normalized.contains("e-learn") || normalized.contains("elearn") -> ELEARNING
                normalized.contains("prakt") || normalized.contains("pzz") -> PRACTICE
                normalized.contains("lab") || normalized == "l" -> LAB
                normalized.contains("ćw") || normalized.contains("cw") || normalized.contains("konw") || normalized == "c" -> EXERCISE
                normalized.contains("proj") || normalized == "p" -> PROJECT
                normalized.contains("sem") || normalized == "s" -> SEMINAR
                normalized.contains("egz") || normalized.contains("zal") -> EXAM
                else -> LECTURE
            }
        }
    }
}

@Immutable
@Serializable
enum class DeliveryMode(val polishBadge: String) {
    ON_SITE("Stacjonarnie"),
    REMOTE("Streaming"),
    ELEARNING("E-learning")
}

/**
 * Formats a Polish 26-digit NRB bank account number into the easy-to-read grouped format:
 * `XX XXXX XXXX XXXX XXXX XXXX XXXX` (e.g. `86 1020 3147 2006 0000 0609 3118`).
 */
fun formatPolishBankAccount(raw: String): String {
    val digitsOnly = raw.filter { it.isDigit() }
    if (digitsOnly.length == 26) {
        val checkDigits = digitsOnly.substring(0, 2)
        val blocks = digitsOnly.substring(2).chunked(4).joinToString(" ")
        return "$checkDigits $blocks"
    }
    return raw.trim().replace(Regex("\\s+"), " ")
}

/**
 * Matches `MsScheduleType` in `APRSystem.coreTest.MS.Const.MsScheduleType`:
 * - [MY_PLAN]: `MsScheduleType.Student = 5` (`"Student"`)
 * - [MY_GROUP_PLAN]: `MsScheduleType.GroupsForStudent = 100` (`"GroupsForStudent"`)
 */
@Immutable
@Serializable
enum class ScheduleFilterType(
    val apiCode: Int,
    val apiEnumName: String,
    val polishLabel: String
) {
    MY_PLAN(apiCode = 5, apiEnumName = "Student", polishLabel = "Mój plan"),
    MY_GROUP_PLAN(apiCode = 100, apiEnumName = "GroupsForStudent", polishLabel = "Plan grupy")
}

@Immutable
@Serializable
data class Subject(
    val id: String,
    val name: String,
    val shortCode: String,
    val ectsPoints: Int? = null
)

@Immutable
@Serializable
data class Lecturer(
    val id: String,
    val academicTitle: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null
) {
    val fullDisplayName: String
        get() = listOf(academicTitle, firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

    /** Czy dziekanat podał prowadzącego (przy ukrytym lub brakującym polu nazwa jest pusta). */
    val hasName: Boolean
        get() = fullDisplayName.isNotBlank()

    /** Nazwa do wyświetlenia w UI — z czytelnym tekstem zastępczym, gdy brak danych. */
    val displayNameOrPlaceholder: String
        get() = fullDisplayName.ifBlank { "Brak prowadzącego" }
}

@Immutable
@Serializable
data class Room(
    val number: String,
    val building: String = "Kampus Uniwersytetu WSEI • Projektowa 4",
    val floorLabel: String? = null,
    val isRemoteTeams: Boolean = false
) {
    /** Tylko sala (bez adresu): „Sala 205K", „Streaming", „E-learning". */
    val displayLabel: String
        get() = when {
            isRemoteTeams && number.equals("E-learning", ignoreCase = true) -> "E-learning"
            isRemoteTeams -> "Streaming (Zajęcia zdalne)"
            number.isBlank() -> "Sala do ustalenia"
            else -> "Sala $number"
        }

    /** Adres/kampus wyświetlany osobno od sali; null dla zajęć zdalnych i e-learningu. */
    val addressLabel: String?
        get() = if (isRemoteTeams) null else building.trim().takeIf { it.isNotBlank() }
}

@Immutable
@Serializable
data class TimeSlot(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val startFormatted: String, // e.g. "10:30"
    val endFormatted: String    // e.g. "12:55"
) {
    val durationMinutes: Int
        get() = ((endEpochMillis - startEpochMillis) / 60_000L).toInt().coerceAtLeast(1)
}

@Immutable
@Serializable
enum class DataSourceOrigin(val statusLabel: String) {
    APR_WD_API("Wirtualny Dziekanat APR System (API :20830)"),
    HTML_FALLBACK("Wirtualny Dziekanat – strona WWW (tryb awaryjny)"),
    DEBUG_TEST_ACCOUNT("Konto Testowe WSEI (Tryb Debug)"),
    LOCAL_CACHE("Pamięć podręczna Offline (Room)")
}

/**
 * Primary schedule event mapped 1:1 from `MsClassScheduleItemDto` & `MsClassScheduleItemDetailsDto`.
 */
@Immutable
@Serializable
data class ClassEvent(
    val id: String,
    val dateIso: String, // YYYY-MM-DD
    val subject: Subject,
    val classType: ClassType,
    val lecturer: Lecturer,
    val room: Room,
    val timeSlot: TimeSlot,
    val groupCode: String,
    val deliveryMode: DeliveryMode = DeliveryMode.ON_SITE,
    val passingForm: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val incidentAlert: String? = null, // Mapped from `ScheduleIncident`
    val origin: DataSourceOrigin = DataSourceOrigin.APR_WD_API
) {
    val totalDurationMinutes: Int
        get() = timeSlot.durationMinutes

    fun isOngoing(nowEpochMillis: Long): Boolean =
        nowEpochMillis in timeSlot.startEpochMillis until timeSlot.endEpochMillis

    fun isUpcoming(nowEpochMillis: Long): Boolean =
        timeSlot.startEpochMillis > nowEpochMillis

    fun elapsedMinutes(nowEpochMillis: Long): Int {
        if (nowEpochMillis <= timeSlot.startEpochMillis) return 0
        val clampedNow = nowEpochMillis.coerceAtMost(timeSlot.endEpochMillis)
        return ((clampedNow - timeSlot.startEpochMillis) / 60_000L).toInt()
    }

    fun remainingMinutes(nowEpochMillis: Long): Int {
        if (nowEpochMillis >= timeSlot.endEpochMillis) return 0
        val startRef = nowEpochMillis.coerceAtLeast(timeSlot.startEpochMillis)
        return (((timeSlot.endEpochMillis - startRef) / 60_000.0).roundToInt()).coerceAtLeast(0)
    }

    fun progressFraction(nowEpochMillis: Long): Float {
        val totalMillis = (timeSlot.endEpochMillis - timeSlot.startEpochMillis).coerceAtLeast(1L)
        val elapsedMillis = (nowEpochMillis - timeSlot.startEpochMillis).coerceIn(0L, totalMillis)
        return (elapsedMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
    }
}

typealias Lesson = ClassEvent

@Immutable
@Serializable
data class AcademicDay(
    val date: LocalDate,
    val dayOfWeekLabel: String,
    val shortDayLabel: String,
    val dayOfMonthNumber: String,
    val events: List<ClassEvent>,
    val isWeekendExtramuralSession: Boolean = false
) {
    fun findActiveClass(nowEpochMillis: Long): ClassEvent? =
        events.firstOrNull { it.isOngoing(nowEpochMillis) }

    fun findNextClassAfter(currentEvent: ClassEvent): NextClassPreview? {
        val sorted = events.sortedBy { it.timeSlot.startEpochMillis }
        val next = sorted.firstOrNull {
            it.timeSlot.startEpochMillis >= currentEvent.timeSlot.endEpochMillis && it.id != currentEvent.id
        } ?: return null
        val breakMinutes = ((next.timeSlot.startEpochMillis - currentEvent.timeSlot.endEpochMillis) / 60_000L)
            .toInt()
            .coerceAtLeast(0)
        return NextClassPreview(
            nextEvent = next,
            breakDurationMinutes = breakMinutes
        )
    }
}

@Immutable
@Serializable
data class NextClassPreview(
    val nextEvent: ClassEvent,
    val breakDurationMinutes: Int
) {
    val summaryText: String
        get() = buildString {
            if (breakDurationMinutes > 0) {
                append("${breakDurationMinutes} min przerwy → ")
            } else {
                append("Bezpośrednio po zajęciach → ")
            }
            append(nextEvent.subject.name)
            if (nextEvent.room.isRemoteTeams) {
                append(" (Zdalnie)")
            } else {
                append(" w sali ${nextEvent.room.number}")
            }
        }
}

/**
 * Authenticated student session state modeled 1:1 after `MobileAuthResponse`, `MsCourseOfStudyDto`,
 * `MsCourseOfStudyDashboardDto`, and `MsProfileDataDto` from `APRSystem.MobilnyStudent`.
 */
@Immutable
@Serializable
data class StudentSession(
    val userId: Long,
    val personId: Int,
    val studId: Int, // CourseOfStudyId / STUD_ID
    val login: String,
    val firstName: String,
    val lastName: String,
    val albumNumber: String,
    val fieldOfStudy: String,
    val department: String = "Wydział Transportu i Informatyki",
    val courseOfStudyCode: String = "KI/N 2026/27 Z",
    val studyMode: String = "niestacjonarne",
    val obtainedTitle: String = "inż.",
    val specialization: String = "",
    val semesterNumber: Int = 1,
    val academicYear: Int = 1,
    val statusName: String = "Studiuje",
    val averageCurrentSemester: Double = 0.0,
    val averagePreviousSemester: Double = 0.0,
    val averageGrade: Double = 0.0,
    val personPictureBase64: String? = null,
    val allowPasswordChanges: Boolean = true,
    val unreadAnnouncementCount: Int = 0,
    val accessToken: String,
    val isDebugAccount: Boolean
) {
    val fullName: String get() = "$firstName $lastName"

    /**
     * Real specialization, or `null` when missing, identical to [fieldOfStudy], or polluted with a
     * study-mode label ("niestacjonarne", "stacjonarne") — including sessions restored from
     * encrypted storage written by older app versions.
     */
    val cleanSpecialization: String?
        get() = specialization.trim().takeUnless { raw ->
            raw.isEmpty() ||
                raw.equals(fieldOfStudy.trim(), ignoreCase = true) ||
                isStudyModeSpecializationNoise(raw, studyMode)
        }

    fun sanitized(): StudentSession = copy(specialization = cleanSpecialization.orEmpty())
}

internal fun isStudyModeSpecializationNoise(value: String, studyMode: String = ""): Boolean {
    val normalized = value.lowercase().replace(Regex("[^\\p{L}]"), "")
    if (normalized.isEmpty()) return true
    val normalizedMode = studyMode.lowercase().replace(Regex("[^\\p{L}]"), "")
    return normalized == normalizedMode || normalized in KNOWN_STUDY_MODE_WORDS
}

private val KNOWN_STUDY_MODE_WORDS = setOf(
    "stacjonarne", "niestacjonarne", "stacjonarny", "niestacjonarny",
    "studiastacjonarne", "studianiestacjonarne", "zaoczne", "dzienne", "wieczorowe",
    "studiazaoczne", "studiadzienne", "parttime", "fulltime"
)

/**
 * Student Groups (`Grupy` tab in Profile) mapped from `MsGroupsDto` -> `MsCollegeSemesterDto` -> `MsGroupDto`.
 */
@Immutable
@Serializable
data class StudentGroupItem(
    val id: Int,
    val groupCode: String, // e.g. "W/1/I/N", "Cw1/1/I/N", "Lab4/1/I/N"
    val formName: String,  // e.g. "Wykład", "Ćwiczenia", "Laboratorium", "Elearning", "Prakt zaw zajęcia", "Tok"
    /**
     * Group number parsed from [groupCode] ("4" for "Lab4/1/I/N"), "" when the code carries none.
     * Snapshots cached by 0.7.4 and older hold the group *edition* or a fake "1" here — UI should
     * read [displayGroupNumber], which is always recomputed from [groupCode].
     */
    val groupNumber: String,
    val isPlanVisible: Boolean = true
) {
    /** Real group number derived from [groupCode] (also for stale cached snapshots); "" when unknown. */
    val displayGroupNumber: String
        get() = GroupCodeParser.parse(groupCode, formName).number.orEmpty()

    /** Kind of classes this group is for, derived from [groupCode] with [formName] as a fallback. */
    val groupKind: GroupClassKind
        get() = GroupCodeParser.parse(groupCode, formName).kind

    /** Study-track entry ("Tok", e.g. `ROK1/ N`) — part of "Tok nauki", not a class group. */
    val isStudyTrack: Boolean
        get() = groupKind == GroupClassKind.STUDY_TRACK
}

@Immutable
@Serializable
data class StudentSemesterGroups(
    val semesterId: Int,
    val semesterLabel: String, // e.g. "Semestr 2026/27 Z"
    val semesterNumber: Int,
    val groups: List<StudentGroupItem>
)

/**
 * Study Plans (`Plany studiów` tab in Profile) mapped from `MsStudentPlansDto`, `MsStudyPlanDto`, and `MsPlanSubjectDto`.
 */
@Immutable
@Serializable
data class StudyPlanSubjectItem(
    val id: Int,
    val subjectNumber: Int,
    val subjectName: String,
    val form: String,
    val passingForm: String,
    val hours: Int,
    val ects: Float,
    val semesterNumber: Int
)

@Immutable
@Serializable
data class StudyPlanSemester(
    val semesterId: Int,
    val semesterName: String,
    val semesterNumber: Int,
    val subjects: List<StudyPlanSubjectItem>
)

@Immutable
@Serializable
data class StudyPlanOverview(
    val planId: Int,
    val planName: String,
    val semesters: List<StudyPlanSemester>
)

/**
 * Subjects & Grades (`SubjectsData`) mapped from `MsSubjectsSemestersDataDto` & `MsSubjectDetailsDto`.
 */
@Immutable
@Serializable
data class GradeItem(
    val id: String,
    val subjectId: Int = 0,
    val semesterLabel: String,
    val semesterNumber: Int = 1,
    val subjectName: String,
    val moduleName: String = "Moduł ogólny",
    val classType: ClassType,
    val formLabel: String = classType.polishLabel,
    val gradeValue: String,
    val partialGrades: List<String> = emptyList(),
    val passingForm: String,
    val ects: Int,
    val hours: Int = 30,
    val lecturerName: String,
    val dateAwarded: String
)

/**
 * Scholarships (`Stypendia` / `SchoolarshipData`) mapped from `MsSchoolarshipsSemestersDto` & `MsScholarshipDetailsDto`.
 */
@Immutable
@Serializable
data class ScholarshipPayoutItem(
    val id: Int,
    val amountFormatted: String,
    val dateIso: String,
    val status: String
)

/**
 * Semantic class of a scholarship application's `Stan` (status) text.
 *
 * The raw backend text is always kept for display; this classification only drives colours and
 * icons. Matching is keyword based on a normalized (lower-case, diacritic-free) string, so variants
 * such as "Przyjęto", "Przyjęty" or "PRZYJETO" all resolve to [ACCEPTED].
 */
enum class ScholarshipStatusKind {
    /** Application formally accepted for processing ("Przyjęto"). */
    ACCEPTED,

    /** Scholarship granted ("Przyznano", "Rozpatrzony pozytywnie"). */
    GRANTED,

    /** Application rejected or refused ("Odrzucono", "Rozpatrzony negatywnie", "Odmowa"). */
    REJECTED,

    /** Application is being reviewed ("W rozpatrywaniu", "W toku"). */
    UNDER_REVIEW,

    /** Application submitted but not yet processed ("Złożony"). */
    SUBMITTED,

    /** Missing or unrecognized status text. */
    UNKNOWN;

    companion object {
        private val REJECTED_KEYS = listOf("odrzuc", "negatywn", "odmow", "nie przyzn", "nieprzyzn", "niezasadn", "rejected", "denied")
        private val GRANTED_KEYS = listOf("przyzna", "pozytywn", "granted", "awarded")
        private val ACCEPTED_KEYS = listOf("przyjet", "zaakceptow", "accepted")
        private val UNDER_REVIEW_KEYS = listOf("rozpatrywan", "w toku", "weryfik", "oczekuj", "review", "pending")
        private val SUBMITTED_KEYS = listOf("zlozon", "zlozen", "wyslan", "submitted")
        private val COMBINING_MARKS = Regex("\\p{Mn}+")
        private val WHITESPACE = Regex("\\s+")

        /** Classifies a raw status text; `null`/blank yields [UNKNOWN]. */
        fun classify(raw: String?): ScholarshipStatusKind {
            if (raw.isNullOrBlank()) return UNKNOWN
            val text = normalize(raw)
            return when {
                REJECTED_KEYS.any { it in text } -> REJECTED
                GRANTED_KEYS.any { it in text } -> GRANTED
                ACCEPTED_KEYS.any { it in text } -> ACCEPTED
                UNDER_REVIEW_KEYS.any { it in text } -> UNDER_REVIEW
                SUBMITTED_KEYS.any { it in text } -> SUBMITTED
                else -> UNKNOWN
            }
        }

        private fun normalize(raw: String): String {
            val lower = raw.trim().lowercase().replace('ł', 'l')
            return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD)
                .replace(COMBINING_MARKS, "")
                .replace(WHITESPACE, " ")
        }
    }
}

/**
 * A single scholarship application.
 *
 * Optional fields are `null`/blank when the backend does not provide them; the UI must hide them
 * instead of inventing placeholders. All fields added after 0.7.6 have defaults so previously
 * cached JSON still deserializes.
 *
 * @property number application number shown as "№ 70"; `null` (or a legacy `0`) when unknown.
 * @property title application name, e.g. "WNIOSEK O STYPENDIUM SOCJALNE 2026/2027".
 * @property typeName scholarship type (`Typ`); often identical to [title].
 * @property typeVariantName type variant (`Wariant`), e.g. "Studenci".
 * @property submissionDateIso submission date (`Złożony`) as dd.MM.yyyy; blank when unknown.
 * @property statusString raw `Stan` text exactly as returned by the backend; `null` when absent.
 * @property isGranted backend `schoolarshipGranted` flag. It never implies a decision exists.
 * @property decisionNumber ruling number; only set when a real ruling exists.
 * @property decisionDateIso ruling date (dd.MM.yyyy); only set when a real ruling exists.
 * @property decisionStatus ruling status text; only set when a real ruling exists.
 * @property amountFormatted granted/accrued amount; only set when the backend returns a positive amount.
 * @property payouts payments with positive amounts.
 */
@Immutable
@Serializable
data class ScholarshipApplication(
    val id: Int,
    val semesterLabel: String,
    val number: Int? = null,
    val title: String,
    val typeVariantName: String = "",
    val submissionDateIso: String = "",
    val statusString: String? = null,
    val isGranted: Boolean = false,
    val decisionNumber: String? = null,
    val decisionDateIso: String? = null,
    val acceptedRules: Boolean = true,
    val payouts: List<ScholarshipPayoutItem> = emptyList(),
    val typeName: String = "",
    val decisionStatus: String? = null,
    val amountFormatted: String? = null
) {
    /** Application number to display, ignoring the legacy `0` placeholder. */
    val displayNumber: Int? get() = number?.takeIf { it > 0 }

    /** Semantic class of [statusString]. */
    val statusKind: ScholarshipStatusKind get() = ScholarshipStatusKind.classify(statusString)

    /** `true` only when the backend returned an actual ruling (number, date or status). */
    val hasDecision: Boolean
        get() = !decisionNumber.isNullOrBlank() || !decisionDateIso.isNullOrBlank() || !decisionStatus.isNullOrBlank()
}

/**
 * Finances & Online Payments (`FinancesData` & `BlueMedia`) mapped from `MsFinancesDto`, `MsChargeDto`, and `PaymentAnswerDto`.
 */
@Immutable
@Serializable
data class FinancialObligation(
    val id: String,
    val chargeId: Int = 0,
    val title: String,
    val amountPln: Double,
    val amountFormatted: String = String.format("%.2f PLN", amountPln),
    val currentStatePln: Double,
    val dueDateIso: String,
    val paymentDateIso: String? = null,
    val statusLabel: String = if (currentStatePln == 0.0) "Opłacono" else "Do uregulowania",
    val interestFormatted: String = "0.00 PLN",            // Odsetki naliczone
    val accumulatedInterestFormatted: String = "0.00 PLN", // Odsetki zapłacone
    val amortizationFormatted: String = "0.00 PLN",        // Umorzenie
    val discountFormatted: String = "0.00 PLN",            // Bonifikata
    val expiredFormatted: String = "0.00 PLN",             // Przedawnienie
    val isPaid: Boolean,
    val isExpired: Boolean,
    val payOnlineAvailable: Boolean,
    val bankAccountNumber: String
)

/**
 * Announcements & Attachments (`AnnouncementsData`) mapped from `MsAnnouncementDto` & `MsAnnouncementAttachmentDto`.
 */
@Immutable
@Serializable
data class AnnouncementAttachment(
    val id: Int,
    val fileName: String,
    val fileBytesBase64: String? = null,
    /** Id of the announcement the file belongs to (0 = unknown); sent to download endpoint fallbacks. */
    val announcementId: Int = 0,
    /** Size in bytes reported by the list endpoint (0 = unknown). */
    val sizeBytes: Long = 0,
    /** Direct download address from the list entry (`url`/`path`/`link`), absolute or server-relative. */
    val downloadUrl: String? = null,
    /** Alternative file identifier from the list entry (`fileId`/`guid`), used as a download fallback. */
    val alternativeFileId: String? = null,
    /** MIME type reported by the list endpoint, if any. */
    val mimeType: String? = null
)

@Immutable
@Serializable
data class DeanAnnouncement(
    val id: String,
    val announcementId: Int = 0,
    val title: String,
    val bodySnippet: String,
    val fullText: String = bodySnippet,
    val publishedAtIso: String,
    val priorityLevel: Int = 0, // 0 = High, 1 = Medium, 2 = Low
    val isImportant: Boolean,
    val isRead: Boolean,
    val hasAttachment: Boolean,
    val attachments: List<AnnouncementAttachment> = emptyList(),
    val authorDepartment: String = "Dziekanat",
    val senderInitial: String = "D"
)
