package com.tiempo.app.domain.model

import java.time.LocalDateTime

/**
 * Próxima fecha y hora en que debe avisar esta meta, estrictamente después de [after].
 * Devuelve null si ya no hay más ocurrencias (por ejemplo, pasó su fecha de fin).
 */
fun Goal.nextOccurrence(after: LocalDateTime): LocalDateTime? {
    var date = after.toLocalDate()
    repeat(400) {
        if (occursOn(date)) {
            val candidate = LocalDateTime.of(date, time)
            if (candidate.isAfter(after)) return candidate
        }
        date = date.plusDays(1)
    }
    return null
}