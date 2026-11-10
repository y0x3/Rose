package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.GoalRepository
import javax.inject.Inject

class DeleteGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteGoal(id)
}