package com.tiempo.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class GoalCompletion(
    val id: Long = 0,
    val goalId: Long,
    val date: LocalDate,
    val status: CompletionStatus,
    val completedAt: Instant
)