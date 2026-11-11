package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.GoalRepository
import javax.inject.Inject

class ArchiveGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(id: Long) = repository.archiveGoal(id)
}