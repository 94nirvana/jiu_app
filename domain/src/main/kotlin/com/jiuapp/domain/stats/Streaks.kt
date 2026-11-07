package com.jiuapp.domain.stats

import com.jiuapp.domain.InvalidGoalException
import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate

/** Quantidade de treinos por semana (chave = início da semana, segunda-feira). */
internal fun sessionsPerWeek(sessions: List<TrainingSession>): Map<LocalDate, Int> =
    sessions.groupingBy { it.date.weekStart() }.eachCount()

/**
 * Semanas consecutivas, terminando na última semana completa ou na atual, em que a meta foi
 * atingida. A semana atual, se ainda incompleta, não quebra a sequência.
 */
fun currentStreak(sessions: List<TrainingSession>, weeklyGoal: Int, today: LocalDate): Int {
    validateGoal(weeklyGoal)
    val counts = sessionsPerWeek(sessions)
    var week = today.weekStart()
    if ((counts[week] ?: 0) < weeklyGoal) week = week.minusWeeks(1)

    var streak = 0
    while ((counts[week] ?: 0) >= weeklyGoal) {
        streak++
        week = week.minusWeeks(1)
    }
    return streak
}

/** Maior sequência de semanas consecutivas com meta atingida, em todo o histórico. */
fun longestStreak(sessions: List<TrainingSession>, weeklyGoal: Int): Int {
    validateGoal(weeklyGoal)
    val qualifyingWeeks = sessionsPerWeek(sessions)
        .filterValues { it >= weeklyGoal }
        .keys
        .sorted()

    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (week in qualifyingWeeks) {
        run = if (previous != null && week == previous.plusWeeks(1)) run + 1 else 1
        best = maxOf(best, run)
        previous = week
    }
    return best
}

private fun validateGoal(weeklyGoal: Int) {
    if (weeklyGoal < 1) throw InvalidGoalException("Meta semanal deve ser ao menos 1: $weeklyGoal")
}
