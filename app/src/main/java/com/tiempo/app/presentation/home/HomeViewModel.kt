package com.tiempo.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeUserProfile: ObserveUserProfileUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeUserProfile()
        .map { profile -> HomeUiState(profile = profile, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
