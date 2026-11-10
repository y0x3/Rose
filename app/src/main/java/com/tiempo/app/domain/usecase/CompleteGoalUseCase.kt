package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.repository.GoalRepository
import java.time.LocalDate
import javax.inject.Inject

class CompleteGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goalId: Long, date: LocalDate) =
        repository.setCompletion(goalId, date, CompletionStatus.DONE)
}