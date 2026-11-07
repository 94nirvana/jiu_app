package com.jiuapp.domain.competition

import com.jiuapp.domain.InvalidCompetitionException
import com.jiuapp.domain.sync.Syncable
import com.jiuapp.domain.training.MediaLink
import java.time.Instant

enum class MatchResult { WIN, LOSS, DRAW }

enum class VictoryMethod {
    SUBMISSION,
    POINTS,
    ADVANTAGES,
    REFEREE_DECISION,
    DISQUALIFICATION,
    WALKOVER,
    OTHER,
}

data class Match(
    val competitionId: String,
    val opponentName: String,
    val result: MatchResult,
    val method: VictoryMethod,
    val opponentId: String? = null,
    val submissionName: String? = null,
    val myPoints: Int = 0,
    val opponentPoints: Int = 0,
    val durationSeconds: Int? = null,
    val phase: String? = null,
    val notes: String? = null,
    val videoLinks: List<String> = emptyList(),
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(opponentName.isNotBlank()) { "Nome do adversário é obrigatório" }
        if (myPoints < 0 || opponentPoints < 0) throw InvalidCompetitionException("Pontuação não pode ser negativa")
        if (method == VictoryMethod.SUBMISSION && submissionName.isNullOrBlank()) {
            throw InvalidCompetitionException("Vitória/derrota por finalização exige o nome da finalização")
        }
        if (method == VictoryMethod.WALKOVER && result == MatchResult.LOSS) {
            throw InvalidCompetitionException("WO não pode ser derrota")
        }
        durationSeconds?.let {
            if (it !in 0..MAX_DURATION_SECONDS) throw InvalidCompetitionException("Duração inválida")
        }
        videoLinks.forEach(MediaLink::requireAllowed)
    }

    companion object {
        const val MAX_DURATION_SECONDS = 4 * 60 * 60
    }
}
