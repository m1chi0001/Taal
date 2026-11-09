package pl.lublin.wsei.plan.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import pl.lublin.wsei.plan.domain.model.ClassEvent
import pl.lublin.wsei.plan.domain.model.ClassType
import pl.lublin.wsei.plan.domain.model.DataSourceOrigin
import pl.lublin.wsei.plan.domain.model.DeliveryMode
import pl.lublin.wsei.plan.domain.model.Lecturer
import pl.lublin.wsei.plan.domain.model.Room
import pl.lublin.wsei.plan.domain.model.Subject
import pl.lublin.wsei.plan.domain.model.TimeSlot

@Entity(tableName = "schedule_events")
data class ClassEventEntity(
    @PrimaryKey val id: String,
    val ownerStudId: Int = 0,
    val dateIso: String,
    val subjectId: String,
    val subjectName: String,
    val subjectShortCode: String,
    val ectsPoints: Int?,
    val classTypeName: String,
    val lecturerId: String,
    val lecturerTitle: String,
    val lecturerFirstName: String,
    val lecturerLastName: String,
    val lecturerEmail: String?,
    val roomNumber: String,
    val roomBuilding: String,
    val isRemoteTeams: Boolean,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val startFormatted: String,
    val endFormatted: String,
    val groupCode: String,
    val deliveryModeName: String = DeliveryMode.ON_SITE.name,
    val notes: String?,
    val incidentAlert: String? = null,
    val originName: String
) {
    fun toDomain(): ClassEvent {
        val isLegacyUnassignedRoom = roomNumber.isBlank() ||
            roomNumber.equals("Kampus WSEI", ignoreCase = true) ||
            roomNumber.equals("Kampus Uniwersytetu WSEI", ignoreCase = true) ||
            roomNumber.contains("Stream", ignoreCase = true) ||
            roomNumber.contains("Zdaln", ignoreCase = true)

        val resolvedRemote = isRemoteTeams || isLegacyUnassignedRoom
        val resolvedDeliveryMode = when {
            deliveryModeName == DeliveryMode.ELEARNING.name -> DeliveryMode.ELEARNING
            resolvedRemote -> DeliveryMode.REMOTE
            else -> runCatching { DeliveryMode.valueOf(deliveryModeName) }.getOrDefault(DeliveryMode.ON_SITE)
        }
        val resolvedRoomNumber = when {
            resolvedDeliveryMode == DeliveryMode.ELEARNING -> "E-learning"
            resolvedRemote && (roomNumber.isBlank() || roomNumber.contains("Kampus", ignoreCase = true)) -> "Streaming"
            else -> roomNumber
        }
        val resolvedBuilding = when {
            resolvedDeliveryMode == DeliveryMode.ELEARNING -> "Platforma e-learningowa Uniwersytetu WSEI"
            resolvedRemote -> "Zajęcia zdalne (Streaming)"
            else -> roomBuilding.replace("Kampus WSEI", "Kampus Uniwersytetu WSEI")
        }

        return ClassEvent(
            id = id,
            dateIso = dateIso,
            subject = Subject(
                id = subjectId,
                name = subjectName,
                shortCode = subjectShortCode,
                ectsPoints = ectsPoints
            ),
            classType = runCatching { ClassType.valueOf(classTypeName) }.getOrDefault(ClassType.LECTURE),
            lecturer = Lecturer(
                id = lecturerId,
                academicTitle = lecturerTitle,
                firstName = lecturerFirstName,
                lastName = lecturerLastName,
                email = lecturerEmail
            ),
            room = Room(
                number = resolvedRoomNumber,
                building = resolvedBuilding,
                isRemoteTeams = resolvedRemote
            ),
            timeSlot = TimeSlot(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                startFormatted = startFormatted,
                endFormatted = endFormatted
            ),
            groupCode = groupCode,
            deliveryMode = resolvedDeliveryMode,
            notes = notes,
            incidentAlert = incidentAlert,
            origin = runCatching { DataSourceOrigin.valueOf(originName) }.getOrDefault(DataSourceOrigin.LOCAL_CACHE)
        )
    }
}

fun ClassEvent.toEntity(ownerStudId: Int = 0): ClassEventEntity = ClassEventEntity(
    id = id,
    ownerStudId = ownerStudId,
    dateIso = dateIso,
    subjectId = subject.id,
    subjectName = subject.name,
    subjectShortCode = subject.shortCode,
    ectsPoints = subject.ectsPoints,
    classTypeName = classType.name,
    lecturerId = lecturer.id,
    lecturerTitle = lecturer.academicTitle,
    lecturerFirstName = lecturer.firstName,
    lecturerLastName = lecturer.lastName,
    lecturerEmail = lecturer.email,
    roomNumber = room.number,
    roomBuilding = room.building,
    isRemoteTeams = room.isRemoteTeams,
    startEpochMillis = timeSlot.startEpochMillis,
    endEpochMillis = timeSlot.endEpochMillis,
    startFormatted = timeSlot.startFormatted,
    endFormatted = timeSlot.endFormatted,
    groupCode = groupCode,
    deliveryModeName = deliveryMode.name,
    notes = notes,
    incidentAlert = incidentAlert,
    originName = origin.name
)

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_events ORDER BY startEpochMillis ASC")
    fun observeAllEvents(): Flow<List<ClassEventEntity>>

    @Query(
        """
        SELECT * FROM schedule_events
        WHERE dateIso BETWEEN :startDateIso AND :endDateIso
        ORDER BY startEpochMillis ASC
        """
    )
    fun observeEventsBetween(startDateIso: String, endDateIso: String): Flow<List<ClassEventEntity>>

    @Query(
        """
        SELECT * FROM schedule_events
        WHERE :nowEpochMillis >= startEpochMillis AND :nowEpochMillis < endEpochMillis
        ORDER BY startEpochMillis ASC
        LIMIT 1
        """
    )
    fun observeActiveEvent(nowEpochMillis: Long): Flow<ClassEventEntity?>

    @Query("SELECT * FROM schedule_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): ClassEventEntity?

    @Query(
        """
        SELECT * FROM schedule_events
        WHERE dateIso = :dateIso
        ORDER BY startEpochMillis ASC
        """
    )
    suspend fun getEventsOnDay(dateIso: String): List<ClassEventEntity>

    @Query(
        """
        SELECT * FROM schedule_events
        WHERE dateIso = :dateIso AND startEpochMillis >= :afterEpochMillis AND id != :excludeId
        ORDER BY startEpochMillis ASC
        LIMIT 1
        """
    )
    suspend fun getNextEventOnDay(
        dateIso: String,
        afterEpochMillis: Long,
        excludeId: String
    ): ClassEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<ClassEventEntity>)

    @Query("DELETE FROM schedule_events WHERE dateIso BETWEEN :startDateIso AND :endDateIso")
    suspend fun deleteRange(startDateIso: String, endDateIso: String)

    @Query("DELETE FROM schedule_events")
    suspend fun clearAllEvents()
}

@Database(
    entities = [ClassEventEntity::class],
    version = 4,
    exportSchema = false
)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
}
