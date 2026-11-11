package com.tiempo.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalOccurrence
import com.tiempo.app.domain.model.OccurrenceStatus
import com.tiempo.app.domain.usecase.ArchiveGoalUseCase
import com.tiempo.app.domain.usecase.CompleteGoalUseCase
import com.tiempo.app.domain.usecase.ObserveDayGoalsUseCase
import com.tiempo.app.domain.usecase.SaveGoalUseCase
import com.tiempo.app.domain.usecase.UndoCompletionUseCase
import com.tiempo.app.service.notifications.GoalNotifier
import com.tiempo.app.service.scheduling.GoalAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GoalsUiState(
    val date: LocalDate,
    val occurrences: List<GoalOccurrence> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    observeDayGoals: ObserveDayGoalsUseCase,
    private val completeGoal: CompleteGoalUseCase,
    private val undoCompletion: UndoCompletionUseCase,
    private val saveGoalUseCase: SaveGoalUseCase,
    private val archiveGoalUseCase: ArchiveGoalUseCase,
    private val scheduler: GoalAlarmScheduler,
    private val notifier: GoalNotifier,
    private val clock: Clock
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now(clock))

    val uiState: StateFlow<GoalsUiState> = selectedDate
        .flatMapLatest { date ->
            observeDayGoals(date).map { list ->
                GoalsUiState(date = date, occurrences = list, isLoading = false)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GoalsUiState(date = selectedDate.value)
        )

    fun previousDay() { selectedDate.value = selectedDate.value.minusDays(1) }

    fun nextDay() { selectedDate.value = selectedDate.value.plusDays(1) }

    fun today() { selectedDate.value = LocalDate.now(clock) }

    fun toggle(occurrence: GoalOccurrence) {
        viewModelScope.launch {
            if (occurrence.status == OccurrenceStatus.DONE) {
                undoCompletion(occurrence.goal.id, occurrence.date)
            } else {
                completeGoal(occurrence.goal.id, occurrence.date)
                notifier.cancel(occurrence.goal.id)   // quita el aviso si seguía visible
            }
        }
    }

    fun save(goal: Goal) {
        viewModelScope.launch {
            val id = saveGoalUseCase(goal)
            scheduler.schedule(goal.copy(id = id))
        }
    }

    /** Oculta la meta, cancela sus avisos y conserva su historial. */
    fun archive(goalId: Long) {
        viewModelScope.launch {
            scheduler.cancel(goalId)
            notifier.cancel(goalId)
            archiveGoalUseCase(goalId)
        }
    }
}