package com.tiempo.app.domain.model

enum class ProfileValidationError {
    BIRTH_DATE_MISSING,
    BIRTH_DATE_IN_FUTURE,
    BIRTH_DATE_TOO_OLD,
    REFERENCE_AGE_OUT_OF_RANGE,
    REFERENCE_AGE_NOT_ABOVE_CURRENT_AGE,
    SLEEP_WINDOW_INVALID
}
