package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.DayOfWeek
import java.time.LocalDate

enum class HeatMetric { COUNT, HOURS }

/** Intensidade de 0 (sem treino) a 4 (máxima) para o calendário de calor. */
object Heatmap {

    private const val MAX_LEVEL = 4
    private const val MINUTES_PER_LEVEL = 60

    fun levels(sessions: List<TrainingSession>, year: Int, metric: HeatMetric): Map<LocalDate, Int> {
        val perDay = sessions
            .filter { it.deletedAt == null && it.date.year == year }
            .groupBy { it.date }
            .mapValues { (_, list) ->
                when (metric) {
                    HeatMetric.COUNT -> list.size
                    HeatMetric.HOURS -> list.sumOf { it.durationMinutes }
                }
            }
        return perDay.mapValues { (_, value) ->
            when (metric) {
                HeatMetric.COUNT -> value.coerceAtMost(MAX_LEVEL)
                HeatMetric.HOURS -> (value / MINUTES_PER_LEVEL + 1).coerceAtMost(MAX_LEVEL)
            }
        }
    }
}

data class Share(val label: String, val count: Int, val fraction: Double)

object Distributions {

    fun byType(sessions: List<TrainingSession>): List<Share> = shares(sessions.map { it.type.name }, sessions.size)

    /** Treinos de sparring/competição versus treinos técnicos e de outros tipos. */
    fun sparringVsTechnical(sessions: List<TrainingSession>): Pair<Share, Share> {
        val sparring = sessions.count { it.type.isSparring }
        val other = sessions.size - sparring
        return share("SPARRING", sparring, sessions.size) to share("TECHNICAL", other, sessions.size)
    }

    /** Gi e no-gi. Treinos sem modalidade definida (drilling, técnica etc.) não entram aqui. */
    fun giVsNoGi(sessions: List<TrainingSession>): Pair<Share, Share> {
        val gi = sessions.count { it.type.isGi == true }
        val noGi = sessions.count { it.type.isGi == false }
        val total = gi + noGi
        return share("GI", gi, total) to share("NO_GI", noGi, total)
    }

    fun byBelt(sessions: List<TrainingSession>): List<Share> =
        shares(sessions.map { it.rankOnDay.belt.name }, sessions.size)

    private fun shares(keys: List<String>, total: Int): List<Share> =
        keys.groupingBy { it }.eachCount().map { (label, count) -> share(label, count, total) }
            .sortedByDescending { it.count }

    private fun share(label: String, count: Int, total: Int): Share =
        Share(label, count, if (total == 0) 0.0 else count.toDouble() / total)
}

object Comparison {

    /** Variação percentual de [current] sobre [previous]. Null quando não há base de comparação. */
    fun percentChange(current: Double, previous: Double): Double? =
        if (previous == 0.0) null else (current - previous) / previous * 100.0
}

object TimeOfDay {

    fun byWeekday(sessions: List<TrainingSession>): Map<DayOfWeek, Int> =
        sessions.groupingBy { it.date.dayOfWeek }.eachCount()

    /** Treinos por hora de início. Treinos sem horário registrado não entram. */
    fun byHour(sessions: List<TrainingSession>): Map<Int, Int> =
        sessions.mapNotNull { it.startTime?.hour }.groupingBy { it }.eachCount()
}

data class TagCount(val name: String, val count: Int)

object TagRanking {

    /** Tags mais citadas. [focus] = true usa "o que preciso corrigir"; false usa "onde treinei". */
    fun rank(sessions: List<TrainingSession>, focus: Boolean): List<TagCount> =
        sessions
            .flatMap { if (focus) it.focusTags else it.tags }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) -> TagCount(name, count) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.name })
}

data class SessionFilter(
    val query: String = "",
    val tag: String? = null,
    val types: Set<TrainingType> = emptySet(),
    val belt: Belt? = null,
    val academy: String? = null,
    val professor: String? = null,
    val range: DateRange? = null,
) {
    fun apply(sessions: List<TrainingSession>): List<TrainingSession> {
        val needle = query.trim().lowercase()
        return sessions.filter { session ->
            session.deletedAt == null &&
                (needle.isEmpty() || textOf(session).contains(needle)) &&
                (tag == null || session.tags.any { it.equals(tag, ignoreCase = true) } ||
                    session.focusTags.any { it.equals(tag, ignoreCase = true) }) &&
                (types.isEmpty() || session.type in types) &&
                (belt == null || session.rankOnDay.belt == belt) &&
                (academy == null || session.academy.equals(academy, ignoreCase = true)) &&
                (professor == null || session.professor.equals(professor, ignoreCase = true)) &&
                (range == null || range.contains(session.date))
        }
    }

    private fun textOf(session: TrainingSession): String =
        listOfNotNull(
            session.learned,
            session.toFix,
            session.notes,
            session.academy,
            session.professor,
            session.tags.joinToString(" "),
            session.focusTags.joinToString(" "),
        ).joinToString(" ").lowercase()
}
