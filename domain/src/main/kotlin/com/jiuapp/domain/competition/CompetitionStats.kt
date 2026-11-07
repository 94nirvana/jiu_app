package com.jiuapp.domain.competition

import com.jiuapp.domain.competition.MatchResult.DRAW
import com.jiuapp.domain.competition.MatchResult.LOSS
import com.jiuapp.domain.competition.MatchResult.WIN
import com.jiuapp.domain.competition.VictoryMethod.SUBMISSION
import com.jiuapp.domain.competition.VictoryMethod.WALKOVER

data class CampaignSummary(
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val walkovers: Int,
) {
    val total: Int get() = wins + losses + draws

    /** Taxa de vitória sobre lutas disputadas. WO entra como vitória, mas não conta como luta. */
    val winRate: Double?
        get() {
            val fought = wins - walkovers + losses + draws
            return if (fought == 0) null else (wins - walkovers).toDouble() / fought
        }
}

data class CompetitionResult(val competitionId: String, val summary: CampaignSummary, val placement: Int?)

object CompetitionStats {

    fun campaign(matches: List<Match>): CampaignSummary {
        val active = matches.filter { it.deletedAt == null }
        return CampaignSummary(
            wins = active.count { it.result == WIN },
            losses = active.count { it.result == LOSS },
            draws = active.count { it.result == DRAW },
            walkovers = active.count { it.method == WALKOVER },
        )
    }

    /** Finalizações mais usadas em vitórias. */
    fun submissionsWon(matches: List<Match>): Map<String, Int> =
        countSubmissions(matches.filter { it.result == WIN && it.method == SUBMISSION })

    /** Finalizações sofridas (derrotas por finalização). */
    fun submissionsSuffered(matches: List<Match>): Map<String, Int> =
        countSubmissions(matches.filter { it.result == LOSS && it.method == SUBMISSION })

    fun perCompetition(matches: List<Match>, competitions: List<Competition>): List<CompetitionResult> =
        competitions
            .filter { it.deletedAt == null }
            .map { competition ->
                val own = matches.filter { it.competitionId == competition.id && it.deletedAt == null }
                CompetitionResult(competition.id, campaign(own), competition.finalPlacement)
            }

    private fun countSubmissions(matches: List<Match>): Map<String, Int> =
        matches
            .mapNotNull { it.submissionName?.trim()?.takeIf(String::isNotEmpty) }
            .groupingBy { it.lowercase() }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .toMap()
}
