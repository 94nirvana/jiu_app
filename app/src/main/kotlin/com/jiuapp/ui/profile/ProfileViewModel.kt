package com.jiuapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.achievement.AchievementId
import com.jiuapp.domain.achievement.Achievements
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.repository.AchievementRepository
import com.jiuapp.domain.repository.CompetitionRepository
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.ProfileRepository
import com.jiuapp.domain.repository.SessionRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.stats.Progression
import com.jiuapp.domain.stats.TimelineEntry
import com.jiuapp.domain.stats.daysSinceGraduation
import com.jiuapp.domain.stats.latestGraduation
import com.jiuapp.domain.stats.sessionsSince
import com.jiuapp.domain.stats.toTotals
import com.jiuapp.domain.stats.Totals
import com.jiuapp.domain.training.Graduation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loading: Boolean = true,
    val profile: AthleteProfile? = null,
    val currentGraduation: Graduation? = null,
    val graduations: List<TimelineEntry> = emptyList(),
    val sinceGraduation: Totals = Totals.EMPTY,
    val daysSinceGraduation: Long? = null,
    val estimatedNextGraduation: LocalDate? = null,
    val achievements: Map<AchievementId, LocalDate> = emptyMap(),
    val message: Int? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val graduations: GraduationRepository,
    private val sessions: SessionRepository,
    private val competitions: CompetitionRepository,
    private val achievements: AchievementRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val message = MutableStateFlow<Int?>(null)

    val state: StateFlow<ProfileUiState> = combine(
        profiles.observeProfile(),
        graduations.observeAll(),
        sessions.observeAll(),
        achievements.observeUnlocked(),
        message,
    ) { profile, graduationList, sessionList, unlocked, note ->
        val today = LocalDate.now(clock)
        val last = latestGraduation(graduationList)
        val live = sessionList.filter { it.deletedAt == null }
        ProfileUiState(
            loading = false,
            profile = profile,
            currentGraduation = last,
            graduations = Progression.timeline(graduationList).sortedByDescending { it.date },
            sinceGraduation = sessionsSince(live, last).toTotals(),
            daysSinceGraduation = daysSinceGraduation(last, today),
            estimatedNextGraduation = Progression.estimateNextGraduation(graduationList),
            achievements = unlocked,
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState())

    init {
        // Registra conquistas novas com a data de hoje. Datas já registradas nunca mudam.
        viewModelScope.launch {
            combine(sessions.observeAll(), graduations.observeAll(), competitions.observeMatches(), settings.observeSettings()) { s, g, m, cfg ->
                Achievements.satisfied(s, g, m, cfg.weeklyGoal, LocalDate.now(clock))
            }.collect { ids -> achievements.recordUnlocks(ids, LocalDate.now(clock)) }
        }
    }

    fun saveProfile(profile: AthleteProfile) {
        viewModelScope.launch {
            try {
                profiles.saveProfile(profile.copy(updatedAt = Instant.now(clock)))
                message.update { R.string.profile_saved }
            } catch (error: DomainException) {
                message.update { R.string.error_invalid_data }
            } catch (error: IllegalArgumentException) {
                message.update { R.string.error_name_required }
            }
        }
    }

    /** Adiciona um registro de graduação. Registros existentes não são alterados. */
    fun addGraduation(belt: Belt, degree: Int, date: LocalDate, professor: String?, note: String?) {
        viewModelScope.launch {
            try {
                graduations.save(
                    Graduation(
                        rank = Rank(belt, degree),
                        date = date,
                        professor = professor?.trim()?.ifEmpty { null },
                        note = note?.trim()?.ifEmpty { null },
                        updatedAt = Instant.now(clock),
                    ),
                )
                message.update { R.string.graduation_saved }
            } catch (error: DomainException) {
                message.update { R.string.error_invalid_data }
            }
        }
    }

    fun clearMessage() = message.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
