package com.tiempo.app.presentation.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SpanishLocale = Locale.forLanguageTag("es-ES")
private val LongDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", SpanishLocale)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.formatAsLongDate(): String = format(LongDateFormatter)

fun LocalTime.formatAsTime(): String = format(TimeFormatter)
