package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodsAnalyticsTest {

    private val rank = Rank(Belt.BLUE, 1)

    private fun session(
        id: String,
        date: LocalDate,
        minutes: Int = 60,
        type: TrainingType = TrainingType.GI,
        tags: Set<String> = emptySet(),
        focus: Set<String> = emptySet(),
        start: LocalTime? = null,
    ) = TrainingSession(
        id = id,
        date = date,
        durationMinutes = minutes,
        type = type,
        intensity = 3,
        rankOnDay = rank,
        tags = tags,
        focusTags = focus,
        startTime = start,
    )

    @Test
    fun `intervalo mensal cobre o mes inteiro e o deslocamento volta um mes`() {
        val range = Periods.rangeOf(Period.MONTH, LocalDate.of(2026, 2, 14))

        assertEquals(LocalDate.of(2026, 2, 1), range.start)
        assertEquals(LocalDate.of(2026, 2, 28), range.end)
        assertEquals(LocalDate.of(2026, 1, 1), Periods.shift(Period.MONTH, range, -1).start)
    }

    @Test
    fun `buckets anuais somam treinos por mes`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 1, 5)),
            session("2", LocalDate.of(2026, 1, 20), minutes = 30),
            session("3", LocalDate.of(2026, 3, 1)),
        )

        val buckets = Periods.buckets(sessions, Periods.rangeOf(Period.YEAR, LocalDate.of(2026, 5, 1)), Period.YEAR)

        assertEquals(12, buckets.size)
        assertEquals(2, buckets[0].sessions)
        assertEquals(90, buckets[0].minutes)
        assertEquals(1, buckets[2].sessions)
    }

    @Test
    fun `buckets semanais tem sete dias`() {
        val buckets = Periods.buckets(emptyList(), Periods.rangeOf(Period.WEEK, LocalDate.of(2026, 3, 4)), Period.WEEK)

        assertEquals(7, buckets.size)
    }

    @Test
    fun `sequencia de meses consecutivos com treino`() {
        val sessions = listOf(
            session("1", LocalDate.of(2025, 11, 2)),
            session("2", LocalDate.of(2025, 12, 2)),
            session("3", LocalDate.of(2026, 1, 2)),
            session("4", LocalDate.of(2026, 3, 2)),
        )

        assertEquals(3, longestMonthlyStreak(sessions))
    }

    @Test
    fun `mapa de calor limita intensidade a quatro`() {
        val sessions = (1..6).map { session("s$it", LocalDate.of(2026, 4, 1), minutes = 30) }

        val levels = Heatmap.levels(sessions, 2026, HeatMetric.COUNT)

        assertEquals(4, levels.getValue(LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `mapa de calor por horas separa por hora de treino`() {
        val sessions = listOf(session("1", LocalDate.of(2026, 4, 1), minutes = 150))

        assertEquals(3, Heatmap.levels(sessions, 2026, HeatMetric.HOURS).getValue(LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `distribuicao gi e no-gi ignora modalidade indefinida`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 4, 1), type = TrainingType.GI),
            session("2", LocalDate.of(2026, 4, 2), type = TrainingType.NO_GI),
            session("3", LocalDate.of(2026, 4, 3), type = TrainingType.DRILLING),
        )

        val (gi, noGi) = Distributions.giVsNoGi(sessions)

        assertEquals(1, gi.count)
        assertEquals(0.5, gi.fraction)
        assertEquals(1, noGi.count)
    }

    @Test
    fun `variacao percentual sem base e nula`() {
        assertNull(Comparison.percentChange(5.0, 0.0))
        assertEquals(50.0, Comparison.percentChange(3.0, 2.0)!!, 0.0001)
    }

    @Test
    fun `ranking de tags separa onde treinou de o que corrigir`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 4, 1), tags = setOf("Guarda"), focus = setOf("Passagem")),
            session("2", LocalDate.of(2026, 4, 2), tags = setOf("guarda", "Queda"), focus = setOf("passagem")),
        )

        assertEquals("guarda", TagRanking.rank(sessions, focus = false).first().name)
        assertEquals(2, TagRanking.rank(sessions, focus = true).first().count)
    }

    @Test
    fun `horarios e dias da semana ignoram treinos sem hora`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 4, 6), start = LocalTime.of(19, 0)),
            session("2", LocalDate.of(2026, 4, 6)),
        )

        assertEquals(mapOf(19 to 1), TimeOfDay.byHour(sessions))
        assertEquals(2, TimeOfDay.byWeekday(sessions).getValue(DayOfWeek.MONDAY))
    }

    @Test
    fun `filtro combina texto, tipo e intervalo`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 4, 1), type = TrainingType.SPARRING).copy(learned = "Costas e triângulo"),
            session("2", LocalDate.of(2026, 4, 2), type = TrainingType.GI).copy(learned = "Costas"),
        )

        val filtered = SessionFilter(
            query = "TRIÂNGULO",
            types = setOf(TrainingType.SPARRING),
            range = DateRange(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
        ).apply(sessions)

        assertEquals(listOf("1"), filtered.map { it.id })
        assertTrue(SessionFilter().apply(sessions).size == 2)
    }
}
