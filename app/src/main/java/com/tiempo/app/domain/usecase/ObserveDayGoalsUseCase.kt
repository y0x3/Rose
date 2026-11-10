package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.GoalOccurrence
import com.tiempo.app.domain.model.OccurrenceStatus
import com.tiempo.app.domain.model.occursOn
import com.tiempo.app.domain.repository.GoalRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveDayGoalsUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<GoalOccurrence>> =
        combine(
            repository.observeGoals(),
            repository.observeCompletions(date, date)
        ) { goals, completions ->
            val byGoal = completions.associateBy { it.goalId }
            goals
                .filter { it.occursOn(date) }
                .sortedBy { it.time }
                .map { goal ->
                    val status = when (byGoal[goal.id]?.status) {
                        CompletionStatus.DONE -> OccurrenceStatus.DONE
                        CompletionStatus.SKIPPED -> OccurrenceStatus.SKIPPED
                        null -> OccurrenceStatus.PENDING
                    }
                    GoalOccurrence(goal, date, status)
                }
        }
}