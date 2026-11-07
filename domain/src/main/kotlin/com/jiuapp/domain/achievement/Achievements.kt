package com.jiuapp.domain.achievement

import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.stats.longestMonthlyStreak
import com.jiuapp.domain.stats.longestStreak
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate

enum class AchievementId {
    SESSIONS_1,
    SESSIONS_10,
    SESSIONS_50,
    SESSIONS_100,
    SESSIONS_250,
    SESSIONS_500,
    SESSIONS_1000,
    WEEK_STREAK_4,
    WEEK_STREAK_12,
    WEEK_STREAK_52,
    YEAR_OF_TRAINING,
    FIRST_COMPETITION,
    FIRST_GRADUATION,
}

/**
 * Conjuntos de conquistas satisfeitas hoje. A camada de dados guarda a data do primeiro desbloqueio
 * e não repete o aviso.
 */
object Achievements {

    private val sessionMilestones = listOf(
        1 to AchievementId.SESSIONS_1,
        10 to AchievementId.SESSIONS_10,
        50 to AchievementId.SESSIONS_50,
        100 to AchievementId.SESSIONS_100,
        250 to AchievementId.SESSIONS_250,
        500 to AchievementId.SESSIONS_500,
        1000 to AchievementId.SESSIONS_1000,
    )

    fun satisfied(
        sessions: List<TrainingSession>,
        graduations: List<Graduation>,
        matches: List<Match>,
        weeklyGoal: Int,
        today: LocalDate,
    ): Set<AchievementId> {
        val live = sessions.filter { it.deletedAt == null && !it.date.isAfter(today) }
        val count = live.size
        val result = mutableSetOf<AchievementId>()

        sessionMilestones.filter { count >= it.first }.forEach { result += it.second }

        val weeks = longestStreak(live, weeklyGoal)
        if (weeks >= 4) result += AchievementId.WEEK_STREAK_4
        if (weeks >= 12) result += AchievementId.WEEK_STREAK_12
        if (weeks >= 52) result += AchievementId.WEEK_STREAK_52

        if (longestMonthlyStreak(live) >= 12) result += AchievementId.YEAR_OF_TRAINING
        if (matches.any { it.deletedAt == null }) result += AchievementId.FIRST_COMPETITION
        // O primeiro registro costuma ser a faixa inicial do cadastro; a conquista é a graduação seguinte.
        if (graduations.count { it.deletedAt == null } >= 2) result += AchievementId.FIRST_GRADUATION

        return result
    }
}
