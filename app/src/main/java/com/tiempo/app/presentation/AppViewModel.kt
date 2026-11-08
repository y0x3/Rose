package com.tiempo.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppUiState {
    data object Loading : AppUiState
    data object NeedsOnboarding : AppUiState
    data object Ready : AppUiState
}

/**
 * Decides the app shell: onboarding or the main experience. Once a profile
 * exists in Room, onboarding never appears again.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase
) : ViewModel() {

    val uiState: StateFlow<AppUiState> = observeUserProfile()
        .map { profile ->
            if (profile == null) AppUiState.NeedsOnboarding else AppUiState.Ready
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState.Loading
        )
}
