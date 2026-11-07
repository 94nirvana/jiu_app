package com.jiuapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.home.HomeSummary
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.repository.CompetitionRepository
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.domain.repository.ProfileRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.stats.latestGraduation
import com.jiuapp.domain.training.Graduation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val athleteName: String? = null,
    val currentGraduation: Graduation? = null,
    val summary: HomeSummary? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessions: SessionRepository,
    graduations: GraduationRepository,
    competitions: CompetitionRepository,
    health: HealthRepository,
    settings: SettingsRepository,
    profiles: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {

    private val summaryFlow = combine(
        sessions.observeAll(),
        graduations.observeAll(),
        competitions.observeCompetitions(),
        health.observeInjuries(),
        settings.observeSettings(),
    ) { sessionList, graduationList, competitionList, injuryList, appSettings ->
        val today = LocalDate.now(clock)
        Pair(
            HomeSummary.from(sessionList, graduationList, competitionList, injuryList, appSettings.weeklyGoal, today),
            latestGraduation(graduationList),
        )
    }

    val state: StateFlow<HomeUiState> = combine(summaryFlow, profiles.observeProfile()) { (summary, graduation), profile: AthleteProfile? ->
        HomeUiState(
            loading = false,
            athleteName = profile?.name,
            currentGraduation = graduation,
            summary = summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
