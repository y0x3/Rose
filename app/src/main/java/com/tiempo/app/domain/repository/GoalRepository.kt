package com.tiempo.app.domain.repository

import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalCompletion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>

    fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<GoalCompletion>>

    suspend fun getGoal(id: Long): Goal?

    /** Devuelve el id de la meta (nuevo o existente). */
    suspend fun saveGoal(goal: Goal): Long

    suspend fun deleteGoal(id: Long)

    suspend fun setCompletion(goalId: Long, date: LocalDate, status: CompletionStatus)

    /** Quita el cumplimiento (sirve para "Deshacer"). */
    suspend fun clearCompletion(goalId: Long, date: LocalDate)
}