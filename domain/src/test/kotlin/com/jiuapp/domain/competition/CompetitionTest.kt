package com.jiuapp.domain.competition

import com.jiuapp.domain.InvalidCompetitionException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompetitionTest {

    private val today = LocalDate.of(2026, 5, 1)

    private fun match(result: MatchResult, method: VictoryMethod, sub: String? = null, id: String) = Match(
        competitionId = "c1",
        opponentName = "Adversário",
        result = result,
        method = method,
        submissionName = sub,
        id = id,
    )

    @Test
    fun `finalizacao exige nome da finalizacao`() {
        assertFailsWith<InvalidCompetitionException> {
            match(MatchResult.WIN, VictoryMethod.SUBMISSION, sub = null, id = "m1")
        }
    }

    @Test
    fun `WO nao pode ser derrota`() {
        assertFailsWith<InvalidCompetitionException> {
            match(MatchResult.LOSS, VictoryMethod.WALKOVER, id = "m2")
        }
    }

    @Test
    fun `campanha conta vitorias, derrotas e WO`() {
        val matches = listOf(
            match(MatchResult.WIN, VictoryMethod.SUBMISSION, "Triângulo", "1"),
            match(MatchResult.WIN, VictoryMethod.POINTS, id = "2"),
            match(MatchResult.LOSS, VictoryMethod.SUBMISSION, "Armlock", "3"),
            match(MatchResult.WIN, VictoryMethod.WALKOVER, id = "4"),
        )

        val summary = CompetitionStats.campaign(matches)

        assertEquals(3, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(1, summary.walkovers)
        assertEquals(2.0 / 3.0, summary.winRate!!, 0.0001)
    }

    @Test
    fun `finalizacoes usadas e sofridas sao agrupadas sem diferenca de caixa`() {
        val matches = listOf(
            match(MatchResult.WIN, VictoryMethod.SUBMISSION, "Triângulo", "1"),
            match(MatchResult.WIN, VictoryMethod.SUBMISSION, "triângulo", "2"),
            match(MatchResult.LOSS, VictoryMethod.SUBMISSION, "Armlock", "3"),
        )

        assertEquals(2, CompetitionStats.submissionsWon(matches).getValue("triângulo"))
        assertEquals(1, CompetitionStats.submissionsSuffered(matches).getValue("armlock"))
    }

    @Test
    fun `taxa de vitoria sem lutas e nula`() {
        assertNull(CompetitionStats.campaign(emptyList()).winRate)
    }

    @Test
    fun `proximo campeonato ignora passados`() {
        val past = Competition(name = "Antigo", date = LocalDate.of(2026, 1, 1), id = "a")
        val soon = Competition(name = "Breve", date = LocalDate.of(2026, 6, 1), id = "b")
        val later = Competition(name = "Depois", date = LocalDate.of(2026, 9, 1), id = "c")

        val next = nextCompetition(listOf(past, later, soon), today)

        assertNotNull(next)
        assertEquals("Breve", next.name)
        assertEquals(31L, soon.daysUntil(today))
        assertTrue(soon.isUpcoming(today))
        assertFalse(past.isUpcoming(today))
    }

    @Test
    fun `checklist calcula progresso`() {
        val competition = Competition(
            name = "Copa",
            date = LocalDate.of(2026, 6, 1),
            checklist = listOf(
                ChecklistItem("Kimono", done = true),
                ChecklistItem("Inscrição"),
            ),
            id = "c",
        )

        assertEquals(0.5f, competition.checklistProgress())
    }

    @Test
    fun `pesagem depois do campeonato e rejeitada`() {
        assertFailsWith<InvalidCompetitionException> {
            Competition(name = "X", date = today, weighInDate = today.plusDays(1), id = "c")
        }
    }

    @Test
    fun `plano de corte calcula perda semanal e marca perda rapida`() {
        val plan = WeightCut.plan(currentKg = 80.0, targetKg = 76.0, daysUntilWeighIn = 14)

        assertNotNull(plan)
        assertEquals(4.0, plan.requiredLossKg)
        assertEquals(2.5, plan.lossPerWeekPercent)
        assertTrue(plan.isRapid)
    }

    @Test
    fun `corte viavel nao e marcado como rapido`() {
        val plan = WeightCut.plan(currentKg = 80.0, targetKg = 79.0, daysUntilWeighIn = 28)

        assertNotNull(plan)
        assertFalse(plan.isRapid)
    }

    @Test
    fun `sem perda necessaria nao ha plano`() {
        assertNull(WeightCut.plan(currentKg = 70.0, targetKg = 72.0, daysUntilWeighIn = 10))
    }

    @Test
    fun `dias minimos respeitam o limite semanal`() {
        assertEquals(35L, WeightCut.minimumDaysFor(currentKg = 80.0, lossKg = 4.0))
    }
}
