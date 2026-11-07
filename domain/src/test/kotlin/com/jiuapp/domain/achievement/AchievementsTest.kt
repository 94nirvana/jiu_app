package com.jiuapp.domain.achievement

import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.competition.MatchResult
import com.jiuapp.domain.competition.VictoryMethod
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementsTest {

    private val rank = Rank(Belt.WHITE, 0)

    private fun sessions(count: Int, start: LocalDate) = (0 until count).map { i ->
        TrainingSession(
            id = "s$i",
            date = start.plusDays(i.toLong()),
            durationMinutes = 60,
            type = TrainingType.GI,
            intensity = 3,
            rankOnDay = rank,
        )
    }

    @Test
    fun `marcos de treinos sao desbloqueados pela contagem`() {
        val result = Achievements.satisfied(
            sessions = sessions(10, LocalDate.of(2026, 1, 1)),
            graduations = emptyList(),
            matches = emptyList(),
            weeklyGoal = 3,
            today = LocalDate.of(2026, 6, 1),
        )

        assertTrue(AchievementId.SESSIONS_1 in result)
        assertTrue(AchievementId.SESSIONS_10 in result)
        assertFalse(AchievementId.SESSIONS_50 in result)
    }

    @Test
    fun `treinos futuros nao contam`() {
        val result = Achievements.satisfied(
            sessions = sessions(1, LocalDate.of(2027, 1, 1)),
            graduations = emptyList(),
            matches = emptyList(),
            weeklyGoal = 1,
            today = LocalDate.of(2026, 6, 1),
        )

        assertFalse(AchievementId.SESSIONS_1 in result)
    }

    @Test
    fun `primeira graduacao depende de duas graduacoes registradas`() {
        val initial = Graduation(Rank(Belt.WHITE, 0), LocalDate.of(2025, 1, 1))
        val next = Graduation(Rank(Belt.WHITE, 1), LocalDate.of(2025, 6, 1))
        val today = LocalDate.of(2026, 1, 1)

        val onlyInitial = Achievements.satisfied(emptyList(), listOf(initial), emptyList(), 3, today)
        val withNext = Achievements.satisfied(emptyList(), listOf(initial, next), emptyList(), 3, today)

        assertFalse(AchievementId.FIRST_GRADUATION in onlyInitial)
        assertTrue(AchievementId.FIRST_GRADUATION in withNext)
    }

    @Test
    fun `primeira competicao conta a partir de uma luta registrada`() {
        val match = Match(
            competitionId = "c",
            opponentName = "X",
            result = MatchResult.WIN,
            method = VictoryMethod.POINTS,
            id = "m",
        )

        val result = Achievements.satisfied(emptyList(), emptyList(), listOf(match), 3, LocalDate.of(2026, 1, 1))

        assertTrue(AchievementId.FIRST_COMPETITION in result)
    }

    @Test
    fun `sequencia de quatro semanas desbloqueia a conquista`() {
        val start = LocalDate.of(2026, 3, 2)
        val four = (0 until 4).flatMap { week -> sessions(2, start.plusWeeks(week.toLong())) }

        val result = Achievements.satisfied(four, emptyList(), emptyList(), 2, LocalDate.of(2026, 4, 1))

        assertTrue(AchievementId.WEEK_STREAK_4 in result)
    }
}
