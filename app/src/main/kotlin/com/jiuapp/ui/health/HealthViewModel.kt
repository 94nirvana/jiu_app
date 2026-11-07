package com.jiuapp.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.health.BodyRegion
import com.jiuapp.domain.health.Injury
import com.jiuapp.domain.health.WeightAlert
import com.jiuapp.domain.health.WeightAnalysis
import com.jiuapp.domain.health.WeightEntry
import com.jiuapp.domain.health.activeInjuries
import com.jiuapp.domain.health.recurringRegions
import com.jiuapp.domain.repository.HealthRepository
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.settings.AppSettings
import androidx.annotation.StringRes
import com.jiuapp.R
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthUiState(
    val loading: Boolean = true,
    val injuries: List<Injury> = emptyList(),
    val active: List<Injury> = emptyList(),
    val recurring: Set<BodyRegion> = emptySet(),
    val weights: List<WeightEntry> = emptyList(),
    val weeklyChangePercent: Double? = null,
    val rapidLoss: WeightAlert? = null,
    val targetWeightKg: Double? = null,
    val remainingKg: Double? = null,
    @StringRes val message: Int? = null,
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val health: HealthRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val message = MutableStateFlow<Int?>(null)

    val state: StateFlow<HealthUiState> = combine(
        health.observeInjuries(),
        health.observeWeights(),
        settings.observeSettings(),
        message,
    ) { injuries, weights, appSettings: AppSettings, note ->
        HealthUiState(
            loading = false,
            injuries = injuries,
            active = activeInjuries(injuries),
            recurring = recurringRegions(injuries),
            weights = weights.sortedBy { it.date },
            weeklyChangePercent = WeightAnalysis.weeklyChangePercent(weights),
            rapidLoss = WeightAnalysis.rapidLossAlert(weights),
            targetWeightKg = appSettings.targetWeightKg,
            remainingKg = appSettings.targetWeightKg?.let { WeightAnalysis.remainingToTarget(weights, it) },
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HealthUiState())

    fun saveInjury(injury: Injury) = runAndNotify(R.string.injury_saved) { health.saveInjury(injury) }

    fun deleteInjury(id: String) = viewModelScope.launch { health.deleteInjury(id, Instant.now(clock)) }

    fun saveWeight(date: LocalDate, kg: Double) = runAndNotify(R.string.weight_saved) {
        health.saveWeight(WeightEntry(date = date, kg = kg, id = UUID.randomUUID().toString(), updatedAt = Instant.now(clock)))
    }

    fun deleteWeight(id: String) = viewModelScope.launch { health.deleteWeight(id, Instant.now(clock)) }

    fun saveTargetWeight(kg: Double?) = viewModelScope.launch {
        try {
            settings.update { it.copy(targetWeightKg = kg) }
            message.update { R.string.target_saved }
        } catch (error: DomainException) {
            message.update { R.string.error_invalid_data }
        }
    }

    fun clearMessage() = message.update { null }

    private fun runAndNotify(success: Int, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
            message.update { success }
        } catch (error: DomainException) {
            message.update { R.string.error_invalid_data }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
