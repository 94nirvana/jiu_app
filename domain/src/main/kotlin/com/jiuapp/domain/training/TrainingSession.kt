package com.jiuapp.domain.training

import com.jiuapp.domain.InvalidSessionException
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class TrainingType(val isSparring: Boolean, val isGi: Boolean?) {
    GI(isSparring = false, isGi = true),
    NO_GI(isSparring = false, isGi = false),
    DRILLING(isSparring = false, isGi = null),
    SPARRING(isSparring = true, isGi = null),
    TECHNIQUE(isSparring = false, isGi = null),
    CONDITIONING(isSparring = false, isGi = null),
    COMPETITION(isSparring = true, isGi = null),
    PRIVATE_LESSON(isSparring = false, isGi = null),
    SEMINAR(isSparring = false, isGi = null),
}

/**
 * Um treino registrado. [rankOnDay] é gravado no momento do registro para que as
 * estatísticas por faixa não mudem após novas graduações.
 *
 * [tags] são os lugares onde o atleta treinou; [focusTags] são os pontos a corrigir.
 */
data class TrainingSession(
    override val id: String,
    val date: LocalDate,
    val durationMinutes: Int,
    val type: TrainingType,
    val intensity: Int,
    val rankOnDay: Rank,
    val fatigue: Int? = null,
    val pain: Int? = null,
    val mood: Int? = null,
    val energy: Int? = null,
    val startTime: LocalTime? = null,
    val academy: String? = null,
    val professor: String? = null,
    val learned: String? = null,
    val toFix: String? = null,
    val notes: String? = null,
    val tags: Set<String> = emptySet(),
    val focusTags: Set<String> = emptySet(),
    val techniqueIds: Set<String> = emptySet(),
    val attachments: List<Attachment> = emptyList(),
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(id.isNotBlank()) { "id vazio" }
        if (durationMinutes !in MIN_DURATION..MAX_DURATION) {
            throw InvalidSessionException("Duração inválida: $durationMinutes min")
        }
        checkScale("intensidade", intensity)
        fatigue?.let { checkScale("cansaço", it) }
        pain?.let { checkScale("dor", it) }
        mood?.let { checkScale("humor", it) }
        energy?.let { checkScale("energia", it) }
    }

    companion object {
        const val MIN_DURATION = 1
        const val MAX_DURATION = 600
        const val SCALE_MIN = 1
        const val SCALE_MAX = 5

        private fun checkScale(name: String, value: Int) {
            if (value !in SCALE_MIN..SCALE_MAX) {
                throw InvalidSessionException("Escala de $name fora de $SCALE_MIN..$SCALE_MAX: $value")
            }
        }
    }
}
