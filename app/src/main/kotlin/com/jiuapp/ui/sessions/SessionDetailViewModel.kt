package com.jiuapp.ui.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.TechniqueRepository
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.ui.nav.SessionDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val loading: Boolean = true,
    val session: TrainingSession? = null,
    val techniqueNames: List<String> = emptyList(),
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessions: SessionRepository,
    techniques: TechniqueRepository,
    private val clock: Clock,
) : ViewModel() {

    private val sessionId = savedStateHandle.toRoute<SessionDetailRoute>().id

    val state: StateFlow<SessionDetailUiState> = combine(
        sessions.observeAll().map { list -> list.firstOrNull { it.id == sessionId } },
        techniques.observeAll(),
    ) { session, techniqueList ->
        SessionDetailUiState(
            loading = false,
            session = session,
            techniqueNames = session?.techniqueIds
                ?.mapNotNull { id -> techniqueList.firstOrNull { it.id == id }?.name }
                .orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionDetailUiState())

    /** Exclusão lógica: o registro segue no backup e na sincronização até ser purgado. */
    fun delete() {
        viewModelScope.launch {
            sessions.delete(sessionId, Instant.now(clock))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
