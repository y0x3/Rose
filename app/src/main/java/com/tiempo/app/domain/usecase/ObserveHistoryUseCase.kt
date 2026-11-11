package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.DayHistory
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalCompletion
import com.tiempo.app.domain.model.HistoryActivity
import com.tiempo.app.domain.model.HistoryStatus
import com.tiempo.app.domain.model.JournalNote
import com.tiempo.app.domain.model.scheduledOn
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.domain.repository.JournalRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHistoryUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val journalRepository: JournalRepository,
    private val clock: Clock
) {
    operator fun invoke(): Flow<List<DayHistory>> =
        combine(
            goalRepository.observeAllGoals(),
            goalRepository.observeAllCompletions(),
            journalRepository.observeAllNotes()
        ) { goals, completions, notes -> build(goals, completions, notes) }

    private fun build(
        goals: List<Goal>,
        completions: List<GoalCompletion>,
        notes: List<JournalNote>
    ): List<DayHistory> {
        val today = LocalDate.now(clock)
        val completionsByDay = completions.groupBy { it.date }
        val notesByDay = notes.groupBy { it.date }
        val goalsById = goals.associateBy { it.id }

        val first = listOfNotNull(
            goals.minOfOrNull { it.startDate },
            completionsByDay.keys.minOrNull(),
            notesByDay.keys.minOrNull()
        ).minOrNull() ?: return emptyList()

        return generateSequence(today) { it.minusDays(1) }
            .takeWhile { !it.isBefore(first) }
            .mapNotNull { date ->
                val dayCompletions = completionsByDay[date].orEmpty()

                val handled = dayCompletions.map { c ->
                    val goal = goalsById[c.goalId]
                    HistoryActivity(
                        goalId = c.goalId,
                        title = c.goalTitle.ifBlank { goal?.title.orEmpty() },
                        color = if (c.goalColor != 0) c.goalColor else goal?.color ?: 0,
                        status = if (c.status == CompletionStatus.DONE) HistoryStatus.DONE
                        else HistoryStatus.SKIPPED,
                        completedAt = c.completedAt
                    )
                }.sortedBy { it.completedAt }

                val handledIds = dayCompletions.map { it.goalId }.toSet()
                val open = goals
                    .filter { it.id !in handledIds && it.scheduledOn(date) }
                    .sortedBy { it.time }
                    .map { g ->
                        HistoryActivity(
                            goalId = g.id,
                            title = g.title,
                            color = g.color,
                            status = if (date.isBefore(today)) HistoryStatus.MISSED
                            else HistoryStatus.PENDING,
                            completedAt = null
                        )
                    }

                val activities = handled + open
                val dayNotes = notesByDay[date].orEmpty()
                if (activities.isEmpty() && dayNotes.isEmpty()) null
                else DayHistory(date, activities, dayNotes)
            }
            .toList()
    }
}