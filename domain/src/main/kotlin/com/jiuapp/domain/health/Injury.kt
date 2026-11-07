package com.jiuapp.domain.health

import com.jiuapp.domain.InvalidHealthRecordException
import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate

enum class BodyRegion(val label: String) {
    HEAD("Cabeça"),
    NECK("Pescoço"),
    SHOULDER_LEFT("Ombro esquerdo"),
    SHOULDER_RIGHT("Ombro direito"),
    ELBOW_LEFT("Cotovelo esquerdo"),
    ELBOW_RIGHT("Cotovelo direito"),
    WRIST_HAND_LEFT("Punho/mão esquerda"),
    WRIST_HAND_RIGHT("Punho/mão direita"),
    RIBS("Costelas"),
    BACK("Coluna"),
    HIP_LEFT("Quadril esquerdo"),
    HIP_RIGHT("Quadril direito"),
    KNEE_LEFT("Joelho esquerdo"),
    KNEE_RIGHT("Joelho direito"),
    ANKLE_FOOT_LEFT("Tornozelo/pé esquerdo"),
    ANKLE_FOOT_RIGHT("Tornozelo/pé direito"),
    OTHER("Outra região"),
}

data class Injury(
    val region: BodyRegion,
    val type: String,
    val severity: Int,
    val startDate: LocalDate,
    val treatment: String? = null,
    val expectedRecoveryDate: LocalDate? = null,
    val actualRecoveryDate: LocalDate? = null,
    val notes: String? = null,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(type.isNotBlank()) { "Tipo da lesão é obrigatório" }
        if (severity !in 1..5) throw InvalidHealthRecordException("Gravidade deve estar entre 1 e 5")
        actualRecoveryDate?.let {
            if (it.isBefore(startDate)) throw InvalidHealthRecordException("Recuperação antes do início da lesão")
        }
    }

    fun isActive(): Boolean = actualRecoveryDate == null
}

/** Regiões com pelo menos [minOccurrences] lesões registradas. */
fun recurringRegions(injuries: List<Injury>, minOccurrences: Int = 2): Set<BodyRegion> =
    injuries.groupingBy { it.region }.eachCount().filterValues { it >= minOccurrences }.keys

fun activeInjuries(injuries: List<Injury>): List<Injury> = injuries.filter { it.isActive() }
