package com.jiuapp.domain.load

import com.jiuapp.domain.InvalidGoalException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class TrainingLoadTest {

    private val rank = Rank(Belt.BLUE, 1)
    private val today = LocalDate.of(2026, 10, 14) // quarta-feira
    private val thisMonday = LocalDate.of(2026, 10, 12)

    private fun session(
        date: LocalDate,
        duration: Int = 60,
        intensity: Int = 3,
        fatigue: Int? = null,
        pain: Int? = null,
        id: String = "${date}-${duration}",
    ) = TrainingSession(
        id = id,
        date = date,
        durationMinutes = duration,
        type = TrainingType.SPARRING,
        intensity = intensity,
        rankOnDay = rank,
        fatigue = fatigue,
        pain = pain,
    )

    @Test
    fun `carga de sessao e duracao vezes intensidade`() {
        assertEquals(180, sessionLoad(session(today, duration = 60, intensity = 3)))
    }

    @Test
    fun `alerta de sobrecarga dispara quando semana atual passa do limite`() {
        val history = (1..4).map { session(thisMonday.minusWeeks(it.toLong()), duration = 60, intensity = 3) }
        val current = listOf(session(thisMonday, duration = 120, intensity = 5))

        val alert = overloadAlert(history + current, today)

        requireNotNull(alert)
        assertEquals(600, alert.currentLoad)
        assertEquals(180.0, alert.averageLoad)
    }

    @Test
    fun `sem sobrecarga nao ha alerta`() {
        val history = (1..4).map { session(thisMonday.minusWeeks(it.toLong()), duration = 60, intensity = 3) }
        val current = listOf(session(thisMonday, duration = 60, intensity = 3))

        assertNull(overloadAlert(history + current, today))
    }

    @Test
    fun `sem historico de carga nao ha alerta de sobrecarga`() {
        val current = listOf(session(thisMonday, duration = 120, intensity = 5))

        assertNull(overloadAlert(current, today))
    }

    @Test
    fun `semanas sem treino contam como zero na media`() {
        val history = listOf(session(thisMonday.minusWeeks(1), duration = 120, intensity = 5))

        assertEquals(150.0, averagePreviousWeeklyLoad(history, today))
    }

    @Test
    fun `limite de sobrecarga menor ou igual a um e rejeitado`() {
        assertFailsWith<InvalidGoalException> { overloadAlert(emptyList(), today, threshold = 1.0) }
    }

    @Test
    fun `tres treinos seguidos com cansaco alto disparam alerta de desgaste`() {
        val sessions = listOf(
            session(today.minusDays(3), fatigue = 4),
            session(today.minusDays(2), pain = 5),
            session(today.minusDays(1), fatigue = 4),
        )

        assertTrue(repeatedHighStrainAlert(sessions))
    }

    @Test
    fun `um treino sem metricas quebra a sequencia de desgaste`() {
        val sessions = listOf(
            session(today.minusDays(3), fatigue = 5),
            session(today.minusDays(2)),
            session(today.minusDays(1), fatigue = 5),
        )

        assertFalse(repeatedHighStrainAlert(sessions))
    }

    @Test
    fun `menos treinos que a sequencia exigida nao disparam alerta`() {
        val sessions = listOf(session(today, fatigue = 5), session(today.minusDays(1), fatigue = 5))

        assertFalse(repeatedHighStrainAlert(sessions))
    }
}
