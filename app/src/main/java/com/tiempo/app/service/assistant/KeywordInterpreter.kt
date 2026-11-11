package com.tiempo.app.service.assistant

import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.domain.assistant.CommandInterpreter
import com.tiempo.app.domain.assistant.GoalRef
import com.tiempo.app.domain.assistant.InterpreterContext
import com.tiempo.app.domain.model.FrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class KeywordInterpreter @Inject constructor() : CommandInterpreter {

    override suspend fun interpret(text: String, context: InterpreterContext): AssistantCommand {
        val s = Span.of(text.trim())
        if (s.norm.isBlank()) return unknown()

        // 1) Notas
        Rx.NOTES.firstNotNullOfOrNull { it.find(s.norm) }?.let { m ->
            return noteFrom(s.after(m.range.last + 1), context.today)
        }

        // 2) "marca X como hecho"
        Rx.DONE_B1.find(s.norm)?.let { m ->
            val g = m.groups[1]!!.range
            return completeFrom(s.orig.substring(g.first, g.last + 1), context.goals)
        }
        Rx.DONE_B2.find(s.norm)?.let { m ->
            val g = m.groups[1]!!.range
            return completeFrom(s.orig.substring(g.first, g.last + 1), context.goals)
        }

        // 3) Meta nueva
        Rx.GOAL.find(s.norm)?.let { m ->
            return goalFrom(s.after(m.range.last + 1), context.today)
        }

        // 4) "ya hice X"
        Rx.DONE_A.find(s.norm)?.let { m ->
            return completeFrom(s.after(m.range.last + 1).orig, context.goals)
        }

        return unknown()
    }

    private fun unknown() = AssistantCommand.Unknown(
        "No te entendí. Puedes decir, por ejemplo: “anota que hoy corrí”, " +
                "“nueva meta leer todos los días a las 9 de la noche” o “ya hice el gimnasio”."
    )

    // ------------------------------------------------------------------ notas

    private fun noteFrom(rest: Span, today: LocalDate): AssistantCommand {
        val text = rest.orig.trim { it in " ,:;." }
        return if (text.isBlank()) {
            AssistantCommand.NeedMore("¿Qué quieres anotar?") { answer ->
                val t = answer.trim { it in " ,:;." }
                if (t.isBlank()) unknown() else AssistantCommand.AddNote(t.capitalized(), today)
            }
        } else {
            AssistantCommand.AddNote(text.capitalized(), today)
        }
    }

    // ------------------------------------------------------------------ metas

    private data class Freq(
        val type: FrequencyType,
        val days: Set<DayOfWeek> = emptySet(),
        val dayOfMonth: Int? = null,
        val interval: Int = 1
    )

    private fun goalFrom(rest: Span, today: LocalDate): AssistantCommand {
        val time = takeTime(rest, Rx.TIME_MAIN)      // primero la hora, para no confundirla con el título
        val freq = takeFrequency(rest, today)
        return finishGoal(cleanTitle(rest.orig), time, freq)
    }

    private fun finishGoal(title: String, time: LocalTime?, freq: Freq): AssistantCommand = when {
        title.isBlank() -> AssistantCommand.NeedMore("¿Cómo se llama la meta?") { answer ->
            finishGoal(cleanTitle(answer), time, freq)
        }

        time == null -> AssistantCommand.NeedMore("¿A qué hora quieres que te avise?") { answer ->
            val parsed = takeTime(Span.of(answer), Rx.TIME_ANY)
            if (parsed == null) {
                AssistantCommand.Unknown(
                    "No entendí la hora. Vuelve a empezar y dime, por ejemplo, “a las 7 de la mañana”."
                )
            } else {
                finishGoal(title, parsed, freq)
            }
        }

        else -> AssistantCommand.CreateGoal(
            title = title.capitalized(),
            time = time,
            frequency = freq.type,
            days = freq.days,
            dayOfMonth = freq.dayOfMonth,
            interval = freq.interval
        )
    }

    private fun takeFrequency(s: Span, today: LocalDate): Freq {
        val everyN = s.take(Rx.EVERY_N)
        val work = s.take(Rx.WORKDAYS)
        val weekend = s.take(Rx.WEEKEND)
        val list = s.take(Rx.DAY_LIST)
        val monthDay = s.take(Rx.MONTH_DAY)
        val monthly = s.take(Rx.MONTHLY)
        val weekly = s.take(Rx.WEEKLY)
        s.take(Rx.DAILY)

        return when {
            everyN != null -> Freq(
                FrequencyType.EVERY_N_DAYS,
                interval = (numberValue(everyN.groupValues[1]) ?: 2).coerceIn(2, 365)
            )

            work != null -> Freq(
                FrequencyType.WEEKDAYS,
                days = setOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                )
            )

            weekend != null -> Freq(
                FrequencyType.WEEKDAYS,
                days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            )

            list != null -> {
                val days = Rx.DAY_NAME.findAll(list.value).map { dayOf(it.value) }.toSet()
                Freq(if (days.size == 1) FrequencyType.WEEKLY else FrequencyType.WEEKDAYS, days = days)
            }

            monthDay != null -> Freq(
                FrequencyType.MONTHLY,
                dayOfMonth = monthDay.groupValues[1].toInt().coerceIn(1, 31)
            )

            monthly != null -> Freq(FrequencyType.MONTHLY, dayOfMonth = today.dayOfMonth)

            weekly != null -> Freq(FrequencyType.WEEKLY, days = setOf(today.dayOfWeek))

            else -> Freq(FrequencyType.DAILY)
        }
    }

    private fun dayOf(name: String): DayOfWeek = when {
        name.startsWith("lun") -> DayOfWeek.MONDAY
        name.startsWith("mar") -> DayOfWeek.TUESDAY
        name.startsWith("mie") -> DayOfWeek.WEDNESDAY
        name.startsWith("jue") -> DayOfWeek.THURSDAY
        name.startsWith("vie") -> DayOfWeek.FRIDAY
        name.startsWith("sab") -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("\\s+"), " ").trim { it in " ,:;." }
        repeat(3) {
            t = t.replace(Rx.TRAIL, "").replace(Rx.LEAD, "").trim { it in " ,:;." }
        }
        return t
    }

    // ------------------------------------------------------------------ hora

    private fun takeTime(s: Span, regex: Regex): LocalTime? {
        val m = regex.find(s.norm)
        if (m != null) {
            val t = timeFrom(m)
            if (t != null) {
                s.remove(m.range)
                return t
            }
        }
        if (s.take(Rx.NOON) != null) return LocalTime.NOON
        if (s.take(Rx.MIDNIGHT) != null) return LocalTime.MIDNIGHT
        return null
    }

    private fun timeFrom(m: MatchResult): LocalTime? {
        var h = numberValue(m.groupValues[1]) ?: return null
        val colon = m.groupValues[2]
        val y = m.groupValues[3]
        val menos = m.groupValues[4]
        val word = m.groupValues[5]
        val ampm = m.groupValues[6]
        var min = 0

        when {
            colon.isNotEmpty() -> min = colon.toInt()
            y == "media" -> min = 30
            y == "cuarto" -> min = 15
            y.isNotEmpty() -> min = y.toInt()
            menos == "cuarto" -> { h -= 1; min = 45 }
            menos.isNotEmpty() -> { h -= 1; min = 60 - menos.toInt() }
        }
        if (menos.isNotEmpty() && h == 0) h = 12

        val pm = word == "tarde" || word == "noche" || ampm.startsWith("p")
        val am = word.startsWith("ma") || ampm.startsWith("a")

        if (pm) {
            if (word == "noche" && h == 12) h = 0 else if (h < 12) h += 12
        } else if (am && h == 12) {
            h = 0
        }

        if (h !in 0..23 || min !in 0..59) return null
        return LocalTime.of(h, min)
    }

    // ------------------------------------------------------ completar metas

    private fun completeFrom(query: String, goals: List<GoalRef>): AssistantCommand {
        if (goals.isEmpty()) {
            return AssistantCommand.Unknown("No tienes metas programadas para hoy.")
        }
        val q = tokens(query)
        if (q.isEmpty()) {
            return AssistantCommand.NeedMore("¿Qué meta completaste?") { answer ->
                completeFrom(answer, goals)
            }
        }

        val scored = goals.map { it to score(q, tokens(it.title)) }
        val best = scored.maxOf { it.second }
        if (best <= 0.0) {
            return AssistantCommand.Unknown(
                "No encontré ninguna meta de hoy parecida a “${query.trim()}”."
            )
        }
        val top = scored.filter { it.second == best }.map { it.first }
        if (top.size == 1) {
            val g = top.first()
            return AssistantCommand.CompleteGoal(g.id, g.title, LocalDate.now())
        }
        return AssistantCommand.NeedMore(
            "¿Cuál? " + top.joinToString(" o ") { "“${it.title}”" }
        ) { answer -> completeFrom(answer, top) }
    }

    private fun tokens(text: String): List<String> =
        fold(text).split(Regex("[^a-zñ0-9]+"))
            .filter { it.length >= 2 && it !in Rx.STOP }

    private fun score(query: List<String>, goal: List<String>): Double =
        query.count { q -> goal.any { g -> sameWord(q, g) } }.toDouble() / query.size

    /** Tolera plurales y conjugaciones cercanas: ejercicio/ejercicios, correr/corrí. */
    private fun sameWord(a: String, b: String): Boolean {
        if (a == b) return true
        val min = minOf(a.length, b.length)
        if (min < 4) return false
        val common = a.commonPrefixWith(b).length
        return common >= 4 && common >= min - 1
    }
}

// ============================================================ utilidades

private fun String.capitalized() = replaceFirstChar { it.uppercase() }

/** Minúsculas y sin acentos, **conservando la longitud** para poder cortar el texto original. */
private fun fold(s: String): String = buildString(s.length) {
    for (c in s) {
        append(
            when (val l = Character.toLowerCase(c)) {
                'á' -> 'a'
                'é' -> 'e'
                'í' -> 'i'
                'ó' -> 'o'
                'ú', 'ü' -> 'u'
                else -> l
            }
        )
    }
}

private val WORDS = mapOf(
    "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
    "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
    "once" to 11, "doce" to 12
)

private fun numberValue(s: String): Int? = s.toIntOrNull() ?: WORDS[s]

/** Texto original + versión normalizada de la misma longitud. */
private class Span(var orig: String, var norm: String) {
    companion object {
        fun of(text: String) = Span(text, fold(text))
    }

    fun remove(range: IntRange) {
        orig = orig.removeRange(range)
        norm = norm.removeRange(range)
    }

    /** Busca [regex], quita ese tramo de ambos textos y devuelve la coincidencia. */
    fun take(regex: Regex): MatchResult? {
        val m = regex.find(norm) ?: return null
        remove(m.range)
        return m
    }

    fun after(index: Int) = Span(orig.substring(index), norm.substring(index))
}

private object Rx {
    private const val FILLER = "(?:(?:por favor|oye|ok|hola)\\s+)*"
    private const val DONE =
        "(?:hecho|hecha|completad[oa]|terminad[oa]|cumplid[oa]|listo|lista)"
    private const val NUM = "\\d{1,2}|una|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|once|doce"
    private const val DAY = "(?:lunes|martes|miercoles|jueves|viernes|sabados?|domingos?)"
    private const val TIME_CORE =
        "($NUM)(?:\\s*[:.h]\\s*(\\d{2})|\\s+y\\s+(media|cuarto|\\d{1,2})|\\s+menos\\s+(cuarto|\\d{1,2}))?" +
                "(?:\\s*(?:de\\s+la\\s+(ma[ñn]ana|tarde|noche|madrugada)|(a\\.?\\s?m\\.?|p\\.?\\s?m\\.?)|horas|hrs\\.?))?"

    // --- Intenciones ---
    val NOTES = listOf(
        Regex(
            "^$FILLER(?:anota|anotame|apunta|apuntame)\\b[\\s,:]*(?:(?:una\\s+)?nota\\b[\\s,:]*)?" +
                    "(?:en\\s+(?:mi|el)\\s+diario\\b[\\s,:]*)?(?:(?:de\\s+que|que|de|sobre)\\b)?"
        ),
        Regex(
            "^$FILLER(?:(?:quiero|necesito|me gustaria)\\s+)?" +
                    "(?:(?:agrega|agregar|agregame|anade|anadir|escribe|escribir|guarda|guardar|pon|poner|crea|crear|toma|tomar)\\s+)?" +
                    "(?:(?:una|la|mi|otra)\\s+)?(?:nueva\\s+)?nota\\b[\\s,:]*" +
                    "(?:en\\s+(?:mi|el)\\s+diario\\b[\\s,:]*)?(?:(?:de\\s+que|que|de|sobre)\\b)?"
        ),
        Regex(
            "^$FILLER(?:escribe|escribir|agrega|agregar|anade|anadir|pon|poner|guarda|guardar)\\s+" +
                    "(?:algo\\s+)?(?:en|a)\\s+(?:mi|el)\\s+diario\\b[\\s,:]*(?:(?:lo siguiente|que)\\b)?"
        )
    )

    val GOAL = Regex(
        "^$FILLER(?:(?:quiero|necesito|me gustaria)\\s+)?" +
                "(?:(?:agrega|agregar|agregame|anade|anadir|crea|crear|creame|pon|poner|configura|configurar|registra|registrar)\\s+)?" +
                "(?:(?:una|un|otra|otro|mi)\\s+)?(?:(?:nueva|nuevo)\\s+)?(?:meta|objetivo|habito|actividad)\\b[\\s,:]*" +
                "(?:(?:de|para|que|llamada|llamado|se llame)\\b)?"
    )

    val DONE_A = Regex(
        "^$FILLER(?:ya\\s+)?(?:hice|hize|termine|acabe|complete|cumpli|realice|finalice|" +
                "hecho|listo|lista|terminado|completado|completa|completar|termina)\\b"
    )
    val DONE_B1 = Regex("^$FILLER(?:marca|marcar|marcame|pon|poner)\\s+(.+?)\\s+como\\s+$DONE\\b")
    val DONE_B2 = Regex("^$FILLER(?:marca|marcar|marcame)\\s+como\\s+$DONE\\s+(.+)$")

    // --- Hora ---
    val TIME_MAIN = Regex("\\ba\\s+las?\\s+$TIME_CORE")
    val TIME_ANY = Regex("(?:\\ba\\s+las?\\s+)?\\b$TIME_CORE")
    val NOON = Regex("\\bmediodia\\b")
    val MIDNIGHT = Regex("\\bmedianoche\\b")

    // --- Frecuencia ---
    val EVERY_N = Regex("\\bcada\\s+(\\d{1,3}|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez)\\s+dias\\b")
    val WORKDAYS = Regex("\\bde\\s+lunes\\s+a\\s+viernes\\b|\\blunes\\s+a\\s+viernes\\b|\\bentre\\s+semana\\b|\\bdias\\s+(?:laborales|habiles)\\b")
    val WEEKEND = Regex("\\b(?:los\\s+)?(?:fines?\\s+de\\s+semana|findes?)\\b|\\bsabados?\\s+y\\s+domingos?\\b")
    val DAY_NAME = Regex(DAY)
    val DAY_LIST = Regex(
        "\\b(?:(?:todos\\s+)?(?:los|cada|el)\\s+)?$DAY" +
                "(?:(?:\\s*,\\s*|\\s+y\\s+|\\s+e\\s+|\\s+)(?:los\\s+)?$DAY)*\\b"
    )
    val MONTH_DAY = Regex("\\b(?:el\\s+)?(?:dia\\s+)?(\\d{1,2})\\s+de\\s+cada\\s+mes\\b")
    val MONTHLY = Regex("\\bcada\\s+mes\\b|\\bmensual(?:mente)?\\b|\\btodos\\s+los\\s+meses\\b")
    val WEEKLY = Regex("\\bcada\\s+semana\\b|\\bsemanal(?:mente)?\\b|\\buna\\s+vez\\s+(?:a\\s+la|por)\\s+semana\\b|\\btodas\\s+las\\s+semanas\\b")
    val DAILY = Regex("\\btodos\\s+los\\s+dias\\b|\\bcada\\s+dia\\b|\\bdiari[oa]s?\\b|\\bdiariamente\\b|\\ba\\s+diario\\b")

    // --- Limpieza de títulos ---
    val TRAIL = Regex("(?i)\\s+(?:y|a|de|en|por|los|las|el|la|cada|todos|todas|todo|al|del|con)$")
    val LEAD = Regex("(?i)^(?:se\\s+llama|llamala|llamalo|que\\s+se\\s+llame|de|para|que|y|a)\\s+")

    val STOP = setOf(
        "de", "del", "la", "el", "los", "las", "mi", "mis", "con", "un", "una", "unos", "unas",
        "que", "por", "para", "en", "y", "a", "al", "ya", "hoy", "tambien", "lo", "se", "me",
        "meta", "objetivo", "actividad", "tarea", "habito", "es", "mas"
    )
}