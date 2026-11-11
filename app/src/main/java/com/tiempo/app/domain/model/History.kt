package com.tiempo.app.domain.model

import java.time.Instant
import java.time.LocalDate

enum class HistoryStatus { DONE, SKIPPED, MISSED, PENDING }

data class HistoryActivity(
    val goalId: Long,
    val title: String,
    val color: Int,
    val status: HistoryStatus,
    val completedAt: Instant?
)

data class DayHistory(
    val date: LocalDate,
    val activities: List<HistoryActivity>,
    val notes: List<JournalNote>
) {
    val doneCount: Int get() = activities.count { it.status == HistoryStatus.DONE }

    /** Las omitidas a propósito no cuentan en contra. */
    val totalCount: Int get() = activities.count { it.status != HistoryStatus.SKIPPED }
}