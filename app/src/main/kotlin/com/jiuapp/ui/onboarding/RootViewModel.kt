package com.jiuapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiuapp.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** `null` enquanto as configurações não foram lidas: evita piscar o onboarding para quem já concluiu. */
@HiltViewModel
class RootViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> = settings.observeSettings()
        .map<_, Boolean?> { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
