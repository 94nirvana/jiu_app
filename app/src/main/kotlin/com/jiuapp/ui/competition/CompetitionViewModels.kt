package com.jiuapp.ui.competition

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.competition.CampaignSummary
import com.jiuapp.domain.competition.ChecklistItem
import com.jiuapp.domain.competition.Competition
import com.jiuapp.domain.competition.CompetitionStats
import com.jiuapp.domain.competition.Match
import com.jiuapp.domain.competition.WeightCut
import com.jiuapp.domain.competition.WeightCutPlan
import com.jiuapp.domain.repository.CompetitionRepository
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.ui.nav.CompetitionDetailRoute
import com.jiuapp.ui.nav.CompetitionFormRoute
import com.jiuapp.ui.nav.NEW
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

data class CompetitionListState(
    val loading: Boolean = true,
    val upcoming: List<Competition> = emptyList(),
    val past: List<Competition> = emptyList(),
    val campaign: CampaignSummary = CampaignSummary(0, 0, 0, 0),
)

@HiltViewModel
class CompetitionListViewModel @Inject constructor(
    competitions: CompetitionRepository,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<CompetitionListState> = combine(competitions.observeCompetitions(), competitions.observeMatches()) { list, matches ->
        val today = LocalDate.now(clock)
        val live = list.filter { it.deletedAt == null }
        CompetitionListState(
            loading = false,
            upcoming = live.filter { it.isUpcoming(today) }.sortedBy { it.date },
            past = live.filter { !it.isUpcoming(today) }.sortedByDescending { it.date },
            campaign = CompetitionStats.campaign(matches),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CompetitionListState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class CompetitionDetailState(
    val loading: Boolean = true,
    val competition: Competition? = null,
    val matches: List<Match> = emptyList(),
    val campaign: CampaignSummary = CampaignSummary(0, 0, 0, 0),
    val submissionsWon: Map<String, Int> = emptyMap(),
    val submissionsSuffered: Map<String, Int> = emptyMap(),
    val weightPlan: WeightCutPlan? = null,
    val currentWeightKg: Double? = null,
    val deleted: Boolean = false,
    @StringRes val message: Int? = null,
)

@HiltViewModel
class CompetitionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val competitions: CompetitionRepository,
    private val health: HealthRepository,
    private val clock: Clock,
) : ViewModel() {

    private val competitionId = savedStateHandle.toRoute<CompetitionDetailRoute>().id
    private val message = MutableStateFlow<Int?>(null)
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<CompetitionDetailState> = combine(
        competitions.observeCompetitions(),
        competitions.observeMatches(),
        health.observeWeights(),
        message,
        deleted,
    ) { list, matchList, weights, note, isDeleted ->
        val competition = list.firstOrNull { it.id == competitionId }
        val own = matchList.filter { it.competitionId == competitionId && it.deletedAt == null }
        val currentKg = weights.filter { it.deletedAt == null }.maxByOrNull { it.date }?.kg
        val target = competition?.targetWeightKg
        val weighIn = competition?.weighInDate
        val plan = if (target != null && currentKg != null && weighIn != null) {
            val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(clock), weighIn)
            if (days >= 1) WeightCut.plan(currentKg, target, days) else null
        } else {
            null
        }
        CompetitionDetailState(
            loading = false,
            competition = competition,
            matches = own.sortedByDescending { it.id },
            campaign = CompetitionStats.campaign(own),
            submissionsWon = CompetitionStats.submissionsWon(own),
            submissionsSuffered = CompetitionStats.submissionsSuffered(own),
            weightPlan = plan,
            currentWeightKg = currentKg,
            deleted = isDeleted,
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), CompetitionDetailState())

    fun toggleChecklist(item: ChecklistItem) {
        val competition = state.value.competition ?: return
        viewModelScope.launch {
            val updated = competition.checklist.map { if (it.id == item.id) it.copy(done = !it.done) else it }
            competitions.saveCompetition(competition.copy(checklist = updated, updatedAt = Instant.now(clock)))
        }
    }

    fun saveMatch(match: Match) = viewModelScope.launch {
        try {
            competitions.saveMatch(match)
            message.update { R.string.match_saved }
        } catch (error: DomainException) {
            message.update { R.string.error_invalid_data }
        }
    }

    fun deleteMatch(id: String) = viewModelScope.launch {
        competitions.deleteMatch(id, Instant.now(clock))
    }

    fun deleteCompetition() = viewModelScope.launch {
        competitions.deleteCompetition(competitionId, Instant.now(clock))
        deleted.update { true }
    }

    fun clearMessage() = message.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

data class CompetitionFormState(
    val isNew: Boolean = true,
    val name: String = "",
    val date: LocalDate? = null,
    val location: String = "",
    val organizer: String = "",
    val beltCategory: String = "",
    val ageCategory: String = "",
    val weightCategory: String = "",
    val targetWeightText: String = "",
    val weighInDate: LocalDate? = null,
    val notes: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    val newItem: String = "",
    val loading: Boolean = true,
    val saved: Boolean = false,
    @StringRes val error: Int? = null,
) {
}

@HiltViewModel
class CompetitionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val competitions: CompetitionRepository,
    private val clock: Clock,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<CompetitionFormRoute>()
    private val _state = MutableStateFlow(CompetitionFormState())
    val state: StateFlow<CompetitionFormState> = _state
    private var existing: Competition? = null

    init {
        viewModelScope.launch {
            if (route.id == NEW) {
                val defaults = listOf(R.string.checklist_kimono, R.string.checklist_documents, R.string.checklist_mouthguard)
                    .map { ChecklistItem(context.getString(it)) }
                _state.update { it.copy(loading = false, date = LocalDate.now(clock), checklist = defaults) }
            } else {
                val found = competitions.observeCompetitions().first().firstOrNull { it.id == route.id }
                existing = found
                _state.value = found?.let {
                    CompetitionFormState(
                        isNew = false,
                        name = it.name,
                        date = it.date,
                        location = it.location.orEmpty(),
                        organizer = it.organizer.orEmpty(),
                        beltCategory = it.beltCategory.orEmpty(),
                        ageCategory = it.ageCategory.orEmpty(),
                        weightCategory = it.weightCategory.orEmpty(),
                        targetWeightText = it.targetWeightKg?.toString().orEmpty(),
                        weighInDate = it.weighInDate,
                        notes = it.notes.orEmpty(),
                        checklist = it.checklist,
                        loading = false,
                    )
                } ?: _state.value.copy(loading = false, error = R.string.error_not_found)
            }
        }
    }

    fun update(transform: (CompetitionFormState) -> CompetitionFormState) = _state.update { transform(it).copy(error = null) }

    fun addChecklistItem() = update { s ->
        val label = s.newItem.trim()
        if (label.isEmpty()) s else s.copy(checklist = s.checklist + ChecklistItem(label), newItem = "")
    }

    fun removeChecklistItem(id: String) = update { s -> s.copy(checklist = s.checklist.filterNot { it.id == id }) }

    fun toggleChecklistItem(id: String) = update { s ->
        s.copy(checklist = s.checklist.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun save() {
        val current = _state.value
        val date = current.date
        if (date == null) {
            _state.update { it.copy(error = R.string.error_date_required) }
            return
        }
        val target = current.targetWeightText.replace(',', '.').trim().ifEmpty { null }?.toDoubleOrNull()
        if (current.targetWeightText.isNotBlank() && target == null) {
            _state.update { it.copy(error = R.string.error_number_format) }
            return
        }
        viewModelScope.launch {
            try {
                val base = existing
                val competition = Competition(
                    name = current.name.trim(),
                    date = date,
                    location = current.location.trim().ifEmpty { null },
                    organizer = current.organizer.trim().ifEmpty { null },
                    beltCategory = current.beltCategory.trim().ifEmpty { null },
                    ageCategory = current.ageCategory.trim().ifEmpty { null },
                    weightCategory = current.weightCategory.trim().ifEmpty { null },
                    targetWeightKg = target,
                    weighInDate = current.weighInDate,
                    finalPlacement = base?.finalPlacement,
                    notes = current.notes.trim().ifEmpty { null },
                    checklist = current.checklist,
                    id = base?.id ?: UUID.randomUUID().toString(),
                    updatedAt = Instant.now(clock),
                    deletedAt = base?.deletedAt,
                )
                competitions.saveCompetition(competition)
                _state.update { it.copy(saved = true) }
            } catch (error: DomainException) {
                _state.update { it.copy(error = R.string.error_invalid_data) }
            } catch (error: IllegalArgumentException) {
                _state.update { it.copy(error = R.string.error_name_required) }
            }
        }
    }
}
