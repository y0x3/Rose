package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.GoalRepository
import java.time.LocalDate
import javax.inject.Inject

class UndoCompletionUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goalId: Long, date: LocalDate) =
        repository.clearCompletion(goalId, date)
}