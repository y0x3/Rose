package com.tiempo.app.service.alarm

import kotlinx.coroutines.flow.MutableStateFlow

data class AlarmInfo(
    val goalId: Long,
    val title: String,
    val note: String?,
    val epochDay: Long
)

object AlarmState {
    /** null = no hay ninguna alarma sonando. */
    val current = MutableStateFlow<AlarmInfo?>(null)
}