package com.jiuapp.domain.technique

import com.jiuapp.domain.InvalidSessionException
import com.jiuapp.domain.sync.Syncable
import com.jiuapp.domain.training.MediaLink
import java.time.Instant
import java.time.LocalDate

enum class TechniqueCategory { POSITION, SUBMISSION, SWEEP, PASS, TAKEDOWN, DEFENSE, ESCAPE }

/** Estágios de domínio, do mais inicial ao mais avançado. A ordem é significativa. */
enum class TechniqueStatus {
    LEARNING,
    WORKS_IN_TECHNICAL_TRAINING,
    WORKS_IN_SPARRING,
    WORKS_IN_COMPETITION,
}

data class StatusChange(val status: TechniqueStatus, val date: LocalDate)

data class Technique(
    val name: String,
    val category: TechniqueCategory,
    val originPosition: String? = null,
    val description: String? = null,
    val keyNotes: String? = null,
    val videoLinks: List<String> = emptyList(),
    val photoUris: List<String> = emptyList(),
    val status: TechniqueStatus = TechniqueStatus.LEARNING,
    val statusHistory: List<StatusChange> = listOf(StatusChange(TechniqueStatus.LEARNING, LocalDate.of(1970, 1, 1))),
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome da técnica é obrigatório" }
        require(name.length <= MAX_NAME_LENGTH) { "Nome da técnica muito longo" }
        videoLinks.forEach(MediaLink::requireAllowed)
        require(statusHistory.isNotEmpty()) { "Histórico de status não pode ser vazio" }
        require(statusHistory.last().status == status) { "Status atual deve corresponder ao último histórico" }
    }

    /**
     * Registra a mudança de status na data informada. Repetir o status atual não gera entrada.
     * Datas anteriores à última mudança são rejeitadas para manter o histórico cronológico.
     */
    fun changeStatus(newStatus: TechniqueStatus, on: LocalDate): Technique {
        if (newStatus == status) return this
        if (on.isBefore(statusHistory.last().date)) {
            throw InvalidSessionException("Mudança de status anterior à última registrada")
        }
        return copy(
            status = newStatus,
            statusHistory = statusHistory + StatusChange(newStatus, on),
        )
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
    }
}

/** Lista inicial sugerida. O usuário edita ou remove depois, em lote. */
object SuggestedTechniques {
    val all: List<Pair<String, TechniqueCategory>> = listOf(
        "Guarda fechada" to TechniqueCategory.POSITION,
        "Meia guarda" to TechniqueCategory.POSITION,
        "Guarda de laço" to TechniqueCategory.POSITION,
        "Guarda de borboleta" to TechniqueCategory.POSITION,
        "Montada" to TechniqueCategory.POSITION,
        "Controle por trás" to TechniqueCategory.POSITION,
        "Triângulo" to TechniqueCategory.SUBMISSION,
        "Armlock" to TechniqueCategory.SUBMISSION,
        "Mata leão" to TechniqueCategory.SUBMISSION,
        "Chave de calcanhar" to TechniqueCategory.SUBMISSION,
        "Estrangulamento guilhotina" to TechniqueCategory.SUBMISSION,
        "Raspagem de tesoura" to TechniqueCategory.SWEEP,
        "Raspagem de quadril" to TechniqueCategory.SWEEP,
        "Passagem de joelho na barriga" to TechniqueCategory.PASS,
        "Passagem de cem quilos" to TechniqueCategory.PASS,
        "Queda de bicicleta" to TechniqueCategory.TAKEDOWN,
        "Defesa de quadril" to TechniqueCategory.DEFENSE,
        "Fuga de montada" to TechniqueCategory.ESCAPE,
        "Fuga de controle por trás" to TechniqueCategory.ESCAPE,
    )
}
