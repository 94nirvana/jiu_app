package com.jiuapp.domain.repository

import com.jiuapp.domain.achievement.AchievementId
import com.jiuapp.domain.community.AttendanceRecord
import com.jiuapp.domain.community.ClassGroup
import com.jiuapp.domain.community.Student
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.settings.AppSettings
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/*
 * Contratos entre domínio e dados. Todas as operações de escrita são upserts e a exclusão é lógica:
 * `delete` marca `deletedAt`/`updatedAt` com o instante informado, para que a sincronização propague a remoção.
 * Leituras emitem apenas registros não excluídos.
 */

interface ProfileRepository {
    fun observeProfile(): Flow<AthleteProfile?>
    suspend fun saveProfile(profile: AthleteProfile)
}

interface GraduationRepository {
    fun observeAll(): Flow<List<Graduation>>
    suspend fun save(graduation: Graduation)
    suspend fun delete(id: String, at: Instant)
}

interface SessionRepository {
    fun observeAll(): Flow<List<TrainingSession>>
    suspend fun get(id: String): TrainingSession?
    suspend fun save(session: TrainingSession)
    suspend fun delete(id: String, at: Instant)
}

interface TechniqueRepository {
    fun observeAll(): Flow<List<Technique>>
    suspend fun get(id: String): Technique?
    suspend fun save(technique: Technique)
    suspend fun delete(id: String, at: Instant)
}

interface GamePlanRepository {
    fun observeAll(): Flow<List<GamePlan>>
    suspend fun save(plan: GamePlan)
    suspend fun delete(id: String, at: Instant)
}

interface OpponentRepository {
    fun observeAll(): Flow<List<Opponent>>
    suspend fun save(opponent: Opponent)
    suspend fun delete(id: String, at: Instant)
}

interface HealthRepository {
    fun observeInjuries(): Flow<List<Injury>>
    suspend fun saveInjury(injury: Injury)
    suspend fun deleteInjury(id: String, at: Instant)
    fun observeWeights(): Flow<List<WeightEntry>>
    suspend fun saveWeight(entry: WeightEntry)
    suspend fun deleteWeight(id: String, at: Instant)
}

interface CompetitionRepository {
    fun observeCompetitions(): Flow<List<Competition>>
    suspend fun saveCompetition(competition: Competition)
    suspend fun deleteCompetition(id: String, at: Instant)
    fun observeMatches(): Flow<List<Match>>
    suspend fun saveMatch(match: Match)
    suspend fun deleteMatch(id: String, at: Instant)
}

interface AchievementRepository {
    /** Conquistas já desbloqueadas, com a data do primeiro desbloqueio. */
    fun observeUnlocked(): Flow<Map<AchievementId, LocalDate>>

    /** Registra apenas as conquistas ainda não desbloqueadas; não altera datas existentes. */
    suspend fun recordUnlocks(ids: Set<AchievementId>, on: LocalDate)
}

interface RosterRepository {
    fun observeClasses(): Flow<List<ClassGroup>>
    suspend fun saveClass(classGroup: ClassGroup)
    suspend fun deleteClass(id: String, at: Instant)
    fun observeStudents(): Flow<List<Student>>
    suspend fun saveStudent(student: Student)
    suspend fun deleteStudent(id: String, at: Instant)
    fun observeAttendance(): Flow<List<AttendanceRecord>>
    suspend fun saveAttendance(records: List<AttendanceRecord>)
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Backup, restauração e exclusão total de dados (LGPD). */
interface DataRepository {
    /** Exporta todos os registros como JSON versionado. */
    suspend fun exportBackup(): String

    /** Importa um backup, resolvendo conflitos por última modificação. Retorna a quantidade de registros aplicados. */
    suspend fun importBackup(json: String): Int

    /** Apaga todos os dados locais e chaves associadas. Não é reversível. */
    suspend fun deleteAllData()
}
