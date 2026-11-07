package com.jiuapp.domain.health

import com.jiuapp.domain.InvalidHealthRecordException
import com.jiuapp.domain.sync.Syncable
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WeightEntry(
    val date: LocalDate,
    val kg: Double,
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        if (kg !in MIN_KG..MAX_KG) throw InvalidHealthRecordException("Peso fora da faixa plausível: $kg kg")
    }

    companion object {
        const val MIN_KG = 20.0
        const val MAX_KG = 300.0
    }
}

data class WeightAlert(val weeklyLossPercent: Double)

object WeightAnalysis {

    /** Limite conservador para perda rápida de peso em atletas. Premissa a validar com profissional de saúde. */
    const val RAPID_LOSS_PERCENT_PER_WEEK = 1.0

    /** Compara a última medição com a mais próxima de 7 dias antes (janela de 4 a 10 dias). */
    fun weeklyChangePercent(entries: List<WeightEntry>): Double? {
        val sorted = entries.sortedBy { it.date }
        val latest = sorted.lastOrNull() ?: return null
        val reference = sorted
            .filter { ChronoUnit.DAYS.between(it.date, latest.date) in 4L..10L }
            .minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(it.date, latest.date) - 7) }
            ?: return null
        return (latest.kg - reference.kg) / reference.kg * 100.0
    }

    /** Alerta quando a perda semanal supera o limite. Ganho de peso não gera alerta. */
    fun rapidLossAlert(entries: List<WeightEntry>): WeightAlert? {
        val change = weeklyChangePercent(entries) ?: return null
        return if (change < -RAPID_LOSS_PERCENT_PER_WEEK) WeightAlert(change) else null
    }

    fun remainingToTarget(entries: List<WeightEntry>, targetKg: Double): Double? =
        entries.maxByOrNull { it.date }?.let { (it.kg - targetKg).coerceAtLeast(0.0) }
}
