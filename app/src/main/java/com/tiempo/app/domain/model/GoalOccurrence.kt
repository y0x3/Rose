package com.tiempo.app.domain.model

import java.time.LocalDate

enum class OccurrenceStatus { PENDING, DONE, SKIPPED }

data class GoalOccurrence(
    val goal: Goal,
    val date: LocalDate,
    val status: OccurrenceStatus
)