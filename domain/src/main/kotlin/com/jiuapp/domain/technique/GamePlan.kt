package com.jiuapp.domain.technique

import com.jiuapp.domain.InvalidSessionException
import com.jiuapp.domain.sync.Syncable
import java.time.Instant

enum class PlanNodeKind { START, POSITION, TECHNIQUE, FINISH }

data class PlanNode(
    val id: String,
    val label: String,
    val kind: PlanNodeKind,
    val x: Float = 0f,
    val y: Float = 0f,
)

data class PlanEdge(val fromId: String, val toId: String, val label: String? = null)

/** Fluxograma de plano de jogo: nós e conexões direcionadas. */
data class GamePlan(
    val name: String,
    val nodes: List<PlanNode> = emptyList(),
    val edges: List<PlanEdge> = emptyList(),
    override val id: String,
    override val updatedAt: Instant = Instant.EPOCH,
    override val deletedAt: Instant? = null,
) : Syncable {
    init {
        require(name.isNotBlank()) { "Nome do plano é obrigatório" }
        val ids = nodes.map { it.id }
        if (ids.toSet().size != ids.size) throw InvalidSessionException("Ids de nós duplicados")
        val known = ids.toSet()
        edges.forEach { edge ->
            if (edge.fromId !in known || edge.toId !in known) {
                throw InvalidSessionException("Conexão aponta para nó inexistente")
            }
            if (edge.fromId == edge.toId) throw InvalidSessionException("Conexão não pode apontar para o próprio nó")
        }
    }
}
