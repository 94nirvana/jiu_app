package com.jiuapp.data.mapper

import com.jiuapp.data.db.AchievementEntity
import com.jiuapp.data.db.AttachmentEntity
import com.jiuapp.data.db.AttendanceEntity
import com.jiuapp.data.db.ChecklistItemEntity
import com.jiuapp.data.db.ClassEntity
import com.jiuapp.data.db.CompetitionEntity
import com.jiuapp.data.db.GamePlanEntity
import com.jiuapp.data.db.GraduationEntity
import com.jiuapp.data.db.InjuryEntity
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
import com.jiuapp.domain.achievement.AchievementId
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.community.AttendanceRecord
import com.jiuapp.domain.community.ClassGroup
import com.jiuapp.domain.community.Student
import com.jiuapp.domain.competition.ChecklistItem
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.competition.MatchResult
import com.jiuapp.domain.competition.VictoryMethod
import com.jiuapp.domain.health.BodyRegion
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.domain.technique.PlanEdge
import com.jiuapp.domain.technique.PlanNode
import com.jiuapp.domain.technique.PlanNodeKind
import com.jiuapp.domain.technique.StatusChange
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import com.jiuapp.domain.training.Attachment
import com.jiuapp.domain.training.AttachmentKind
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/*
 * Conversões entre entidades (strings/ISO) e modelos de domínio. Os modelos validam ao serem criados,
 * então dados inconsistentes no banco ou em um backup são rejeitados aqui, não silenciosamente aceitos.
 */

private val json = Json { ignoreUnknownKeys = true }

internal fun LocalDate?.iso(): String? = this?.toString()
internal fun String?.toLocalDateOrNull(): LocalDate? = this?.let(LocalDate::parse)
internal fun Instant.iso(): String = toString()
internal fun Instant?.isoOrNull(): String? = this?.toString()
internal fun String?.toInstantOrNull(): Instant? = this?.let(Instant::parse)

private fun <T : Enum<T>> String.toEnum(clazz: Class<T>): T = java.lang.Enum.valueOf(clazz, this)

// Perfil

fun ProfileEntity.toDomain() = AthleteProfile(
    name = name,
    startDate = LocalDate.parse(startDate),
    birthDate = birthDate.toLocalDateOrNull(),
    academy = academy,
    professor = professor,
    photoUri = photoUri,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun AthleteProfile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    startDate = startDate.toString(),
    birthDate = birthDate.iso(),
    academy = academy,
    professor = professor,
    photoUri = photoUri,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Graduações

fun GraduationEntity.toDomain() = Graduation(
    rank = Rank(belt.toEnum(Belt::class.java), degree),
    date = LocalDate.parse(date),
    professor = professor,
    note = note,
    photoUri = photoUri,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun Graduation.toEntity() = GraduationEntity(
    id = id,
    belt = rank.belt.name,
    degree = rank.degree,
    date = date.toString(),
    professor = professor,
    note = note,
    photoUri = photoUri,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Treinos

/** Dados relacionais de um treino, já carregados das tabelas de junção. */
data class SessionRelations(
    val tags: Set<String>,
    val focusTags: Set<String>,
    val attachments: List<Attachment>,
    val techniqueIds: Set<String>,
)

fun SessionEntity.toDomain(relations: SessionRelations) = TrainingSession(
    id = id,
    date = LocalDate.parse(date),
    durationMinutes = durationMinutes,
    type = type.toEnum(TrainingType::class.java),
    intensity = intensity,
    rankOnDay = Rank(belt.toEnum(Belt::class.java), degree),
    fatigue = fatigue,
    pain = pain,
    mood = mood,
    energy = energy,
    startTime = startTime?.let(LocalTime::parse),
    academy = academy,
    professor = professor,
    learned = learned,
    toFix = toFix,
    notes = notes,
    tags = relations.tags,
    focusTags = relations.focusTags,
    techniqueIds = relations.techniqueIds,
    attachments = relations.attachments,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

/** Converte um treino em linhas das tabelas. Tags vazias ou repetidas são normalizadas aqui. */
data class SessionRows(
    val session: SessionEntity,
    val tags: List<TagEntity>,
    val links: List<SessionTagEntity>,
    val attachments: List<AttachmentEntity>,
    val techniques: List<SessionTechniqueEntity>,
)

fun TrainingSession.toRows(): SessionRows {
    val tagNames = (tags.map { it.trim().lowercase() } + focusTags.map { it.trim().lowercase() })
        .filter { it.isNotEmpty() }
        .toSet()
    return SessionRows(
        session = SessionEntity(
            id = id,
            date = date.toString(),
            durationMinutes = durationMinutes,
            type = type.name,
            intensity = intensity,
            belt = rankOnDay.belt.name,
            degree = rankOnDay.degree,
            fatigue = fatigue,
            pain = pain,
            mood = mood,
            energy = energy,
            startTime = startTime?.toString(),
            academy = academy,
            professor = professor,
            learned = learned,
            toFix = toFix,
            notes = notes,
            updatedAt = updatedAt.iso(),
            deletedAt = deletedAt.isoOrNull(),
        ),
        tags = tagNames.map(::TagEntity),
        links = tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
            .map { SessionTagEntity(id, it, focus = false) } +
            focusTags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
                .map { SessionTagEntity(id, it, focus = true) },
        attachments = attachments.map { AttachmentEntity(it.id, id, it.kind.name, it.uri) },
        techniques = techniqueIds.map { SessionTechniqueEntity(id, it) },
    )
}

fun AttachmentEntity.toDomain() = Attachment(id = id, kind = AttachmentKind.valueOf(kind), uri = uri)

// Técnicas

fun TechniqueEntity.toDomain(history: List<TechniqueStatusHistoryEntity>): Technique {
    val changes = history
        .sortedBy { it.date }
        .map { StatusChange(it.status.toEnum(TechniqueStatus::class.java), LocalDate.parse(it.date)) }
        .ifEmpty { listOf(StatusChange(status.toEnum(TechniqueStatus::class.java), LocalDate.of(1970, 1, 1))) }
    return Technique(
        name = name,
        category = category.toEnum(TechniqueCategory::class.java),
        originPosition = originPosition,
        description = description,
        keyNotes = keyNotes,
        videoLinks = json.decodeFromString(ListSerializer(String.serializer()), videoLinksJson),
        photoUris = json.decodeFromString(ListSerializer(String.serializer()), photoUrisJson),
        status = status.toEnum(TechniqueStatus::class.java),
        statusHistory = changes,
        id = id,
        updatedAt = Instant.parse(updatedAt),
        deletedAt = deletedAt.toInstantOrNull(),
    )
}

fun Technique.toEntity() = TechniqueEntity(
    id = id,
    name = name,
    category = category.name,
    originPosition = originPosition,
    description = description,
    keyNotes = keyNotes,
    videoLinksJson = json.encodeToString(ListSerializer(String.serializer()), videoLinks),
    photoUrisJson = json.encodeToString(ListSerializer(String.serializer()), photoUris),
    status = status.name,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

fun Technique.historyRows(): List<TechniqueStatusHistoryEntity> =
    statusHistory.map { TechniqueStatusHistoryEntity(techniqueId = id, status = it.status.name, date = it.date.toString()) }

// Planos de jogo

@Serializable
private data class NodeDto(val id: String, val label: String, val kind: String, val x: Float, val y: Float)

@Serializable
private data class EdgeDto(val fromId: String, val toId: String, val label: String? = null)

fun GamePlanEntity.toDomain(): GamePlan {
    val nodes = json.decodeFromString(ListSerializer(NodeDto.serializer()), nodesJson).map {
        PlanNode(it.id, it.label, it.kind.toEnum(PlanNodeKind::class.java), it.x, it.y)
    }
    val edges = json.decodeFromString(ListSerializer(EdgeDto.serializer()), edgesJson).map {
        PlanEdge(it.fromId, it.toId, it.label)
    }
    return GamePlan(name = name, nodes = nodes, edges = edges, id = id, updatedAt = Instant.parse(updatedAt), deletedAt = deletedAt.toInstantOrNull())
}

fun GamePlan.toEntity() = GamePlanEntity(
    id = id,
    name = name,
    nodesJson = json.encodeToString(
        ListSerializer(NodeDto.serializer()),
        nodes.map { NodeDto(it.id, it.label, it.kind.name, it.x, it.y) },
    ),
    edgesJson = json.encodeToString(
        ListSerializer(EdgeDto.serializer()),
        edges.map { EdgeDto(it.fromId, it.toId, it.label) },
    ),
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Adversários

fun OpponentEntity.toDomain() = Opponent(
    name = name,
    level = level,
    strengths = strengths,
    worked = worked,
    didNotWork = didNotWork,
    notes = notes,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun Opponent.toEntity() = OpponentEntity(
    id = id,
    name = name,
    level = level,
    strengths = strengths,
    worked = worked,
    didNotWork = didNotWork,
    notes = notes,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Saúde

fun InjuryEntity.toDomain() = Injury(
    region = region.toEnum(BodyRegion::class.java),
    type = type,
    severity = severity,
    startDate = LocalDate.parse(startDate),
    treatment = treatment,
    expectedRecoveryDate = expectedRecoveryDate.toLocalDateOrNull(),
    actualRecoveryDate = actualRecoveryDate.toLocalDateOrNull(),
    notes = notes,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun Injury.toEntity() = InjuryEntity(
    id = id,
    region = region.name,
    type = type,
    severity = severity,
    startDate = startDate.toString(),
    treatment = treatment,
    expectedRecoveryDate = expectedRecoveryDate.iso(),
    actualRecoveryDate = actualRecoveryDate.iso(),
    notes = notes,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

fun WeightEntity.toDomain() = WeightEntry(
    date = LocalDate.parse(date),
    kg = kg,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun WeightEntry.toEntity() = WeightEntity(
    id = id,
    date = date.toString(),
    kg = kg,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Competições

fun CompetitionEntity.toDomain(checklist: List<ChecklistItemEntity>) = Competition(
    name = name,
    date = LocalDate.parse(date),
    location = location,
    organizer = organizer,
    beltCategory = beltCategory,
    ageCategory = ageCategory,
    weightCategory = weightCategory,
    targetWeightKg = targetWeightKg,
    weighInDate = weighInDate.toLocalDateOrNull(),
    finalPlacement = finalPlacement,
    notes = notes,
    checklist = checklist.map { ChecklistItem(label = it.label, done = it.done, id = it.id) },
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

data class CompetitionRows(val competition: CompetitionEntity, val checklist: List<ChecklistItemEntity>)

fun Competition.toRows() = CompetitionRows(
    competition = CompetitionEntity(
        id = id,
        name = name,
        date = date.toString(),
        location = location,
        organizer = organizer,
        beltCategory = beltCategory,
        ageCategory = ageCategory,
        weightCategory = weightCategory,
        targetWeightKg = targetWeightKg,
        weighInDate = weighInDate.iso(),
        finalPlacement = finalPlacement,
        notes = notes,
        updatedAt = updatedAt.iso(),
        deletedAt = deletedAt.isoOrNull(),
    ),
    checklist = checklist.map { ChecklistItemEntity(it.id, id, it.label, it.done) },
)

fun MatchEntity.toDomain() = Match(
    competitionId = competitionId,
    opponentName = opponentName,
    result = result.toEnum(MatchResult::class.java),
    method = method.toEnum(VictoryMethod::class.java),
    opponentId = opponentId,
    submissionName = submissionName,
    myPoints = myPoints,
    opponentPoints = opponentPoints,
    durationSeconds = durationSeconds,
    phase = phase,
    notes = notes,
    videoLinks = json.decodeFromString(ListSerializer(String.serializer()), videoLinksJson),
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun Match.toEntity() = MatchEntity(
    id = id,
    competitionId = competitionId,
    opponentName = opponentName,
    opponentId = opponentId,
    result = result.name,
    method = method.name,
    submissionName = submissionName,
    myPoints = myPoints,
    opponentPoints = opponentPoints,
    durationSeconds = durationSeconds,
    phase = phase,
    notes = notes,
    videoLinksJson = json.encodeToString(ListSerializer(String.serializer()), videoLinks),
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

// Conquistas

fun AchievementEntity.toDomain(): Pair<AchievementId, LocalDate> =
    AchievementId.valueOf(id) to LocalDate.parse(unlockedOn)

// Turmas e alunos

fun ClassEntity.toDomain() = ClassGroup(
    name = name,
    weekday = weekday?.toEnum(DayOfWeek::class.java),
    time = time?.let(LocalTime::parse),
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun ClassGroup.toEntity() = ClassEntity(
    id = id,
    name = name,
    weekday = weekday?.name,
    time = time?.toString(),
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

fun StudentEntity.toDomain() = Student(
    name = name,
    classId = classId,
    rank = Rank(belt.toEnum(Belt::class.java), degree),
    birthDate = birthDate.toLocalDateOrNull(),
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun Student.toEntity() = StudentEntity(
    id = id,
    name = name,
    classId = classId,
    belt = rank.belt.name,
    degree = rank.degree,
    birthDate = birthDate.iso(),
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)

fun AttendanceEntity.toDomain() = AttendanceRecord(
    studentId = studentId,
    classId = classId,
    date = LocalDate.parse(date),
    present = present,
    id = id,
    updatedAt = Instant.parse(updatedAt),
    deletedAt = deletedAt.toInstantOrNull(),
)

fun AttendanceRecord.toEntity() = AttendanceEntity(
    id = id,
    studentId = studentId,
    classId = classId,
    date = date.toString(),
    present = present,
    updatedAt = updatedAt.iso(),
    deletedAt = deletedAt.isoOrNull(),
)
