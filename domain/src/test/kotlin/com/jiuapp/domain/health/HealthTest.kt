package com.jiuapp.domain.health

import com.jiuapp.domain.InvalidHealthRecordException
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthTest {

    private fun injury(region: BodyRegion, id: String, recovered: LocalDate? = null) = Injury(
        region = region,
        type = "Entorse",
        severity = 3,
        startDate = LocalDate.of(2026, 1, 1),
        actualRecoveryDate = recovered,
        id = id,
    )

    @Test
    fun `lesao sem recuperacao real esta ativa`() {
        assertTrue(injury(BodyRegion.KNEE_LEFT, "1").isActive())
        assertFalse(injury(BodyRegion.KNEE_LEFT, "2", recovered = LocalDate.of(2026, 2, 1)).isActive())
    }

    @Test
    fun `recuperacao antes do inicio e rejeitada`() {
        assertFailsWith<InvalidHealthRecordException> {
            injury(BodyRegion.BACK, "3", recovered = LocalDate.of(2025, 12, 1))
        }
    }

    @Test
    fun `regiao com duas lesoes e recorrente`() {
        val injuries = listOf(injury(BodyRegion.KNEE_RIGHT, "1"), injury(BodyRegion.KNEE_RIGHT, "2"), injury(BodyRegion.RIBS, "3"))

        assertEquals(setOf(BodyRegion.KNEE_RIGHT), recurringRegions(injuries))
    }

    @Test
    fun `peso implausivel e rejeitado`() {
        assertFailsWith<InvalidHealthRecordException> { WeightEntry(LocalDate.of(2026, 1, 1), 5.0, id = "w") }
    }

    @Test
    fun `perda semanal acima de um por cento gera alerta`() {
        val entries = listOf(
            WeightEntry(LocalDate.of(2026, 3, 1), 80.0, id = "a"),
            WeightEntry(LocalDate.of(2026, 3, 8), 78.0, id = "b"),
        )

        val alert = WeightAnalysis.rapidLossAlert(entries)

        assertNotNull(alert)
        assertEquals(-2.5, alert.weeklyLossPercent, 0.01)
    }

    @Test
    fun `ganho de peso nao gera alerta de perda rapida`() {
        val entries = listOf(
            WeightEntry(LocalDate.of(2026, 3, 1), 80.0, id = "a"),
            WeightEntry(LocalDate.of(2026, 3, 8), 82.0, id = "b"),
        )

        assertNull(WeightAnalysis.rapidLossAlert(entries))
    }

    @Test
    fun `sem medicao de referencia de 4 a 10 dias nao ha variacao`() {
        val entries = listOf(
            WeightEntry(LocalDate.of(2026, 3, 1), 80.0, id = "a"),
            WeightEntry(LocalDate.of(2026, 3, 2), 79.0, id = "b"),
        )

        assertNull(WeightAnalysis.weeklyChangePercent(entries))
    }
}
