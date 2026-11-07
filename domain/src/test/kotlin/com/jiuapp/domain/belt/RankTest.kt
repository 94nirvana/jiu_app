package com.jiuapp.domain.belt

import com.jiuapp.domain.InvalidRankException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RankTest {

    @Test
    fun `faixa tem precedencia sobre o grau na ordenacao`() {
        val blueFour = Rank(Belt.BLUE, 4)
        val purpleZero = Rank(Belt.PURPLE, 0)

        assertTrue(blueFour < purpleZero)
    }

    @Test
    fun `graus dentro da mesma faixa ordenam numericamente`() {
        val first = Rank(Belt.WHITE, 1)
        val third = Rank(Belt.WHITE, 3)

        assertTrue(first < third)
    }

    @Test
    fun `grau acima do maximo e rejeitado`() {
        val error = assertFailsWith<InvalidRankException> { Rank(Belt.BLACK, Belt.MAX_DEGREE + 1) }

        assertEquals("Grau fora do intervalo 0..4: 5", error.message)
    }

    @Test
    fun `grau negativo e rejeitado`() {
        assertFailsWith<InvalidRankException> { Rank(Belt.WHITE, -1) }
    }
}
