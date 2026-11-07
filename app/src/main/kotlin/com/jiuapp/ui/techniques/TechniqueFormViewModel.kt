package com.jiuapp.ui.techniques

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.repository.TechniqueRepository
import com.jiuapp.domain.technique.StatusChange
import com.jiuapp.domain.technique.Technique
import com.jiuapp.domain.technique.TechniqueCategory
import com.jiuapp.domain.technique.TechniqueStatus
import com.jiuapp.domain.training.MediaLink
import com.jiuapp.ui.nav.NEW
import com.jiuapp.ui.nav.TechniqueFormRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TechniqueFormState(
    val isNew: Boolean = true,
    val name: String = "",
    val category: TechniqueCategory = TechniqueCategory.POSITION,
    val originPosition: String = "",
    val description: String = "",
    val keyNotes: String = "",
    val videoLinksText: String = "",
    val status: TechniqueStatus = TechniqueStatus.LEARNING,
    val history: List<StatusChange> = emptyList(),
    val loading: Boolean = true,
    val saved: Boolean = false,
    @StringRes val error: Int? = null,
)

@HiltViewModel
class TechniqueFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val techniques: TechniqueRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<TechniqueFormRoute>()
    private val _state = MutableStateFlow(TechniqueFormState())
    val state: StateFlow<TechniqueFormState> = _state
    private var existing: Technique? = null

    init {
        viewModelScope.launch {
            if (route.id == NEW) {
                _state.update { it.copy(loading = false) }
            } else {
                val technique = techniques.get(route.id)
                existing = technique
                _state.value = technique?.let {
                    TechniqueFormState(
                        isNew = false,
                        name = it.name,
                        category = it.category,
                        originPosition = it.originPosition.orEmpty(),
                        description = it.description.orEmpty(),
                        keyNotes = it.keyNotes.orEmpty(),
                        videoLinksText = it.videoLinks.joinToString("\n"),
                        status = it.status,
                        history = it.statusHistory,
                        loading = false,
                    )
                } ?: _state.value.copy(loading = false, error = R.string.error_not_found)
            }
        }
    }

    fun update(transform: (TechniqueFormState) -> TechniqueFormState) = _state.update { transform(it).copy(error = null) }

    /** Mudança de status com data. Repetir o status atual não gera histórico. */
    fun changeStatus(status: TechniqueStatus) = update { it.copy(status = status) }

    fun save() {
        val current = _state.value
        val links = current.videoLinksText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (!links.all(MediaLink::isAllowed)) {
            _state.update { it.copy(error = R.string.error_video_link) }
            return
        }
        viewModelScope.launch {
            val now = Instant.now(clock)
            val today = LocalDate.now(clock)
            try {
                val base = existing ?: Technique(
                    name = current.name.trim(),
                    category = current.category,
                    statusHistory = listOf(StatusChange(TechniqueStatus.LEARNING, today)),
                    id = UUID.randomUUID().toString(),
                    updatedAt = now,
                )
                val withStatus = base.changeStatus(current.status, today)
                val saved = withStatus.copy(
                    name = current.name.trim(),
                    category = current.category,
                    originPosition = current.originPosition.trim().ifEmpty { null },
                    description = current.description.trim().ifEmpty { null },
                    keyNotes = current.keyNotes.trim().ifEmpty { null },
                    videoLinks = links,
                    updatedAt = now,
                )
                techniques.save(saved)
                _state.update { it.copy(saved = true) }
            } catch (error: DomainException) {
                _state.update { it.copy(error = R.string.error_invalid_data) }
            }
        }
    }

    fun delete() {
        val id = existing?.id ?: return
        viewModelScope.launch {
            techniques.delete(id, Instant.now(clock))
            _state.update { it.copy(saved = true) }
        }
    }
}
