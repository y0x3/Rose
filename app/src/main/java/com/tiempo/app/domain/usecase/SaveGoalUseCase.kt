package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.repository.GoalRepository
import javax.inject.Inject

class SaveGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal): Long = repository.saveGoal(goal)
}