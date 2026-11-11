package com.tiempo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tiempo.app.domain.model.AlertType
import com.tiempo.app.domain.model.CompletionStatus
import com.tiempo.app.domain.model.FrequencyType

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String?,
    val color: Int,
    val frequency: FrequencyType,
    val daysOfWeekMask: Int,
    val dayOfMonth: Int?,
    val interval: Int,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val timeMinutes: Int,
    val alertType: AlertType,
    val soundUri: String?,
    val archived: Boolean
)

@Entity(
    tableName = "goal_completions",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId", "dateEpochDay"], unique = true)]
)
data class GoalCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val dateEpochDay: Long,
    val status: CompletionStatus,
    val completedAtMillis: Long,
    @ColumnInfo(defaultValue = "''") val goalTitle: String = "",
    @ColumnInfo(defaultValue = "0") val goalColor: Int = 0
)