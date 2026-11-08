package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.ProfileValidationError
import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.model.UserProfileDefaults
import com.tiempo.app.domain.repository.UserProfileRepository
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

sealed interface SaveProfileOutcome {
    data object Success : SaveProfileOutcome
    data class Invalid(val error: ProfileValidationError) : SaveProfileOutcome
}

/**
 * Validates the profile before writing it. The assistant (phase 6) will go
 * through this same use case, so the validation rules live in one place.
 */
class SaveUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(profile: UserProfile): SaveProfileOutcome {
        val error = validate(profile) ?: run {
            repository.saveProfile(profile)
            return SaveProfileOutcome.Success
        }
        return SaveProfileOutcome.Invalid(error)
    }

    private fun validate(profile: UserProfile): ProfileValidationError? {
        val today = LocalDate.now(clock)
        if (profile.birthDate.isAfter(today)) {
            return ProfileValidationError.BIRTH_DATE_IN_FUTURE
        }
        if (profile.birthDate.isBefore(UserProfileDefaults.MIN_BIRTH_DATE)) {
            return ProfileValidationError.BIRTH_DATE_TOO_OLD
        }
        if (profile.referenceAgeYears !in
            UserProfileDefaults.REFERENCE_AGE_MIN_YEARS..UserProfileDefaults.REFERENCE_AGE_MAX_YEARS
        ) {
            return ProfileValidationError.REFERENCE_AGE_OUT_OF_RANGE
        }
        val currentAge = Period.between(profile.birthDate, today).years
        if (profile.referenceAgeYears <= currentAge) {
            return ProfileValidationError.REFERENCE_AGE_NOT_ABOVE_CURRENT_AGE
        }
        if (profile.sleepTime == profile.wakeTime) {
            return ProfileValidationError.SLEEP_WINDOW_INVALID
        }
        return null
    }
}
