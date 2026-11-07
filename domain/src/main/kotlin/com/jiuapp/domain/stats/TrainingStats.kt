package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Totals(val sessions: Int, val minutes: Int) {
    val hours: Double get() = minutes / MINUTES_PER_HOUR

    companion object {
        const val MINUTES_PER_HOUR = 60.0
        val EMPTY = Totals(sessions = 0, minutes = 0)
    }
}

/** Totais de treino agrupados pela faixa/grau registrados em cada treino. */
fun totalsByRank(sessions: List<TrainingSession>): Map<Rank, Totals> =
    sessions.groupBy { it.rankOnDay }.mapValues { (_, list) -> list.toTotals() }

fun totalsByBelt(sessions: List<TrainingSession>): Map<Belt, Totals> =
    sessions.groupBy { it.rankOnDay.belt }.mapValues { (_, list) -> list.toTotals() }

/** Treinos com data posterior à última graduação (a própria data da graduação não conta). */
fun sessionsSince(sessions: List<TrainingSession>, graduation: Graduation?): List<TrainingSession> =
    if (graduation == null) sessions else sessions.filter { it.date.isAfter(graduation.date) }

fun daysSinceGraduation(graduation: Graduation?, today: LocalDate): Long? =
    graduation?.let { ChronoUnit.DAYS.between(it.date, today) }

/** Última graduação cronologicamente registrada, ou null se não houver nenhuma. */
fun latestGraduation(graduations: List<Graduation>): Graduation? =
    graduations.maxByOrNull { it.date }

fun List<TrainingSession>.toTotals(): Totals =
    Totals(sessions = size, minutes = sumOf { it.durationMinutes })
