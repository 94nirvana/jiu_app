package com.jiuapp.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.stats.SessionFilter
import com.jiuapp.domain.training.TrainingSession
import com.jiuapp.domain.training.TrainingType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class SessionsView { LIST, CALENDAR }

data class SessionsUiState(
    val view: SessionsView = SessionsView.LIST,
    val filter: SessionFilter = SessionFilter(),
    val sessions: List<TrainingSession> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = null,
    val loading: Boolean = true,
) {
    /** Dias do mês com pelo menos um treino, com a quantidade de treinos. */
    val trainedDays: Map<LocalDate, Int>
        get() = sessions
            .filter { YearMonth.from(it.date) == month }
            .groupingBy { it.date }
            .eachCount()
}

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val view = MutableStateFlow(SessionsView.LIST)
    private val filter = MutableStateFlow(SessionFilter())
    private val month = MutableStateFlow(YearMonth.now(clock))
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    val state: StateFlow<SessionsUiState> = combine(
        sessions.observeAll(),
        view,
        filter,
        month,
        selectedDay,
    ) { all, currentView, currentFilter, currentMonth, day ->
        SessionsUiState(
            view = currentView,
            filter = currentFilter,
            sessions = currentFilter.apply(all),
            month = currentMonth,
            selectedDay = day,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SessionsUiState())

    fun setView(value: SessionsView) = view.update { value }

    fun setQuery(value: String) = filter.update { it.copy(query = value) }

    fun toggleType(type: TrainingType) = filter.update {
        it.copy(types = if (type in it.types) it.types - type else it.types + type)
    }

    fun clearFilters() = filter.update { SessionFilter(query = it.query) }

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun selectDay(day: LocalDate?) = selectedDay.update { if (it == day) null else day }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
