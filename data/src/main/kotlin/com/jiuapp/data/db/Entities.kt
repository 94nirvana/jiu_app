package com.jiuapp.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * Entidades persistidas. Datas e instantes são guardados como ISO-8601 em TEXT: a ordenação
 * lexicográfica coincide com a cronológica, e o formato é estável no backup JSON.
 * Enums são guardados pelo nome. Listas são guardadas como JSON em texto.
 */

@Serializable
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startDate: String,
    val birthDate: String?,
    val academy: String?,
    val professor: String?,
    val photoUri: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "graduations", indices = [Index("date")])
data class GraduationEntity(
    @PrimaryKey val id: String,
    val belt: String,
    val degree: Int,
    val date: String,
    val professor: String?,
    val note: String?,
    val photoUri: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "sessions", indices = [Index("date"), Index("deletedAt")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val durationMinutes: Int,
    val type: String,
    val intensity: Int,
    val belt: String,
    val degree: Int,
    val fatigue: Int?,
    val pain: Int?,
    val mood: Int?,
    val energy: Int?,
    val startTime: String?,
    val academy: String?,
    val professor: String?,
    val learned: String?,
    val toFix: String?,
    val notes: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey val name: String)

@Serializable
@Entity(
    tableName = "session_tags",
    primaryKeys = ["sessionId", "tagName", "focus"],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["name"], ["tagName"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagName")],
)
data class SessionTagEntity(val sessionId: String, val tagName: String, val focus: Boolean)

@Serializable
@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val kind: String,
    val uri: String,
)

@Serializable
@Entity(
    tableName = "session_techniques",
    primaryKeys = ["sessionId", "techniqueId"],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TechniqueEntity::class, ["id"], ["techniqueId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("techniqueId")],
)
data class SessionTechniqueEntity(val sessionId: String, val techniqueId: String)

@Serializable
@Entity(tableName = "techniques")
data class TechniqueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val originPosition: String?,
    val description: String?,
    val keyNotes: String?,
    val videoLinksJson: String,
    val photoUrisJson: String,
    val status: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(
    tableName = "technique_status_history",
    foreignKeys = [ForeignKey(TechniqueEntity::class, ["id"], ["techniqueId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("techniqueId")],
)
data class TechniqueStatusHistoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val techniqueId: String,
    val status: String,
    val date: String,
)

@Serializable
@Entity(tableName = "game_plans")
data class GamePlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nodesJson: String,
    val edgesJson: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "opponents")
data class OpponentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val level: Int,
    val strengths: String?,
    val worked: String?,
    val didNotWork: String?,
    val notes: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "injuries", indices = [Index("region")])
data class InjuryEntity(
    @PrimaryKey val id: String,
    val region: String,
    val type: String,
    val severity: Int,
    val startDate: String,
    val treatment: String?,
    val expectedRecoveryDate: String?,
    val actualRecoveryDate: String?,
    val notes: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "weights", indices = [Index("date")])
data class WeightEntity(
    @PrimaryKey val id: String,
    val date: String,
    val kg: Double,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "competitions", indices = [Index("date")])
data class CompetitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val date: String,
    val location: String?,
    val organizer: String?,
    val beltCategory: String?,
    val ageCategory: String?,
    val weightCategory: String?,
    val targetWeightKg: Double?,
    val weighInDate: String?,
    val finalPlacement: Int?,
    val notes: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(CompetitionEntity::class, ["id"], ["competitionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("competitionId")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    val competitionId: String,
    val label: String,
    val done: Boolean,
)

@Serializable
@Entity(
    tableName = "matches",
    foreignKeys = [ForeignKey(CompetitionEntity::class, ["id"], ["competitionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("competitionId")],
)
data class MatchEntity(
    @PrimaryKey val id: String,
    val competitionId: String,
    val opponentName: String,
    val opponentId: String?,
    val result: String,
    val method: String,
    val submissionName: String?,
    val myPoints: Int,
    val opponentPoints: Int,
    val durationSeconds: Int?,
    val phase: String?,
    val notes: String?,
    val videoLinksJson: String,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(tableName = "achievements")
data class AchievementEntity(@PrimaryKey val id: String, val unlockedOn: String)

@Serializable
@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val name: String,
    val weekday: String?,
    val time: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(
    tableName = "students",
    foreignKeys = [ForeignKey(ClassEntity::class, ["id"], ["classId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("classId")],
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val classId: String,
    val belt: String,
    val degree: Int,
    val birthDate: String?,
    val updatedAt: String,
    val deletedAt: String?,
)

@Serializable
@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(StudentEntity::class, ["id"], ["studentId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ClassEntity::class, ["id"], ["classId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["studentId", "date"], unique = true), Index("classId")],
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val classId: String,
    val date: String,
    val present: Boolean,
    val updatedAt: String,
    val deletedAt: String?,
)
