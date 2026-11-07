package com.jiuapp.domain.training

import com.jiuapp.domain.InvalidSessionException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TrainingSessionTest {

    private val rank = Rank(Belt.BLUE, 2)

    private fun session(
        duration: Int = 90,
        intensity: Int = 3,
        fatigue: Int? = null,
        pain: Int? = null,
    ) = TrainingSession(
        id = "s1",
        date = LocalDate.of(2026, 3, 10),
        durationMinutes = duration,
        type = TrainingType.GI,
        intensity = intensity,
        rankOnDay = rank,
        fatigue = fatigue,
        pain = pain,
    )

    @Test
    fun `aceita valores nos limites validos`() {
        session(duration = TrainingSession.MIN_DURATION, intensity = 1, fatigue = 5, pain = 1)
        session(duration = TrainingSession.MAX_DURATION, intensity = 5)
    }

    @Test
    fun `rejeita duracao zero`() {
        assertFailsWith<InvalidSessionException> { session(duration = 0) }
    }

    @Test
    fun `rejeita duracao acima do maximo`() {
        assertFailsWith<InvalidSessionException> { session(duration = TrainingSession.MAX_DURATION + 1) }
    }

    @Test
    fun `rejeita intensidade fora da escala`() {
        assertFailsWith<InvalidSessionException> { session(intensity = 6) }
    }

    @Test
    fun `rejeita cansaco fora da escala`() {
        assertFailsWith<InvalidSessionException> { session(fatigue = 0) }
    }

    @Test
    fun `rejeita dor fora da escala`() {
        assertFailsWith<InvalidSessionException> { session(pain = 9) }
    }
}
