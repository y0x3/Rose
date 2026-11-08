package com.tiempo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * There is only one profile in the app, always stored with id = 1.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = SINGLE_PROFILE_ID,
    @ColumnInfo(name = "birth_date_epoch_day")
    val birthDateEpochDay: Long,
    @ColumnInfo(name = "reference_age_years")
    val referenceAgeYears: Int,
    @ColumnInfo(name = "sleep_minute_of_day")
    val sleepMinuteOfDay: Int,
    @ColumnInfo(name = "wake_minute_of_day")
    val wakeMinuteOfDay: Int,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}
