package com.jiuapp.domain.home

import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.nextCompetition
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.activeInjuries
import com.jiuapp.domain.load.OverloadAlert
import com.jiuapp.domain.load.overloadAlert
import com.jiuapp.domain.load.repeatedHighStrainAlert
import com.jiuapp.domain.stats.GoalStatus
import com.jiuapp.domain.stats.WeeklyGoal
import com.jiuapp.domain.stats.currentStreak
import com.jiuapp.domain.stats.daysSinceGraduation
import com.jiuapp.domain.stats.latestGraduation
import com.jiuapp.domain.stats.sessionsSince
import com.jiuapp.domain.stats.toTotals
import com.jiuapp.domain.stats.weekStart
import com.jiuapp.domain.training.Graduation
import com.jiuapp.domain.training.TrainingSession
import java.time.LocalDate

/** Dados prontos para a tela inicial. Calculado no domínio, sem depender de Android. */
data class HomeSummary(
    val weekSessions: Int,
    val weeklyGoal: Int,
    val goalStatus: GoalStatus,
    val streakWeeks: Int,
    val sessionsSinceGraduation: Int,
    val daysSinceGraduation: Long?,
    val overload: OverloadAlert?,
    val strainAlert: Boolean,
    val nextCompetition: Competition?,
    val activeInjuries: List<Injury>,
    val lastSessionDate: LocalDate?,
) {
    val goalProgress: Float
        get() = (weekSessions.toFloat() / weeklyGoal).coerceAtMost(1f)

    companion object {
        fun from(
            sessions: List<TrainingSession>,
            graduations: List<Graduation>,
            competitions: List<Competition>,
            injuries: List<Injury>,
            weeklyGoal: Int,
            today: LocalDate,
        ): HomeSummary {
            val live = sessions.filter { it.deletedAt == null }
            val lastGraduation = latestGraduation(graduations)
            val week = today.weekStart()
            val weekSessions = live.filter { it.date.weekStart() == week }.toTotals().sessions
            return HomeSummary(
                weekSessions = weekSessions,
                weeklyGoal = weeklyGoal,
                goalStatus = WeeklyGoal.status(weekSessions, weeklyGoal, today),
                streakWeeks = currentStreak(live, weeklyGoal, today),
                sessionsSinceGraduation = sessionsSince(live, lastGraduation).size,
                daysSinceGraduation = daysSinceGraduation(lastGraduation, today),
                overload = overloadAlert(live, today),
                strainAlert = repeatedHighStrainAlert(live),
                nextCompetition = nextCompetition(competitions, today),
                activeInjuries = activeInjuries(injuries.filter { it.deletedAt == null }),
                lastSessionDate = live.filter { !it.date.isAfter(today) }.maxOfOrNull { it.date },
            )
        }
    }
}
