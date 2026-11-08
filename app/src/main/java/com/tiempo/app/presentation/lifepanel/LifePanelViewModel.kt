package com.tiempo.app.presentation.lifepanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.data.preferences.ScalePreference
import com.tiempo.app.domain.lifepanel.LifeCalculator
import com.tiempo.app.domain.lifepanel.ScaleSnapshot
import com.tiempo.app.domain.lifepanel.TimeScale
import com.tiempo.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

data class LifePanelUiState(
    val isLoading: Boolean = true,
    val scale: TimeScale = TimeScale.WEEKS,
    val snapshot: ScaleSnapshot? = null
)

@HiltViewModel
class LifePanelViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val scalePreference: ScalePreference,
    clock: Clock
) : ViewModel() {

    private val calculator = LifeCalculator(clock)

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    val uiState: StateFlow<LifePanelUiState> = combine(
        profileRepository.observeProfile(),
        scalePreference.scale,
        ticker
    ) { profile, scale, _ ->
        if (profile == null) {
            LifePanelUiState(isLoading = false, scale = scale)
        } else {
            LifePanelUiState(
                isLoading = false,
                scale = scale,
                snapshot = calculator.compute(profile.birthDate, profile.referenceAgeYears, scale)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LifePanelUiState())

    fun onScaleSelected(scale: TimeScale) {
        viewModelScope.launch { scalePreference.setScale(scale) }
    }
}