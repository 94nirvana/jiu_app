package com.jiuapp.ui.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.R
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.belt.Belt
import com.jiuapp.domain.belt.Rank
import com.jiuapp.domain.profile.AthleteProfile
import com.jiuapp.domain.repository.GraduationRepository
import com.jiuapp.domain.repository.ProfileRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.settings.AppSettings
import com.jiuapp.domain.training.Graduation
import com.jiuapp.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val step: Int = 0,
    val consent: Boolean = false,
    val name: String = "",
    val academy: String = "",
    val professor: String = "",
    val startDate: LocalDate? = null,
    val belt: Belt = Belt.WHITE,
    val degree: Int = 0,
    val graduationDate: LocalDate? = null,
    val weeklyGoal: Int = AppSettings.DEFAULT_WEEKLY_GOAL,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(19, 0),
    val reminderDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
    val saving: Boolean = false,
    @StringRes val error: Int? = null,
) {
    val lastStep: Int get() = LAST_STEP

    /** Valida o passo atual antes de avançar. Retorna a mensagem de erro, ou null se estiver ok. */
    @StringRes
    fun stepError(): Int? = when (step) {
        0 -> if (consent) null else R.string.error_consent_required
        1 -> when {
            name.isBlank() -> R.string.error_name_required
            name.length > AthleteProfile.MAX_NAME_LENGTH -> R.string.error_name_too_long
            startDate == null -> R.string.error_start_date_required
            startDate.isAfter(LocalDate.now()) -> R.string.error_date_in_future
            else -> null
        }
        2 -> when {
            graduationDate == null -> R.string.error_graduation_date_required
            graduationDate.isAfter(LocalDate.now()) -> R.string.error_date_in_future
            else -> null
        }
        else -> null
    }

    companion object {
        const val LAST_STEP = 3
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val graduations: GraduationRepository,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    fun update(transform: (OnboardingState) -> OnboardingState) = _state.update { transform(it).copy(error = null) }

    fun next() {
        val current = _state.value
        current.stepError()?.let { error -> _state.update { it.copy(error = error) }; return }
        if (current.step < current.lastStep) _state.update { it.copy(step = it.step + 1) } else finish()
    }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = null) }

    private fun finish() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val now = Instant.now(clock)
                profiles.saveProfile(
                    AthleteProfile(
                        name = current.name.trim(),
                        startDate = current.startDate!!,
                        academy = current.academy.trim().ifEmpty { null },
                        professor = current.professor.trim().ifEmpty { null },
                        updatedAt = now,
                    ),
                )
                graduations.save(
                    Graduation(
                        rank = Rank(current.belt, current.degree),
                        date = current.graduationDate!!,
                        professor = current.professor.trim().ifEmpty { null },
                        note = null,
                        updatedAt = now,
                    ),
                )
                settings.update {
                    it.copy(
                        weeklyGoal = current.weeklyGoal,
                        reminderEnabled = current.reminderEnabled,
                        reminderTime = current.reminderTime,
                        reminderDays = current.reminderDays,
                        privacyConsentDate = LocalDate.now(clock),
                        onboardingDone = true,
                    )
                }
                reminders.reschedule(settings.observeSettings().first())
            } catch (error: DomainException) {
                _state.update { it.copy(saving = false, error = R.string.error_invalid_data) }
            }
        }
    }
}
