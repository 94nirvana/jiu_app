package com.jiuapp.ui.techniques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.repository.GamePlanRepository
import com.jiuapp.domain.repository.OpponentRepository
import com.jiuapp.domain.repository.TechniqueRepository
import com.jiuapp.domain.technique.GamePlan
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.domain.technique.SuggestedTechniques
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val techniques: List<Technique> = emptyList(),
    val categoryFilter: TechniqueCategory? = null,
    val statusFilter: TechniqueStatus? = null,
    val plans: List<GamePlan> = emptyList(),
    val opponents: List<Opponent> = emptyList(),
    val loading: Boolean = true,
    val suggestionsAdded: Int? = null,
)

@HiltViewModel
class TechniquesViewModel @Inject constructor(
    private val techniques: TechniqueRepository,
    private val plans: GamePlanRepository,
    private val opponents: OpponentRepository,
    private val clock: Clock,
) : ViewModel() {

    private val categoryFilter = MutableStateFlow<TechniqueCategory?>(null)
    private val statusFilter = MutableStateFlow<TechniqueStatus?>(null)
    private val suggestionsAdded = MutableStateFlow<Int?>(null)

    val state: StateFlow<LibraryUiState> = combine(
        techniques.observeAll(),
        plans.observeAll(),
        opponents.observeAll(),
        categoryFilter,
        statusFilter,
    ) { techniqueList, planList, opponentList, category, status ->
        LibraryUiState(
            techniques = techniqueList.filter { (category == null || it.category == category) && (status == null || it.status == status) },
            categoryFilter = category,
            statusFilter = status,
            plans = planList,
            opponents = opponentList,
            loading = false,
        )
    }.combine(suggestionsAdded) { base, added -> base.copy(suggestionsAdded = added) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun setCategory(category: TechniqueCategory?) = categoryFilter.update { if (it == category) null else category }

    fun setStatus(status: TechniqueStatus?) = statusFilter.update { if (it == status) null else status }

    /** Cadastro em lote da lista inicial. Não duplica técnicas com o mesmo nome. */
    fun addSuggestedTechniques() {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val now = Instant.now(clock)
            val existing = techniques.observeAll().first().map { it.name.lowercase() }.toSet()
            val toAdd = SuggestedTechniques.all.filter { (name, _) -> name.lowercase() !in existing }
            toAdd.forEach { (name, category) ->
                techniques.save(
                    Technique(
                        name = name,
                        category = category,
                        statusHistory = listOf(com.jiuapp.domain.technique.StatusChange(TechniqueStatus.LEARNING, today)),
                        id = UUID.randomUUID().toString(),
                        updatedAt = now,
                    ),
                )
            }
            suggestionsAdded.update { toAdd.size }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
