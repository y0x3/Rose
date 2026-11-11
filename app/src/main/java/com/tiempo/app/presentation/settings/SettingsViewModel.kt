package com.tiempo.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.usecase.ObserveUserProfileUseCase
import com.tiempo.app.service.assistant.AssistantMode
import com.tiempo.app.service.assistant.AssistantSettings
import com.tiempo.app.service.assistant.DeviceCapabilities
import com.tiempo.app.service.assistant.DeviceCapabilityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val assistantMode: AssistantMode = AssistantMode.KEYWORDS,
    val device: DeviceCapabilities? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase,
    private val assistantSettings: AssistantSettings,
    private val capabilityChecker: DeviceCapabilityChecker
) : ViewModel() {

    private val device = MutableStateFlow<DeviceCapabilities?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeUserProfile(),
        assistantSettings.mode,
        device
    ) { profile, mode, caps ->
        SettingsUiState(
            profile = profile,
            isLoading = false,
            assistantMode = mode,
            device = caps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setAssistantMode(mode: AssistantMode) {
        // Local y nube todavía no existen: solo se acepta palabras clave
        if (mode == AssistantMode.KEYWORDS) assistantSettings.setMode(mode)
    }

    /** Mide la RAM y el espacio ahora mismo. */
    fun checkDevice() {
        device.value = capabilityChecker.check()
    }
}