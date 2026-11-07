package com.jiuapp.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.jiuapp.data.db.AchievementEntity
import com.jiuapp.data.db.AttachmentEntity
import com.jiuapp.data.db.AttendanceEntity
import com.jiuapp.data.db.ChecklistItemEntity
import com.jiuapp.data.db.ClassEntity
import com.jiuapp.data.db.CompetitionEntity
import com.jiuapp.data.db.GamePlanEntity
import com.jiuapp.data.db.GraduationEntity
import com.jiuapp.data.db.InjuryEntity
import com.jiuapp.data.db.JiuDatabase
import com.jiuapp.data.db.MatchEntity
import com.jiuapp.data.db.OpponentEntity
import com.jiuapp.data.db.ProfileEntity
import com.jiuapp.data.db.SessionEntity
import com.jiuapp.data.db.SessionTagEntity
import com.jiuapp.data.db.SessionTechniqueEntity
import com.jiuapp.data.db.StudentEntity
import com.jiuapp.data.db.TagEntity
import com.jiuapp.data.db.TechniqueEntity
import com.jiuapp.data.db.TechniqueStatusHistoryEntity
import com.jiuapp.data.db.WeightEntity
import com.jiuapp.data.mapper.toDomain
import com.jiuapp.data.security.DatabaseKeyProvider
import com.jiuapp.data.settings.DataStoreSettingsRepository
import com.jiuapp.domain.repository.DataRepository
import com.jiuapp.domain.sync.LastWriteWins
import com.jiuapp.domain.sync.Syncable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Backup JSON versionado. Importação valida cada registro pelos modelos de domínio e resolve conflitos
 * por última modificação. Tudo acontece em uma transação: um backup inválido não deixa dados pela metade.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: JiuDatabase,
    private val keys: DatabaseKeyProvider,
    private val settings: DataStoreSettingsRepository,
    private val clock: Clock,
) : DataRepository {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    override suspend fun exportBackup(): String {
        val payload = BackupPayload(
            exportedAt = clock.instant().toString(),
            profile = db.profileDao().all(),
            graduations = db.profileDao().allGraduations(),
            sessions = db.sessionDao().allSessions(),
            tags = db.sessionDao().allTags(),
            sessionTags = db.sessionDao().allSessionTags(),
            attachments = db.sessionDao().allAttachments(),
            sessionTechniques = db.sessionDao().allSessionTechniques(),
            techniques = db.techniqueDao().allTechniques(),
            techniqueHistory = db.techniqueDao().allHistory(),
            gamePlans = db.planDao().allPlans(),
            opponents = db.planDao().allOpponents(),
            injuries = db.healthDao().allInjuries(),
            weights = db.healthDao().allWeights(),
            competitions = db.competitionDao().allCompetitions(),
            checklistItems = db.competitionDao().allChecklist(),
            matches = db.competitionDao().allMatches(),
            achievements = db.achievementDao().all(),
            classes = db.rosterDao().allClasses(),
            students = db.rosterDao().allStudents(),
            attendance = db.rosterDao().allAttendance(),
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    override suspend fun importBackup(json: String): Int {
        require(json.length <= MAX_BACKUP_CHARS) { "Backup maior que o limite permitido" }
        val payload = this.json.decodeFromString(BackupPayload.serializer(), json)
        require(payload.format == FORMAT && payload.version == VERSION) { "Formato de backup não suportado" }

        return db.withTransaction {
            val profileDao = db.profileDao()
            val sessionDao = db.sessionDao()
            val techniqueDao = db.techniqueDao()
            val competitionDao = db.competitionDao()
            var applied = 0

            // Pais primeiro, depois os filhos que dependem deles.
            val profiles = winners(payload.profile, profileDao.all()) { it.toDomain() }
            profiles.forEach { profileDao.upsert(it); applied++ }

            val graduations = winners(payload.graduations, profileDao.allGraduations()) { it.toDomain() }
            graduations.forEach { profileDao.upsertGraduation(it); applied++ }

            // Tags são catálogo compartilhado: upsert idempotente de todas as tags do backup.
            sessionDao.upsertTags(payload.tags)
            val sessions = winners(payload.sessions, sessionDao.allSessions()) { it.toDomain(EMPTY_RELATIONS) }
            sessions.forEach { session ->
                sessionDao.saveSession(
                    session = session,
                    tags = emptyList(),
                    links = payload.sessionTags.filter { it.sessionId == session.id },
                    attachments = payload.attachments.filter { it.sessionId == session.id },
                    techniques = payload.sessionTechniques.filter { it.sessionId == session.id },
                )
                applied++
            }

            val techniques = winners(payload.techniques, techniqueDao.allTechniques()) { it.toDomain(emptyList()) }
            techniques.forEach { technique ->
                val history = payload.techniqueHistory.filter { it.techniqueId == technique.id }
                techniqueDao.saveTechnique(technique, history)
                applied++
            }

            winners(payload.gamePlans, db.planDao().allPlans()) { it.toDomain() }.forEach { db.planDao().upsertPlan(it); applied++ }
            winners(payload.opponents, db.planDao().allOpponents()) { it.toDomain() }.forEach { db.planDao().upsertOpponent(it); applied++ }

            winners(payload.injuries, db.healthDao().allInjuries()) { it.toDomain() }.forEach { db.healthDao().upsertInjury(it); applied++ }
            winners(payload.weights, db.healthDao().allWeights()) { it.toDomain() }.forEach { db.healthDao().upsertWeight(it); applied++ }

            val competitions = winners(payload.competitions, competitionDao.allCompetitions()) { it.toDomain(emptyList()) }
            competitions.forEach { competition ->
                val checklist = payload.checklistItems.filter { it.competitionId == competition.id }
                competitionDao.saveCompetition(competition, checklist)
                applied++
            }
            winners(payload.matches, competitionDao.allMatches()) { it.toDomain() }.forEach { competitionDao.upsertMatch(it); applied++ }

            db.achievementDao().insertIfAbsent(payload.achievements)
            applied += payload.achievements.size

            val rosterDao = db.rosterDao()
            winners(payload.classes, rosterDao.allClasses()) { it.toDomain() }.forEach { rosterDao.upsertClass(it); applied++ }
            winners(payload.students, rosterDao.allStudents()) { it.toDomain() }.forEach { rosterDao.upsertStudent(it); applied++ }
            winners(payload.attendance, rosterDao.allAttendance()) { it.toDomain() }.let { accepted ->
                rosterDao.upsertAttendance(accepted)
                applied += accepted.size
            }

            applied
        }
    }

    override suspend fun deleteAllData() {
        db.close()
        context.deleteDatabase(DATABASE_NAME)
        settings.update { it.copy(onboardingDone = false) }
        keys.destroy()
    }

    /**
     * Registros do backup que devem substituir os locais: os inexistentes e os mais recentes.
     * Cada registro é validado pelo `toDomain` antes de ser aceito; dados inválidos lançam exceção.
     */
    private fun <E, D : Syncable> winners(incoming: List<E>, local: List<E>, toDomain: (E) -> D): List<E> {
        val localById = local.associateBy { toDomain(it).id }
        return incoming.mapNotNull { entity ->
            val remote = toDomain(entity)
            val existing = localById[remote.id] ?: return@mapNotNull entity
            if (LastWriteWins.resolve(toDomain(existing), remote) === remote) entity else null
        }
    }

    @Serializable
    private data class BackupPayload(
        val format: String = FORMAT,
        val version: Int = VERSION,
        val exportedAt: String,
        val profile: List<ProfileEntity> = emptyList(),
        val graduations: List<GraduationEntity> = emptyList(),
        val sessions: List<SessionEntity> = emptyList(),
        val tags: List<TagEntity> = emptyList(),
        val sessionTags: List<SessionTagEntity> = emptyList(),
        val attachments: List<AttachmentEntity> = emptyList(),
        val sessionTechniques: List<SessionTechniqueEntity> = emptyList(),
        val techniques: List<TechniqueEntity> = emptyList(),
        val techniqueHistory: List<TechniqueStatusHistoryEntity> = emptyList(),
        val gamePlans: List<GamePlanEntity> = emptyList(),
        val opponents: List<OpponentEntity> = emptyList(),
        val injuries: List<InjuryEntity> = emptyList(),
        val weights: List<WeightEntity> = emptyList(),
        val competitions: List<CompetitionEntity> = emptyList(),
        val checklistItems: List<ChecklistItemEntity> = emptyList(),
        val matches: List<MatchEntity> = emptyList(),
        val achievements: List<AchievementEntity> = emptyList(),
        val classes: List<ClassEntity> = emptyList(),
        val students: List<StudentEntity> = emptyList(),
        val attendance: List<AttendanceEntity> = emptyList(),
    )

    private companion object {
        const val FORMAT = "jiuapp-backup"
        const val VERSION = 1
        const val MAX_BACKUP_CHARS = 25_000_000
        const val DATABASE_NAME = "jiuapp.db"
        val EMPTY_RELATIONS = com.jiuapp.data.mapper.SessionRelations(emptySet(), emptySet(), emptyList(), emptySet())
    }
}
