package com.jiuapp.domain.stats

import com.jiuapp.domain.InvalidGoalException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StreakTest {

    private val rank = Rank(Belt.WHITE, 0)

    /** Cria [count] treinos na segunda-feira da semana de [monday] + [weekOffset] semanas. */
    private fun sessionsInWeek(monday: LocalDate, weekOffset: Long, count: Int): List<TrainingSession> =
        (0 until count).map { i ->
            TrainingSession(
                id = "w${weekOffset}-$i",
                date = monday.plusWeeks(weekOffset).plusDays(i.toLong() % 7),
                durationMinutes = 60,
                type = TrainingType.GI,
                intensity = 3,
                rankOnDay = rank,
            )
        }

    private val wednesday = LocalDate.of(2026, 10, 14) // quarta-feira
    private val monday = LocalDate.of(2026, 10, 12)

    @Test
    fun `semana atual incompleta nao quebra a sequencia`() {
        val sessions = sessionsInWeek(monday, -1, 2) + sessionsInWeek(monday, -2, 2)

        val streak = currentStreak(sessions, weeklyGoal = 2, today = wednesday)

        assertEquals(2, streak)
    }

    @Test
    fun `semana atual com meta atingida entra na sequencia`() {
        val sessions = sessionsInWeek(monday, 0, 2) + sessionsInWeek(monday, -1, 2)

        assertEquals(2, currentStreak(sessions, weeklyGoal = 2, today = wednesday))
    }

    @Test
    fun `semana sem meta zera a sequencia atual`() {
        val sessions = sessionsInWeek(monday, -1, 1) + sessionsInWeek(monday, -2, 3)

        assertEquals(0, currentStreak(sessions, weeklyGoal = 2, today = wednesday))
    }

    @Test
    fun `maior sequencia considera lacunas entre semanas`() {
        val sessions = sessionsInWeek(monday, -5, 2) +
            sessionsInWeek(monday, -4, 2) +
            sessionsInWeek(monday, -3, 2) +
            sessionsInWeek(monday, -1, 2)

        assertEquals(3, longestStreak(sessions, weeklyGoal = 2))
    }

    @Test
    fun `historico vazio tem sequencia zero`() {
        assertEquals(0, longestStreak(emptyList(), weeklyGoal = 3))
    }

    @Test
    fun `meta menor que um e rejeitada`() {
        assertFailsWith<InvalidGoalException> { currentStreak(emptyList(), 0, wednesday) }
        assertFailsWith<InvalidGoalException> { longestStreak(emptyList(), 0) }
    }
}
