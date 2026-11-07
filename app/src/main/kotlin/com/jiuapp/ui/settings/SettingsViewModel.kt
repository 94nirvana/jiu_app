package com.jiuapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.DomainException
import com.jiuapp.domain.repository.SettingsRepository
import com.jiuapp.domain.settings.AppSettings
import com.jiuapp.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jiuapp.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
) : ViewModel() {

    private val error = MutableStateFlow<Int?>(null)

    val state: StateFlow<AppSettings> = settings.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppSettings())

    val message: StateFlow<Int?> = error

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                settings.update(transform)
                reminders.reschedule(settings.observeSettings().first())
            } catch (exception: DomainException) {
                error.update { R.string.error_invalid_settings }
            }
        }
    }

    fun clearMessage() = error.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
