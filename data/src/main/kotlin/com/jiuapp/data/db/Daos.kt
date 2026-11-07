package com.jiuapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE deletedAt IS NULL LIMIT 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile")
    suspend fun all(): List<ProfileEntity>

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM graduations ORDER BY date")
    fun observeGraduations(): Flow<List<GraduationEntity>>

    @Query("SELECT * FROM graduations WHERE id = :id")
    suspend fun graduation(id: String): GraduationEntity?

    @Query("SELECT * FROM graduations")
    suspend fun allGraduations(): List<GraduationEntity>

    @Upsert
    suspend fun upsertGraduation(graduation: GraduationEntity)

}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE deletedAt IS NULL ORDER BY date DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun session(id: String): SessionEntity?

    @Query("SELECT * FROM sessions")
    suspend fun allSessions(): List<SessionEntity>

    @Query("SELECT * FROM session_tags")
    fun observeSessionTags(): Flow<List<SessionTagEntity>>

    @Query("SELECT * FROM session_tags")
    suspend fun allSessionTags(): List<SessionTagEntity>

    @Query("SELECT * FROM attachments")
    fun observeAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments")
    suspend fun allAttachments(): List<AttachmentEntity>

    @Query("SELECT * FROM session_techniques")
    fun observeSessionTechniques(): Flow<List<SessionTechniqueEntity>>

    @Query("SELECT * FROM session_techniques")
    suspend fun allSessionTechniques(): List<SessionTechniqueEntity>

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Upsert
    suspend fun upsertAttachments(attachments: List<AttachmentEntity>)

    @Upsert
    suspend fun upsertSessionTags(links: List<SessionTagEntity>)

    @Upsert
    suspend fun upsertSessionTechniques(links: List<SessionTechniqueEntity>)

    @Query("DELETE FROM session_tags WHERE sessionId = :sessionId")
    suspend fun deleteSessionTags(sessionId: String)

    @Query("DELETE FROM attachments WHERE sessionId = :sessionId")
    suspend fun deleteAttachments(sessionId: String)

    @Query("DELETE FROM session_techniques WHERE sessionId = :sessionId")
    suspend fun deleteSessionTechniques(sessionId: String)

    @Query("SELECT * FROM tags")
    suspend fun allTags(): List<TagEntity>

    @Transaction
    suspend fun saveSession(
        session: SessionEntity,
        tags: List<TagEntity>,
        links: List<SessionTagEntity>,
        attachments: List<AttachmentEntity>,
        techniques: List<SessionTechniqueEntity>,
    ) {
        upsertSession(session)
        upsertTags(tags)
        deleteSessionTags(session.id)
        upsertSessionTags(links)
        deleteAttachments(session.id)
        upsertAttachments(attachments)
        deleteSessionTechniques(session.id)
        upsertSessionTechniques(techniques)
    }
}

@Dao
interface TechniqueDao {
    @Query("SELECT * FROM techniques WHERE deletedAt IS NULL ORDER BY name")
    fun observeTechniques(): Flow<List<TechniqueEntity>>

    @Query("SELECT * FROM techniques WHERE id = :id")
    suspend fun technique(id: String): TechniqueEntity?

    @Query("SELECT * FROM techniques")
    suspend fun allTechniques(): List<TechniqueEntity>

    @Query("SELECT * FROM technique_status_history ORDER BY date")
    fun observeHistory(): Flow<List<TechniqueStatusHistoryEntity>>

    @Query("SELECT * FROM technique_status_history")
    suspend fun allHistory(): List<TechniqueStatusHistoryEntity>

    @Query("DELETE FROM technique_status_history WHERE techniqueId = :techniqueId")
    suspend fun deleteHistory(techniqueId: String)

    @Upsert
    suspend fun upsertTechnique(technique: TechniqueEntity)

    @Upsert
    suspend fun upsertHistory(history: List<TechniqueStatusHistoryEntity>)

    @Transaction
    suspend fun saveTechnique(technique: TechniqueEntity, history: List<TechniqueStatusHistoryEntity>) {
        upsertTechnique(technique)
        deleteHistory(technique.id)
        upsertHistory(history)
    }
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM game_plans WHERE deletedAt IS NULL ORDER BY name")
    fun observePlans(): Flow<List<GamePlanEntity>>

    @Query("SELECT * FROM game_plans")
    suspend fun allPlans(): List<GamePlanEntity>

    @Query("SELECT * FROM opponents WHERE deletedAt IS NULL ORDER BY name")
    fun observeOpponents(): Flow<List<OpponentEntity>>

    @Query("SELECT * FROM opponents")
    suspend fun allOpponents(): List<OpponentEntity>

    @Upsert
    suspend fun upsertPlan(plan: GamePlanEntity)

    @Upsert
    suspend fun upsertOpponent(opponent: OpponentEntity)
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM injuries WHERE deletedAt IS NULL ORDER BY startDate DESC")
    fun observeInjuries(): Flow<List<InjuryEntity>>

    @Query("SELECT * FROM injuries")
    suspend fun allInjuries(): List<InjuryEntity>

    @Upsert
    suspend fun upsertInjury(injury: InjuryEntity)

    @Query("SELECT * FROM weights WHERE deletedAt IS NULL ORDER BY date")
    fun observeWeights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights")
    suspend fun allWeights(): List<WeightEntity>

    @Upsert
    suspend fun upsertWeight(weight: WeightEntity)
}

@Dao
interface CompetitionDao {
    @Query("SELECT * FROM competitions WHERE deletedAt IS NULL ORDER BY date")
    fun observeCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions")
    suspend fun allCompetitions(): List<CompetitionEntity>

    @Query("SELECT * FROM checklist_items")
    fun observeChecklist(): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items")
    suspend fun allChecklist(): List<ChecklistItemEntity>

    @Query("DELETE FROM checklist_items WHERE competitionId = :competitionId")
    suspend fun deleteChecklist(competitionId: String)

    @Upsert
    suspend fun upsertCompetition(competition: CompetitionEntity)

    @Upsert
    suspend fun upsertChecklist(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM matches WHERE deletedAt IS NULL ORDER BY rowid DESC")
    fun observeMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches")
    suspend fun allMatches(): List<MatchEntity>

    @Upsert
    suspend fun upsertMatch(match: MatchEntity)

    @Transaction
    suspend fun saveCompetition(competition: CompetitionEntity, checklist: List<ChecklistItemEntity>) {
        upsertCompetition(competition)
        deleteChecklist(competition.id)
        upsertChecklist(checklist)
    }
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun observe(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun all(): List<AchievementEntity>

    /** IGNORE preserva a data do primeiro desbloqueio. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(achievements: List<AchievementEntity>)
}

@Dao
interface RosterDao {
    @Query("SELECT * FROM classes WHERE deletedAt IS NULL ORDER BY name")
    fun observeClasses(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes")
    suspend fun allClasses(): List<ClassEntity>

    @Upsert
    suspend fun upsertClass(classEntity: ClassEntity)

    @Query("SELECT * FROM students WHERE deletedAt IS NULL ORDER BY name")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun allStudents(): List<StudentEntity>

    @Upsert
    suspend fun upsertStudent(student: StudentEntity)

    @Query("SELECT * FROM attendance WHERE deletedAt IS NULL ORDER BY date DESC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun allAttendance(): List<AttendanceEntity>

    @Upsert
    suspend fun upsertAttendance(records: List<AttendanceEntity>)
}
