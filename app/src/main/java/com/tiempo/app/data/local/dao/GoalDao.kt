package com.tiempo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.tiempo.app.data.local.entity.GoalCompletionEntity
import com.tiempo.app.data.local.entity.GoalEntity

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE archived = 0")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoal(id: Long): GoalEntity?

    @Upsert
    suspend fun upsertGoal(goal: GoalEntity): Long

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM goal_completions WHERE dateEpochDay BETWEEN :from AND :to")
    fun observeCompletions(from: Long, to: Long): Flow<List<GoalCompletionEntity>>

    // Seguro con REPLACE: el índice único (goalId, fecha) evita duplicados
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: GoalCompletionEntity)

    @Query("DELETE FROM goal_completions WHERE goalId = :goalId AND dateEpochDay = :date")
    suspend fun deleteCompletion(goalId: Long, date: Long)
}