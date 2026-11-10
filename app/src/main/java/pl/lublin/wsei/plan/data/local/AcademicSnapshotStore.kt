package pl.lublin.wsei.plan.data.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pl.lublin.wsei.plan.domain.model.DeanAnnouncement
import pl.lublin.wsei.plan.domain.model.FinancialObligation
import pl.lublin.wsei.plan.domain.model.GradeItem
import pl.lublin.wsei.plan.domain.model.ScholarshipApplication
import pl.lublin.wsei.plan.domain.model.StudentSemesterGroups
import pl.lublin.wsei.plan.domain.model.StudyPlanOverview
import javax.inject.Inject
import javax.inject.Singleton

/** Typed keys of the cached academic modules. */
sealed class SnapshotKey<T>(val key: String, val serializer: KSerializer<T>) {
    data object Groups : SnapshotKey<List<StudentSemesterGroups>>("groups", ListSerializer(StudentSemesterGroups.serializer()))
    data object StudyPlans : SnapshotKey<List<StudyPlanOverview>>("study_plans", ListSerializer(StudyPlanOverview.serializer()))
    data object Grades : SnapshotKey<List<GradeItem>>("grades", ListSerializer(GradeItem.serializer()))
    data object Scholarships : SnapshotKey<List<ScholarshipApplication>>("scholarships", ListSerializer(ScholarshipApplication.serializer()))
    data object Finances : SnapshotKey<List<FinancialObligation>>("finances", ListSerializer(FinancialObligation.serializer()))
    data object Announcements : SnapshotKey<List<DeanAnnouncement>>("announcements", ListSerializer(DeanAnnouncement.serializer()))
}

data class CachedValue<T>(val value: T, val updatedAtEpochMillis: Long)

@Singleton
class AcademicSnapshotStore @Inject constructor(
    private val dao: AcademicSnapshotDao,
    private val json: Json
) {
    suspend fun <T> save(ownerStudId: Int, key: SnapshotKey<T>, value: T) {
        dao.upsert(
            AcademicSnapshotEntity(
                ownerStudId = ownerStudId,
                snapshotKey = key.key,
                payloadJson = json.encodeToString(key.serializer, value),
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    /** Returns null when missing or written by an incompatible app version. */
    suspend fun <T> load(ownerStudId: Int, key: SnapshotKey<T>): CachedValue<T>? {
        val row = dao.get(ownerStudId, key.key) ?: return null
        return runCatching { CachedValue(json.decodeFromString(key.serializer, row.payloadJson), row.updatedAtEpochMillis) }
            .getOrNull()
    }

    suspend fun clearAll() = dao.clearAll()
}
