package com.jiuapp.domain.load

import com.jiuapp.domain.InvalidGoalException
import com.jiuapp.domain.stats.weekStart
import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate

/** Carga de um treino: duração (min) × intensidade percebida (1..5). */
fun sessionLoad(session: TrainingSession): Int = session.durationMinutes * session.intensity

/** Carga total da semana que contém [anyDayOfWeek]. */
fun weeklyLoad(sessions: List<TrainingSession>, anyDayOfWeek: LocalDate): Int {
    val week = anyDayOfWeek.weekStart()
    return sessions.filter { it.date.weekStart() == week }.sumOf(::sessionLoad)
}

/**
 * Média de carga das [weeks] semanas anteriores à semana de [today].
 * Semanas sem treino entram como zero, pois descanso também faz parte da carga média.
 */
fun averagePreviousWeeklyLoad(
    sessions: List<TrainingSession>,
    today: LocalDate,
    weeks: Int = DEFAULT_COMPARISON_WEEKS,
): Double {
    if (weeks < 1) throw InvalidGoalException("Quantidade de semanas deve ser ao menos 1: $weeks")
    val currentWeek = today.weekStart()
    val loads = (1..weeks).map { back ->
        val week = currentWeek.minusWeeks(back.toLong())
        sessions.filter { it.date.weekStart() == week }.sumOf(::sessionLoad)
    }
    return loads.average()
}

data class OverloadAlert(val currentLoad: Int, val averageLoad: Double, val ratio: Double)

/**
 * Alerta quando a carga da semana atual ultrapassa [threshold] vezes a média das semanas
 * anteriores. Sem histórico de carga (média zero), não há comparação e nenhum alerta é gerado.
 */
fun overloadAlert(
    sessions: List<TrainingSession>,
    today: LocalDate,
    threshold: Double = DEFAULT_OVERLOAD_THRESHOLD,
    weeks: Int = DEFAULT_COMPARISON_WEEKS,
): OverloadAlert? {
    if (threshold <= 1.0) throw InvalidGoalException("Limite de sobrecarga deve ser maior que 1: $threshold")
    val average = averagePreviousWeeklyLoad(sessions, today, weeks)
    if (average == 0.0) return null
    val current = weeklyLoad(sessions, today)
    val ratio = current / average
    return if (ratio >= threshold) OverloadAlert(current, average, ratio) else null
}

/**
 * Alerta quando os últimos [consecutive] treinos registrados têm cansaço ou dor
 * ≥ [highLevel]. Treinos sem essas métricas não contam como alerta.
 */
fun repeatedHighStrainAlert(
    sessions: List<TrainingSession>,
    consecutive: Int = DEFAULT_STRAIN_STREAK,
    highLevel: Int = DEFAULT_HIGH_LEVEL,
): Boolean {
    val recent = sessions.sortedBy { it.date }.takeLast(consecutive)
    if (recent.size < consecutive) return false
    return recent.all { (it.fatigue ?: 0) >= highLevel || (it.pain ?: 0) >= highLevel }
}

const val DEFAULT_COMPARISON_WEEKS = 4
const val DEFAULT_OVERLOAD_THRESHOLD = 1.5
const val DEFAULT_STRAIN_STREAK = 3
const val DEFAULT_HIGH_LEVEL = 4
