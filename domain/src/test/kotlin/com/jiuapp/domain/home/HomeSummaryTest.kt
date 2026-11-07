package com.jiuapp.domain.home

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class HomeSummaryTest {

    private val rank = Rank(Belt.PURPLE, 1)
    private val today = LocalDate.of(2026, 10, 14) // quarta-feira

    private fun session(id: String, date: LocalDate) = TrainingSession(
        id = id,
        date = date,
        durationMinutes = 60,
        type = TrainingType.GI,
        intensity = 3,
        rankOnDay = rank,
    )

    @Test
    fun `resume semana, meta e progresso`() {
        val sessions = listOf(
            session("a", LocalDate.of(2026, 10, 12)),
            session("b", LocalDate.of(2026, 10, 13)),
        )

        val summary = HomeSummary.from(sessions, emptyList(), emptyList(), emptyList(), weeklyGoal = 3, today = today)

        assertEquals(2, summary.weekSessions)
        assertEquals(2f / 3f, summary.goalProgress)
        assertNull(summary.daysSinceGraduation)
        assertFalse(summary.strainAlert)
    }

    @Test
    fun `progresso e limitado a um`() {
        val sessions = listOf(
            session("a", LocalDate.of(2026, 10, 12)),
            session("b", LocalDate.of(2026, 10, 13)),
        )

        val summary = HomeSummary.from(sessions, emptyList(), emptyList(), emptyList(), weeklyGoal = 1, today = today)

        assertEquals(1f, summary.goalProgress)
    }

    @Test
    fun `conta treinos desde a graduacao e dias passados`() {
        val graduation = Graduation(rank, LocalDate.of(2026, 10, 1))
        val sessions = listOf(
            session("antes", LocalDate.of(2026, 9, 30)),
            session("depois", LocalDate.of(2026, 10, 5)),
        )

        val summary = HomeSummary.from(sessions, listOf(graduation), emptyList(), emptyList(), weeklyGoal = 2, today = today)

        assertEquals(1, summary.sessionsSinceGraduation)
        assertEquals(13L, summary.daysSinceGraduation)
    }
}
