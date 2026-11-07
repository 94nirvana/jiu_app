package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrainingStatsTest {

    private val white = Rank(Belt.WHITE, 4)
    private val blue = Rank(Belt.BLUE, 0)
    private val blueGraduation = Graduation(blue, LocalDate.of(2026, 6, 1))

    private fun session(id: String, date: LocalDate, rank: Rank, minutes: Int = 60) = TrainingSession(
        id = id,
        date = date,
        durationMinutes = minutes,
        type = TrainingType.TECHNIQUE,
        intensity = 2,
        rankOnDay = rank,
    )

    @Test
    fun `treinos anteriores a graduacao mantem a faixa do dia`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 5, 2), white, minutes = 60),
            session("2", LocalDate.of(2026, 7, 2), blue, minutes = 90),
        )

        val totals = totalsByRank(sessions)

        assertEquals(1, totals.getValue(white).sessions)
        assertEquals(60, totals.getValue(white).minutes)
        assertEquals(1, totals.getValue(blue).sessions)
        assertEquals(1.5, totals.getValue(blue).hours)
    }

    @Test
    fun `totais por faixa agregam todos os graus`() {
        val sessions = listOf(
            session("1", LocalDate.of(2026, 7, 2), Rank(Belt.BLUE, 1)),
            session("2", LocalDate.of(2026, 7, 3), Rank(Belt.BLUE, 2)),
        )

        assertEquals(2, totalsByBelt(sessions).getValue(Belt.BLUE).sessions)
    }

    @Test
    fun `treinos desde a ultima graduacao excluem o dia da graduacao`() {
        val sessions = listOf(
            session("antes", LocalDate.of(2026, 5, 30), white),
            session("dia", LocalDate.of(2026, 6, 1), blue),
            session("depois", LocalDate.of(2026, 6, 2), blue),
        )

        val since = sessionsSince(sessions, blueGraduation)

        assertEquals(listOf("depois"), since.map { it.id })
    }

    @Test
    fun `sem graduacao todos os treinos contam`() {
        val sessions = listOf(session("1", LocalDate.of(2026, 1, 1), white))

        assertEquals(1, sessionsSince(sessions, null).size)
    }

    @Test
    fun `dias desde a graduacao e a ultima graduacao cronologica`() {
        val older = Graduation(white, LocalDate.of(2025, 1, 1))
        val latest = latestGraduation(listOf(blueGraduation, older))

        assertEquals(blueGraduation, latest)
        assertEquals(153L, daysSinceGraduation(latest, LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `sem graduacao nao ha contagem de dias`() {
        assertNull(daysSinceGraduation(null, LocalDate.of(2026, 1, 1)))
        assertNull(latestGraduation(emptyList()))
    }
}
