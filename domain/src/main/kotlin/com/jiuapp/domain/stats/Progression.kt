package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.Graduation
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class TimelineEntry(val rank: Rank, val date: LocalDate, val daysSincePrevious: Long?)

object Progression {

    /** Linha do tempo de graduações em ordem cronológica, com o intervalo até a anterior. */
    fun timeline(graduations: List<Graduation>): List<TimelineEntry> {
        val sorted = graduations.filter { it.deletedAt == null }.sortedWith(compareBy({ it.date }, { it.rank }))
        return sorted.mapIndexed { index, graduation ->
            val previous = sorted.getOrNull(index - 1)
            TimelineEntry(
                rank = graduation.rank,
                date = graduation.date,
                daysSincePrevious = previous?.let { ChronoUnit.DAYS.between(it.date, graduation.date) },
            )
        }
    }

    /** Média de dias entre graduações consecutivas. Null se houver menos de duas. */
    fun averageDaysBetweenGraduations(graduations: List<Graduation>): Double? {
        val intervals = timeline(graduations).mapNotNull { it.daysSincePrevious }
        return if (intervals.isEmpty()) null else intervals.average()
    }

    /**
     * Estimativa da próxima graduação com base no intervalo médio histórico.
     * Sem histórico suficiente, retorna null em vez de inventar uma data.
     */
    fun estimateNextGraduation(graduations: List<Graduation>): LocalDate? {
        val average = averageDaysBetweenGraduations(graduations) ?: return null
        val last = graduations.filter { it.deletedAt == null }.maxByOrNull { it.date }?.date ?: return null
        return last.plusDays(average.toLong())
    }
}

enum class GoalStatus { ACHIEVED, ON_TRACK, BEHIND }

object WeeklyGoal {

    /**
     * Compara os treinos da semana com o ritmo esperado no dia (meta proporcional aos dias já passados).
     * Dias de domingo a segunda seguem o mesmo cálculo: a semana começa na segunda.
     */
    fun status(sessionsThisWeek: Int, goal: Int, today: LocalDate): GoalStatus {
        require(goal >= 1) { "Meta semanal deve ser ao menos 1" }
        if (sessionsThisWeek >= goal) return GoalStatus.ACHIEVED
        val elapsedDays = today.dayOfWeek.value
        val expected = goal * elapsedDays / DAYS_PER_WEEK
        return if (sessionsThisWeek < expected) GoalStatus.BEHIND else GoalStatus.ON_TRACK
    }

    private const val DAYS_PER_WEEK = 7
}
