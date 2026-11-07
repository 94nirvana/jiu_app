package com.jiuapp.domain.technique

import com.jiuapp.domain.InvalidSessionException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GamePlanTest {

    private val a = PlanNode("a", "Guarda", PlanNodeKind.START)
    private val b = PlanNode("b", "Triângulo", PlanNodeKind.FINISH)

    @Test
    fun `plano valido com conexoes entre nos existentes`() {
        GamePlan(name = "Plano", nodes = listOf(a, b), edges = listOf(PlanEdge("a", "b")), id = "p1")
    }

    @Test
    fun `conexao para no inexistente e rejeitada`() {
        assertFailsWith<InvalidSessionException> {
            GamePlan(name = "Plano", nodes = listOf(a), edges = listOf(PlanEdge("a", "z")), id = "p1")
        }
    }

    @Test
    fun `no ligado a si mesmo e rejeitado`() {
        assertFailsWith<InvalidSessionException> {
            GamePlan(name = "Plano", nodes = listOf(a), edges = listOf(PlanEdge("a", "a")), id = "p1")
        }
    }

    @Test
    fun `ids de nos duplicados sao rejeitados`() {
        assertFailsWith<InvalidSessionException> {
            GamePlan(name = "Plano", nodes = listOf(a, a), id = "p1")
        }
    }
}
