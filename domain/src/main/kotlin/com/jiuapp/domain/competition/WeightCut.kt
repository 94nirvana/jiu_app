package com.jiuapp.domain.competition

import com.jiuapp.domain.InvalidCompetitionException
import kotlin.math.ceil

/** Planejamento para a pesagem. Premissas e limites são estimativas, não orientação médica. */
data class WeightCutPlan(
    val requiredLossKg: Double,
    val lossPerWeekPercent: Double,
    val weeksUntilWeighIn: Double,
    val isRapid: Boolean,
) {
    companion object {
        /** Acima deste percentual por semana, o app alerta sobre perda rápida demais. */
        const val RAPID_PERCENT_PER_WEEK = 1.0
    }
}

object WeightCut {

    fun plan(currentKg: Double, targetKg: Double, daysUntilWeighIn: Long): WeightCutPlan? {
        if (daysUntilWeighIn < 1) throw InvalidCompetitionException("Pesagem deve ser no futuro")
        val loss = currentKg - targetKg
        if (loss <= 0.0) return null
        val weeks = daysUntilWeighIn / 7.0
        val perWeekPercent = loss / currentKg * 100.0 / weeks
        return WeightCutPlan(
            requiredLossKg = roundToOneDecimal(loss),
            lossPerWeekPercent = roundToOneDecimal(perWeekPercent),
            weeksUntilWeighIn = roundToOneDecimal(weeks),
            isRapid = perWeekPercent > WeightCutPlan.RAPID_PERCENT_PER_WEEK,
        )
    }

    /** Dias mínimos para perder [lossKg] sem ultrapassar o limite semanal de perda rápida. */
    fun minimumDaysFor(currentKg: Double, lossKg: Double): Long {
        val weeks = lossKg / currentKg * 100.0 / WeightCutPlan.RAPID_PERCENT_PER_WEEK
        return ceil(weeks * 7.0).toLong()
    }

    private fun roundToOneDecimal(value: Double): Double = kotlin.math.round(value * 10.0) / 10.0
}
