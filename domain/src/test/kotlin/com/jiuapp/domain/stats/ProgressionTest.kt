package com.jiuapp.domain.stats

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.Graduation
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressionTest {

    private val g1 = Graduation(Rank(Belt.WHITE, 0), LocalDate.of(2020, 1, 1), id = "1")
    private val g2 = Graduation(Rank(Belt.WHITE, 1), LocalDate.of(2020, 4, 1), id = "2")
    private val g3 = Graduation(Rank(Belt.WHITE, 2), LocalDate.of(2020, 10, 1), id = "3")

    @Test
    fun `linha do tempo ordena e calcula intervalos`() {
        val timeline = Progression.timeline(listOf(g3, g1, g2))

        assertEquals(listOf(null, 91L, 183L), timeline.map { it.daysSincePrevious })
    }

    @Test
    fun `estimativa usa intervalo medio e nao inventa sem historico`() {
        assertNull(Progression.estimateNextGraduation(listOf(g1)))
        assertEquals(LocalDate.of(2020, 10, 1).plusDays(137), Progression.estimateNextGraduation(listOf(g1, g2, g3)))
    }

    @Test
    fun `meta semanal atingida tem prioridade`() {
        assertEquals(GoalStatus.ACHIEVED, WeeklyGoal.status(3, 3, LocalDate.of(2026, 4, 1)))
    }

    @Test
    fun `meta semanal considera ritmo esperado no dia`() {
        val wednesday = LocalDate.of(2026, 4, 1) // dia 3 da semana
        assertEquals(GoalStatus.BEHIND, WeeklyGoal.status(0, 3, wednesday))
        assertEquals(GoalStatus.ON_TRACK, WeeklyGoal.status(1, 3, wednesday))
    }
}
