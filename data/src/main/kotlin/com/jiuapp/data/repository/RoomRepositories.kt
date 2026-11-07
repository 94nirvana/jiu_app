package com.jiuapp.data.repository

import com.jiuapp.data.db.AchievementEntity
import com.jiuapp.data.db.JiuDatabase
import com.jiuapp.data.mapper.SessionRelations
import com.jiuapp.data.mapper.SessionRows
import com.jiuapp.data.mapper.iso
import com.jiuapp.data.mapper.historyRows
import com.jiuapp.data.mapper.toDomain
import com.jiuapp.data.mapper.toEntity
import com.jiuapp.data.mapper.toRows
import com.jiuapp.domain.achievement.AchievementId
import com.jiuapp.domain.community.AttendanceRecord
import com.jiuapp.domain.community.ClassGroup
import com.jiuapp.domain.community.Student
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.repository.AchievementRepository
import com.jiuapp.domain.repository.CompetitionRepository
import com.jiuapp.domain.repository.GamePlanRepository
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.domain.repository.OpponentRepository
import com.jiuapp.domain.repository.ProfileRepository
import com.jiuapp.domain.repository.RosterRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.TechniqueRepository
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/*
 * Implementações Room dos contratos do domínio. Exclusão é lógica: o registro permanece com
 * `deletedAt` preenchido para que a sincronização e o backup propaguem a remoção.
 */

@Singleton
class RoomProfileRepository @Inject constructor(private val db: JiuDatabase) : ProfileRepository {
    private val dao = db.profileDao()

    override fun observeProfile(): Flow<AthleteProfile?> = dao.observe().map { it?.toDomain() }

    override suspend fun saveProfile(profile: AthleteProfile) = dao.upsert(profile.toEntity())
}

@Singleton
class RoomGraduationRepository @Inject constructor(private val db: JiuDatabase) : GraduationRepository {
    private val dao = db.profileDao()

    override fun observeAll(): Flow<List<Graduation>> =
        dao.observeGraduations().map { list -> list.filter { it.deletedAt == null }.map { it.toDomain() } }

    override suspend fun save(graduation: Graduation) = dao.upsertGraduation(graduation.toEntity())

    override suspend fun delete(id: String, at: Instant) {
        val existing = dao.graduation(id)?.toDomain() ?: return
        dao.upsertGraduation(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }
}

@Singleton
class RoomSessionRepository @Inject constructor(private val db: JiuDatabase) : SessionRepository {
    private val dao = db.sessionDao()

    override fun observeAll(): Flow<List<TrainingSession>> = combine(
        dao.observeSessions(),
        dao.observeSessionTags(),
        dao.observeAttachments(),
        dao.observeSessionTechniques(),
    ) { sessions, tags, attachments, techniques ->
        val tagsBySession = tags.groupBy { it.sessionId }
        val attachmentsBySession = attachments.groupBy { it.sessionId }
        val techniquesBySession = techniques.groupBy { it.sessionId }
        sessions.map { session ->
            val links = tagsBySession[session.id].orEmpty()
            session.toDomain(
                SessionRelations(
                    tags = links.filterNot { it.focus }.map { it.tagName }.toSet(),
                    focusTags = links.filter { it.focus }.map { it.tagName }.toSet(),
                    attachments = attachmentsBySession[session.id].orEmpty().map { it.toDomain() },
                    techniqueIds = techniquesBySession[session.id].orEmpty().map { it.techniqueId }.toSet(),
                ),
            )
        }
    }

    override suspend fun get(id: String): TrainingSession? {
        val session = dao.session(id) ?: return null
        val links = dao.allSessionTags().filter { it.sessionId == id }
        return session.toDomain(
            SessionRelations(
                tags = links.filterNot { it.focus }.map { it.tagName }.toSet(),
                focusTags = links.filter { it.focus }.map { it.tagName }.toSet(),
                attachments = dao.allAttachments().filter { it.sessionId == id }.map { it.toDomain() },
                techniqueIds = dao.allSessionTechniques().filter { it.sessionId == id }.map { it.techniqueId }.toSet(),
            ),
        )
    }

    override suspend fun save(session: TrainingSession) {
        val rows: SessionRows = session.toRows()
        dao.saveSession(rows.session, rows.tags, rows.links, rows.attachments, rows.techniques)
    }

    override suspend fun delete(id: String, at: Instant) {
        val existing = dao.session(id) ?: return
        dao.upsertSession(existing.copy(deletedAt = at.iso(), updatedAt = at.iso()))
    }
}

@Singleton
class RoomTechniqueRepository @Inject constructor(private val db: JiuDatabase) : TechniqueRepository {
    private val dao = db.techniqueDao()

    override fun observeAll(): Flow<List<Technique>> = combine(dao.observeTechniques(), dao.observeHistory()) { techniques, history ->
        val byTechnique = history.groupBy { it.techniqueId }
        techniques.map { it.toDomain(byTechnique[it.id].orEmpty()) }
    }

    override suspend fun get(id: String): Technique? {
        val technique = dao.technique(id) ?: return null
        return technique.toDomain(dao.allHistory().filter { it.techniqueId == id })
    }

    override suspend fun save(technique: Technique) = dao.saveTechnique(technique.toEntity(), technique.historyRows())

    override suspend fun delete(id: String, at: Instant) {
        val existing = dao.technique(id)?.toDomain(dao.allHistory().filter { it.techniqueId == id }) ?: return
        save(existing.copy(deletedAt = at, updatedAt = at))
    }
}

@Singleton
class RoomGamePlanRepository @Inject constructor(private val db: JiuDatabase) : GamePlanRepository {
    private val dao = db.planDao()

    override fun observeAll(): Flow<List<GamePlan>> =
        dao.observePlans().map { list -> list.filter { it.deletedAt == null }.map { it.toDomain() } }

    override suspend fun save(plan: GamePlan) = dao.upsertPlan(plan.toEntity())

    override suspend fun delete(id: String, at: Instant) {
        val existing = dao.allPlans().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertPlan(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }
}

@Singleton
class RoomOpponentRepository @Inject constructor(private val db: JiuDatabase) : OpponentRepository {
    private val dao = db.planDao()

    override fun observeAll(): Flow<List<Opponent>> =
        dao.observeOpponents().map { list -> list.filter { it.deletedAt == null }.map { it.toDomain() } }

    override suspend fun save(opponent: Opponent) = dao.upsertOpponent(opponent.toEntity())

    override suspend fun delete(id: String, at: Instant) {
        val existing = dao.allOpponents().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertOpponent(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }
}

@Singleton
class RoomHealthRepository @Inject constructor(private val db: JiuDatabase) : HealthRepository {
    private val dao = db.healthDao()

    override fun observeInjuries(): Flow<List<Injury>> =
        dao.observeInjuries().map { list -> list.map { it.toDomain() } }

    override suspend fun saveInjury(injury: Injury) = dao.upsertInjury(injury.toEntity())

    override suspend fun deleteInjury(id: String, at: Instant) {
        val existing = dao.allInjuries().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertInjury(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }

    override fun observeWeights(): Flow<List<WeightEntry>> =
        dao.observeWeights().map { list -> list.map { it.toDomain() } }

    override suspend fun saveWeight(entry: WeightEntry) = dao.upsertWeight(entry.toEntity())

    override suspend fun deleteWeight(id: String, at: Instant) {
        val existing = dao.allWeights().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertWeight(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }
}

@Singleton
class RoomCompetitionRepository @Inject constructor(private val db: JiuDatabase) : CompetitionRepository {
    private val dao = db.competitionDao()

    override fun observeCompetitions(): Flow<List<Competition>> = combine(
        dao.observeCompetitions(),
        dao.observeChecklist(),
    ) { competitions, checklist ->
        val byCompetition = checklist.groupBy { it.competitionId }
        competitions.map { it.toDomain(byCompetition[it.id].orEmpty()) }
    }

    override suspend fun saveCompetition(competition: Competition) {
        val rows = competition.toRows()
        dao.saveCompetition(rows.competition, rows.checklist)
    }

    override suspend fun deleteCompetition(id: String, at: Instant) {
        val existing = dao.allCompetitions().firstOrNull { it.id == id } ?: return
        dao.upsertCompetition(existing.copy(deletedAt = at.iso(), updatedAt = at.iso()))
    }

    override fun observeMatches(): Flow<List<Match>> =
        dao.observeMatches().map { list -> list.map { it.toDomain() } }

    override suspend fun saveMatch(match: Match) = dao.upsertMatch(match.toEntity())

    override suspend fun deleteMatch(id: String, at: Instant) {
        val existing = dao.allMatches().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertMatch(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }
}

@Singleton
class RoomAchievementRepository @Inject constructor(private val db: JiuDatabase) : AchievementRepository {
    private val dao = db.achievementDao()

    override fun observeUnlocked(): Flow<Map<AchievementId, LocalDate>> =
        dao.observe().map { list -> list.associate { it.toDomain() } }

    override suspend fun recordUnlocks(ids: Set<AchievementId>, on: LocalDate) {
        if (ids.isEmpty()) return
        dao.insertIfAbsent(ids.map { AchievementEntity(it.name, on.toString()) })
    }
}

@Singleton
class RoomRosterRepository @Inject constructor(private val db: JiuDatabase) : RosterRepository {
    private val dao = db.rosterDao()

    override fun observeClasses(): Flow<List<ClassGroup>> =
        dao.observeClasses().map { list -> list.map { it.toDomain() } }

    override suspend fun saveClass(classGroup: ClassGroup) = dao.upsertClass(classGroup.toEntity())

    override suspend fun deleteClass(id: String, at: Instant) {
        val existing = dao.allClasses().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertClass(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }

    override fun observeStudents(): Flow<List<Student>> =
        dao.observeStudents().map { list -> list.map { it.toDomain() } }

    override suspend fun saveStudent(student: Student) = dao.upsertStudent(student.toEntity())

    override suspend fun deleteStudent(id: String, at: Instant) {
        val existing = dao.allStudents().firstOrNull { it.id == id }?.toDomain() ?: return
        dao.upsertStudent(existing.copy(deletedAt = at, updatedAt = at).toEntity())
    }

    override fun observeAttendance(): Flow<List<AttendanceRecord>> =
        dao.observeAttendance().map { list -> list.map { it.toDomain() } }

    override suspend fun saveAttendance(records: List<AttendanceRecord>) =
        dao.upsertAttendance(records.map { it.toEntity() })
}
