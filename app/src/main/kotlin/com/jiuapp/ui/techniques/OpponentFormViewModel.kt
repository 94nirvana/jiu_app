package com.jiuapp.ui.techniques

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.repository.OpponentRepository
import com.jiuapp.domain.technique.Opponent
import com.jiuapp.ui.nav.NEW
import com.jiuapp.ui.nav.OpponentFormRoute
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

data class OpponentFormState(
    val name: String = "",
    val level: Int = DEFAULT_LEVEL,
    val strengths: String = "",
    val worked: String = "",
    val didNotWork: String = "",
    val notes: String = "",
    val loading: Boolean = true,
    val saved: Boolean = false,
    @StringRes val error: Int? = null,
) {
    companion object {
        const val DEFAULT_LEVEL = 3
    }
}

@HiltViewModel
class OpponentFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val opponents: OpponentRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<OpponentFormRoute>()
    private val _state = MutableStateFlow(OpponentFormState())
    val state: StateFlow<OpponentFormState> = _state
    private var existing: Opponent? = null

    init {
        viewModelScope.launch {
            if (route.id != NEW) {
                val found = opponents.observeAll().first().firstOrNull { it.id == route.id }
                existing = found
                _state.value = found?.let {
                    OpponentFormState(
                        name = it.name,
                        level = it.level,
                        strengths = it.strengths.orEmpty(),
                        worked = it.worked.orEmpty(),
                        didNotWork = it.didNotWork.orEmpty(),
                        notes = it.notes.orEmpty(),
                        loading = false,
                    )
                } ?: _state.value.copy(loading = false, error = R.string.error_not_found)
            } else {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun update(transform: (OpponentFormState) -> OpponentFormState) = _state.update { transform(it).copy(error = null) }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            try {
                val base = existing
                opponents.save(
                    Opponent(
                        name = current.name.trim(),
                        level = current.level,
                        strengths = current.strengths.trim().ifEmpty { null },
                        worked = current.worked.trim().ifEmpty { null },
                        didNotWork = current.didNotWork.trim().ifEmpty { null },
                        notes = current.notes.trim().ifEmpty { null },
                        id = base?.id ?: UUID.randomUUID().toString(),
                        updatedAt = Instant.now(clock),
                        deletedAt = base?.deletedAt,
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
        val id = existing?.id ?: return
        viewModelScope.launch {
            opponents.delete(id, Instant.now(clock))
            _state.update { it.copy(saved = true) }
        }
    }
}
