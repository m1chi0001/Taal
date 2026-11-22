package pl.lublin.wsei.plan.domain.model

/** Builds [StudyTrackInfo] from groups (study-track entries) and the session's course code. */
object StudyTrackParser {
    fun from(semesters: List<StudentSemesterGroups>, courseOfStudyCode: String?, studyModeText: String?): StudyTrackInfo? =
        null
}
