package com.jiuapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco local criptografado (SQLCipher). O esquema é exportado para `schemas/` e toda mudança
 * exige uma Migration explícita: nunca usar fallbackToDestructiveMigration em dados do usuário.
 */
@Database(
    version = 1,
    exportSchema = true,
    entities = [
        ProfileEntity::class,
        GraduationEntity::class,
        SessionEntity::class,
        TagEntity::class,
        SessionTagEntity::class,
        AttachmentEntity::class,
        SessionTechniqueEntity::class,
        TechniqueEntity::class,
        TechniqueStatusHistoryEntity::class,
        GamePlanEntity::class,
        OpponentEntity::class,
        InjuryEntity::class,
        WeightEntity::class,
        CompetitionEntity::class,
        ChecklistItemEntity::class,
        MatchEntity::class,
        AchievementEntity::class,
        ClassEntity::class,
        StudentEntity::class,
        AttendanceEntity::class,
    ],
)
abstract class JiuDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun techniqueDao(): TechniqueDao
    abstract fun healthDao(): HealthDao
    abstract fun competitionDao(): CompetitionDao
    abstract fun rosterDao(): RosterDao
    abstract fun achievementDao(): AchievementDao
    abstract fun planDao(): PlanDao
}
