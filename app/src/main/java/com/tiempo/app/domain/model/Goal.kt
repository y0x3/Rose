package com.tiempo.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class FrequencyType { DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY, EVERY_N_DAYS }

enum class AlertType { SIMPLE, MESSAGE, ALARM }

data class Goal(
    val id: Long = 0,
    val title: String,
    val note: String? = null,
    val color: Int = 0xFF4CAF50.toInt(),
    val frequency: FrequencyType = FrequencyType.DAILY,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),   // WEEKDAYS y WEEKLY
    val dayOfMonth: Int? = null,                   // MONTHLY
    val interval: Int = 1,                         // EVERY_N_DAYS
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val time: LocalTime,
    val alertType: AlertType = AlertType.MESSAGE,
    val soundUri: String? = null,
    val archived: Boolean = false
)

/** ¿Esta meta toca en [date]? Las metas archivadas nunca tocan (se usa en Goals y avisos). */
fun Goal.occursOn(date: LocalDate): Boolean = !archived && scheduledOn(date)

/**
 * ¿Según su calendario, esta meta tocaba en [date]? Ignora si está archivada.
 * Lo usa el historial para mostrar días pasados de metas que ya se archivaron.
 */
fun Goal.scheduledOn(date: LocalDate): Boolean {
    if (date < startDate) return false
    if (endDate != null && date > endDate) return false

    return when (frequency) {
        FrequencyType.DAILY -> true

        FrequencyType.WEEKDAYS,
        FrequencyType.WEEKLY -> date.dayOfWeek in daysOfWeek

        // Día 31 cae el 28/29/30 en meses cortos en vez de saltarse el mes
        FrequencyType.MONTHLY ->
            date.dayOfMonth == minOf(dayOfMonth ?: startDate.dayOfMonth, date.lengthOfMonth())

        // Un 29 de febrero cae el 28 en años no bisiestos
        FrequencyType.YEARLY -> {
            val target = minOf(startDate.dayOfMonth, date.month.length(date.isLeapYear))
            date.month == startDate.month && date.dayOfMonth == target
        }

        FrequencyType.EVERY_N_DAYS ->
            ChronoUnit.DAYS.between(startDate, date) % interval.coerceAtLeast(1) == 0L
    }
}