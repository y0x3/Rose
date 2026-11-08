package com.tiempo.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Single user profile. The app works with one local profile, so there is no id
 * in the domain model.
 */
data class UserProfile(
    val birthDate: LocalDate,
    val referenceAgeYears: Int = UserProfileDefaults.DEFAULT_REFERENCE_AGE_YEARS,
    val sleepTime: LocalTime = UserProfileDefaults.DEFAULT_SLEEP_TIME,
    val wakeTime: LocalTime = UserProfileDefaults.DEFAULT_WAKE_TIME
)

object UserProfileDefaults {
    const val REFERENCE_AGE_MIN_YEARS = 50
    const val REFERENCE_AGE_MAX_YEARS = 120
    const val DEFAULT_REFERENCE_AGE_YEARS = 80

    val MIN_BIRTH_DATE: LocalDate = LocalDate.of(1900, 1, 1)
    val DEFAULT_SLEEP_TIME: LocalTime = LocalTime.of(23, 0)
    val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(7, 0)
}
