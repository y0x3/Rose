package com.tiempo.app.domain.lifepanel

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class LifeCalculator(private val clock: Clock) {

    companion object {
        const val WEEKS_PER_ROW = 52
        const val MIN_REFERENCE_AGE = 50
        const val MAX_REFERENCE_AGE = 120
    }

    fun compute(birthDate: LocalDate, referenceAge: Int, scale: TimeScale): ScaleSnapshot {
        val age = referenceAge.coerceIn(MIN_REFERENCE_AGE, MAX_REFERENCE_AGE)
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val safeBirth = if (birthDate.isAfter(today)) today else birthDate

        val yearsLived = yearsLived(safeBirth, today)
        val beyond = yearsLived >= age

        return when (scale) {
            TimeScale.YEARS -> years(age, yearsLived, beyond)
            TimeScale.MONTHS -> months(safeBirth, today, age, beyond)
            TimeScale.WEEKS -> weeks(safeBirth, today, age, yearsLived, beyond)
            TimeScale.DAYS -> days(safeBirth, today, age, yearsLived, beyond)
            TimeScale.HOURS -> hours(now, beyond)
        }
    }

    // Mayor y tal que cumpleaños(y) <= hoy. Maneja bien el 29 de febrero.
    private fun yearsLived(birth: LocalDate, today: LocalDate): Int {
        var y = ChronoUnit.YEARS.between(birth, today).toInt()
        while (!birth.plusYears((y + 1).toLong()).isAfter(today)) y++
        while (y > 0 && birth.plusYears(y.toLong()).isAfter(today)) y--
        return y
    }

    private fun monthsLived(birth: LocalDate, today: LocalDate): Int {
        var m = ChronoUnit.MONTHS.between(birth, today).toInt()
        while (!birth.plusMonths((m + 1).toLong()).isAfter(today)) m++
        while (m > 0 && birth.plusMonths(m.toLong()).isAfter(today)) m--
        return m
    }

    private fun years(age: Int, yearsLived: Int, beyond: Boolean) = ScaleSnapshot(
        scale = TimeScale.YEARS,
        total = age,
        elapsed = if (beyond) age else yearsLived,
        currentIndex = if (beyond) null else yearsLived,
        columns = 10,
        beyondReference = beyond
    )

    private fun months(birth: LocalDate, today: LocalDate, age: Int, beyond: Boolean): ScaleSnapshot {
        val total = age * 12
        val lived = monthsLived(birth, today)
        return ScaleSnapshot(
            scale = TimeScale.MONTHS,
            total = total,
            elapsed = if (beyond) total else lived.coerceAtMost(total),
            currentIndex = if (lived >= total) null else lived,
            columns = 12,
            beyondReference = beyond
        )
    }

    private fun weeks(birth: LocalDate, today: LocalDate, age: Int, yearsLived: Int, beyond: Boolean): ScaleSnapshot {
        val total = age * WEEKS_PER_ROW
        if (beyond) {
            return ScaleSnapshot(TimeScale.WEEKS, total, total, null, WEEKS_PER_ROW, true)
        }
        val lastBirthday = birth.plusYears(yearsLived.toLong())
        val dayInYear = ChronoUnit.DAYS.between(lastBirthday, today).toInt()
        val weekInYear = (dayInYear / 7).coerceAtMost(WEEKS_PER_ROW - 1) // la semana 52 absorbe sobrantes
        val current = yearsLived * WEEKS_PER_ROW + weekInYear
        return ScaleSnapshot(TimeScale.WEEKS, total, current, current, WEEKS_PER_ROW, false)
    }

    // Días del año de vida actual (o del último año de referencia si ya se superó).
    private fun days(birth: LocalDate, today: LocalDate, age: Int, yearsLived: Int, beyond: Boolean): ScaleSnapshot {
        val lifeYear = if (beyond) age - 1 else yearsLived
        val start = birth.plusYears(lifeYear.toLong())
        val end = birth.plusYears((lifeYear + 1).toLong())
        val total = ChronoUnit.DAYS.between(start, end).toInt()
        if (beyond) {
            return ScaleSnapshot(TimeScale.DAYS, total, total, null, 7, true)
        }
        val index = ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, total - 1)
        return ScaleSnapshot(TimeScale.DAYS, total, index, index, 7, false)
    }

    private fun hours(now: LocalDateTime, beyond: Boolean): ScaleSnapshot =
        if (beyond) ScaleSnapshot(TimeScale.HOURS, 24, 24, null, 6, true)
        else ScaleSnapshot(TimeScale.HOURS, 24, now.hour, now.hour, 6, false)
}