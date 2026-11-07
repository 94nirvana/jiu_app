package com.jiuapp.ui.techniques

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.repository.GamePlanRepository
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.PlanEdge
import com.jiuapp.domain.technique.PlanNode
import com.jiuapp.domain.technique.PlanNodeKind
import com.jiuapp.ui.nav.NEW
import com.jiuapp.ui.nav.PlanEditorRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editor do fluxograma: nós em ordem e conexões entre eles. Posições são calculadas na exibição. */
data class PlanEditorState(
    val name: String = "",
    val nodes: List<PlanNode> = emptyList(),
    val edges: List<PlanEdge> = emptyList(),
    val newNodeLabel: String = "",
    val newNodeKind: PlanNodeKind = PlanNodeKind.POSITION,
    val edgeFrom: String? = null,
    val edgeTo: String? = null,
    val loading: Boolean = true,
    val saved: Boolean = false,
    @StringRes val error: Int? = null,
)

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plans: GamePlanRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlanEditorRoute>()
    private val _state = MutableStateFlow(PlanEditorState())
    val state: StateFlow<PlanEditorState> = _state
    private var planId: String = UUID.randomUUID().toString()
    private var deletedAt: Instant? = null

    init {
        viewModelScope.launch {
            if (route.id != NEW) {
                val plan = plans.observeAll().first().firstOrNull { it.id == route.id }
                if (plan != null) {
                    planId = plan.id
                    deletedAt = plan.deletedAt
                    _state.value = PlanEditorState(name = plan.name, nodes = plan.nodes, edges = plan.edges, loading = false)
                    return@launch
                }
                _state.update { it.copy(loading = false, error = R.string.error_not_found) }
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun setName(value: String) = update { it.copy(name = value) }
    fun setNewNodeLabel(value: String) = update { it.copy(newNodeLabel = value) }
    fun setNewNodeKind(kind: PlanNodeKind) = update { it.copy(newNodeKind = kind) }
    fun selectEdgeFrom(id: String) = update { it.copy(edgeFrom = id) }
    fun selectEdgeTo(id: String) = update { it.copy(edgeTo = id) }

    fun addNode() = update { state ->
        val label = state.newNodeLabel.trim()
        if (label.isEmpty()) return@update state.copy(error = R.string.error_name_required)
        val index = state.nodes.size
        state.copy(
            nodes = state.nodes + PlanNode(UUID.randomUUID().toString(), label, state.newNodeKind, x = index.toFloat(), y = index.toFloat()),
            newNodeLabel = "",
        )
    }

    fun removeNode(id: String) = update { state ->
        state.copy(
            nodes = state.nodes.filterNot { it.id == id },
            edges = state.edges.filterNot { it.fromId == id || it.toId == id },
        )
    }

    fun addEdge() = update { state ->
        val from = state.edgeFrom
        val to = state.edgeTo
        if (from == null || to == null || from == to) return@update state.copy(error = R.string.error_plan_edge)
        if (state.edges.any { it.fromId == from && it.toId == to }) return@update state.copy(error = R.string.error_plan_edge_duplicate)
        state.copy(edges = state.edges + PlanEdge(from, to), edgeFrom = null, edgeTo = null)
    }

    fun removeEdge(index: Int) = update { state -> state.copy(edges = state.edges.filterIndexed { i, _ -> i != index }) }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            try {
                plans.save(
                    GamePlan(
                        name = current.name.trim(),
                        nodes = current.nodes,
                        edges = current.edges,
                        id = planId,
                        updatedAt = Instant.now(clock),
                        deletedAt = deletedAt,
                    ),
                )
                _state.update { it.copy(saved = true) }
            } catch (error: DomainException) {
                _state.update { it.copy(error = R.string.error_invalid_data) }
            } catch (error: IllegalArgumentException) {
                _state.update { it.copy(error = R.string.error_name_required) }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            plans.delete(planId, Instant.now(clock))
            _state.update { it.copy(saved = true) }
        }
    }

    /** Limpa o erro anterior antes de cada ação; a ação pode definir um novo erro. */
    private fun update(transform: (PlanEditorState) -> PlanEditorState) =
        _state.update { previous -> transform(previous.copy(error = null)) }
}
