package com.jiuapp.domain.stats

import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate

enum class Period { WEEK, MONTH, YEAR }

data class DateRange(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start)) { "Fim anterior ao início" }
    }

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

data class Bucket(val start: LocalDate, val end: LocalDate, val sessions: Int, val minutes: Int) {
    val hours: Double get() = minutes / 60.0
}

object Periods {

    fun rangeOf(period: Period, anchor: LocalDate): DateRange = when (period) {
        Period.WEEK -> anchor.weekStart().let { DateRange(it, it.plusDays(6)) }
        Period.MONTH -> anchor.withDayOfMonth(1).let { DateRange(it, it.withDayOfMonth(it.lengthOfMonth())) }
        Period.YEAR -> LocalDate.of(anchor.year, 1, 1).let { DateRange(it, it.withDayOfYear(it.lengthOfYear())) }
    }

    /** Período anterior (sinal negativo) ou seguinte (positivo), do mesmo tipo. */
    fun shift(period: Period, range: DateRange, steps: Long): DateRange {
        val anchor = when (period) {
            Period.WEEK -> range.start.plusWeeks(steps)
            Period.MONTH -> range.start.plusMonths(steps)
            Period.YEAR -> range.start.plusYears(steps)
        }
        return rangeOf(period, anchor)
    }

    /**
     * Agrupa treinos em intervalos do período: dias para semana, semanas para mês, meses para ano.
     * Treinos fora do intervalo são ignorados.
     */
    fun buckets(sessions: List<TrainingSession>, range: DateRange, period: Period): List<Bucket> {
        val live = sessions.filter { it.deletedAt == null && range.contains(it.date) }
        val grouped = live.groupBy { keyOf(it.date, period) }
        val result = mutableListOf<Bucket>()
        var key = keyOf(range.start, period)
        while (!key.isAfter(range.end)) {
            val bucketEnd = minOf(endOf(key, period), range.end)
            val inBucket = grouped[key].orEmpty()
            result += Bucket(
                start = maxOf(key, range.start),
                end = bucketEnd,
                sessions = inBucket.size,
                minutes = inBucket.sumOf { it.durationMinutes },
            )
            key = endOf(key, period).plusDays(1)
        }
        return result
    }

    private fun keyOf(date: LocalDate, period: Period): LocalDate = when (period) {
        Period.WEEK -> date
        Period.MONTH -> date.weekStart()
        Period.YEAR -> date.withDayOfMonth(1)
    }

    private fun endOf(key: LocalDate, period: Period): LocalDate = when (period) {
        Period.WEEK -> key
        Period.MONTH -> key.plusDays(6)
        Period.YEAR -> key.withDayOfMonth(key.lengthOfMonth())
    }
}

/** Maior sequência de meses consecutivos com pelo menos um treino. */
fun longestMonthlyStreak(sessions: List<TrainingSession>): Int {
    val months = sessions
        .filter { it.deletedAt == null }
        .map { it.date.withDayOfMonth(1) }
        .distinct()
        .sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (month in months) {
        run = if (previous != null && month == previous.plusMonths(1)) run + 1 else 1
        best = maxOf(best, run)
        previous = month
    }
    return best
}
