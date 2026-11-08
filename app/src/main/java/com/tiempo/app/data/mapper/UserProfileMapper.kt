package com.tiempo.app.data.mapper

import com.tiempo.app.data.local.entity.UserProfileEntity
import com.tiempo.app.domain.model.UserProfile
import java.time.LocalDate
import java.time.LocalTime

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    birthDate = LocalDate.ofEpochDay(birthDateEpochDay),
    referenceAgeYears = referenceAgeYears,
    sleepTime = sleepMinuteOfDay.toLocalTime(),
    wakeTime = wakeMinuteOfDay.toLocalTime()
)

fun UserProfile.toEntity(updatedAtEpochMillis: Long): UserProfileEntity = UserProfileEntity(
    birthDateEpochDay = birthDate.toEpochDay(),
    referenceAgeYears = referenceAgeYears,
    sleepMinuteOfDay = sleepTime.toMinuteOfDay(),
    wakeMinuteOfDay = wakeTime.toMinuteOfDay(),
    updatedAtEpochMillis = updatedAtEpochMillis
)

private fun Int.toLocalTime(): LocalTime = LocalTime.of(this / 60, this % 60)

private fun LocalTime.toMinuteOfDay(): Int = hour * 60 + minute
