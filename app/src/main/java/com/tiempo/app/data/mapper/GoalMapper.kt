package com.tiempo.app.data.mapper

import com.tiempo.app.data.local.entity.GoalCompletionEntity
import com.tiempo.app.data.local.entity.GoalEntity
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalCompletion
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

private fun Set<DayOfWeek>.toMask(): Int =
    fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

private fun Int.toDays(): Set<DayOfWeek> =
    DayOfWeek.values().filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

fun GoalEntity.toDomain() = Goal(
    id = id, title = title, note = note, color = color,
    frequency = frequency, daysOfWeek = daysOfWeekMask.toDays(),
    dayOfMonth = dayOfMonth, interval = interval,
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = endEpochDay?.let { LocalDate.ofEpochDay(it) },
    time = LocalTime.of(timeMinutes / 60, timeMinutes % 60),
    alertType = alertType, soundUri = soundUri, archived = archived
)

fun Goal.toEntity() = GoalEntity(
    id = id, title = title, note = note, color = color,
    frequency = frequency, daysOfWeekMask = daysOfWeek.toMask(),
    dayOfMonth = dayOfMonth, interval = interval,
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate?.toEpochDay(),
    timeMinutes = time.hour * 60 + time.minute,
    alertType = alertType, soundUri = soundUri, archived = archived
)

fun GoalCompletionEntity.toDomain() = GoalCompletion(
    id = id, goalId = goalId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    status = status,
    completedAt = Instant.ofEpochMilli(completedAtMillis),
    goalTitle = goalTitle,
    goalColor = goalColor
)

fun GoalCompletion.toEntity() = GoalCompletionEntity(
    id = id, goalId = goalId,
    dateEpochDay = date.toEpochDay(),
    status = status,
    completedAtMillis = completedAt.toEpochMilli(),
    goalTitle = goalTitle,
    goalColor = goalColor
)