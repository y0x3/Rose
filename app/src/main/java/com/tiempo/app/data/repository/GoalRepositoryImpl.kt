package com.tiempo.app.data.repository

import com.tiempo.app.data.local.dao.GoalDao
import com.tiempo.app.data.local.entity.GoalCompletionEntity
import com.tiempo.app.data.mapper.toDomain
import com.tiempo.app.data.mapper.toEntity
import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalCompletion
import com.tiempo.app.domain.repository.GoalRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao,
    private val clock: Clock
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> =
        dao.observeGoals().map { list -> list.map { it.toDomain() } }

    override fun observeAllGoals(): Flow<List<Goal>> =
        dao.observeAllGoals().map { list -> list.map { it.toDomain() } }

    override fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<GoalCompletion>> =
        dao.observeCompletions(from.toEpochDay(), to.toEpochDay())
            .map { list -> list.map { it.toDomain() } }

    override fun observeAllCompletions(): Flow<List<GoalCompletion>> =
        dao.observeAllCompletions().map { list -> list.map { it.toDomain() } }

    override suspend fun getGoal(id: Long): Goal? = dao.getGoal(id)?.toDomain()

    override suspend fun saveGoal(goal: Goal): Long {
        val rowId = dao.upsertGoal(goal.toEntity())
        // En una actualización, @Upsert no devuelve el id: usamos el que ya teníamos
        return if (goal.id != 0L) goal.id else rowId
    }

    override suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)

    override suspend fun archiveGoal(id: Long) {
        val goal = dao.getGoal(id) ?: return
        // Termina "ayer": el historial no la marcará como no cumplida en días posteriores
        val yesterday = LocalDate.now(clock).minusDays(1).toEpochDay()
        val end = goal.endEpochDay?.let { minOf(it, yesterday) } ?: yesterday
        dao.upsertGoal(goal.copy(archived = true, endEpochDay = end))
    }

    override suspend fun setCompletion(goalId: Long, date: LocalDate, status: CompletionStatus) {
        // Guardamos el título y color de hoy para que el historial se vea como fue
        val goal = dao.getGoal(goalId)
        dao.upsertCompletion(
            GoalCompletionEntity(
                goalId = goalId,
                dateEpochDay = date.toEpochDay(),
                status = status,
                completedAtMillis = clock.millis(),
                goalTitle = goal?.title.orEmpty(),
                goalColor = goal?.color ?: 0
            )
        )
    }

    override suspend fun clearCompletion(goalId: Long, date: LocalDate) =
        dao.deleteCompletion(goalId, date.toEpochDay())
}