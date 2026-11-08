package com.tiempo.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.model.ProfileValidationError
import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.model.UserProfileDefaults
import com.tiempo.app.domain.usecase.SaveProfileOutcome
import com.tiempo.app.domain.usecase.SaveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OnboardingError {
    data class Validation(val error: ProfileValidationError) : OnboardingError
    data object Unknown : OnboardingError
}

data class OnboardingUiState(
    val birthDate: LocalDate? = null,
    val referenceAgeYears: Int = UserProfileDefaults.DEFAULT_REFERENCE_AGE_YEARS,
    val isSaving: Boolean = false,
    val error: OnboardingError? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val saveUserProfile: SaveUserProfileUseCase,
    clock: Clock
) : ViewModel() {

    val today: LocalDate = LocalDate.now(clock)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onBirthDateSelected(date: LocalDate) {
        _uiState.update { it.copy(birthDate = date, error = null) }
    }

    fun onReferenceAgeChanged(age: Int) {
        _uiState.update {
            it.copy(
                referenceAgeYears = age.coerceIn(
                    UserProfileDefaults.REFERENCE_AGE_MIN_YEARS,
                    UserProfileDefaults.REFERENCE_AGE_MAX_YEARS
                ),
                error = null
            )
        }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current.isSaving) return

        val birthDate = current.birthDate
        if (birthDate == null) {
            _uiState.update {
                it.copy(
                    error = OnboardingError.Validation(ProfileValidationError.BIRTH_DATE_MISSING)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val outcome = saveUserProfile(
                    UserProfile(
                        birthDate = birthDate,
                        referenceAgeYears = current.referenceAgeYears
                    )
                )
                _uiState.update { state ->
                    when (outcome) {
                        SaveProfileOutcome.Success -> state.copy(isSaving = false, error = null)
                        is SaveProfileOutcome.Invalid -> state.copy(
                            isSaving = false,
                            error = OnboardingError.Validation(outcome.error)
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, error = OnboardingError.Unknown)
                }
            }
        }
    }
}
