package com.tiempo.app.service.assistant

import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.domain.model.FrequencyType
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.usecase.AddJournalNoteUseCase
import com.tiempo.app.domain.usecase.CompleteGoalUseCase
import com.tiempo.app.domain.usecase.SaveGoalUseCase
import com.tiempo.app.service.notifications.GoalNotifier
import com.tiempo.app.service.scheduling.GoalAlarmScheduler
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class AssistantExecutor @Inject constructor(
    private val addNote: AddJournalNoteUseCase,
    private val saveGoal: SaveGoalUseCase,
    private val completeGoal: CompleteGoalUseCase,
    private val scheduler: GoalAlarmScheduler,
    private val notifier: GoalNotifier,
    private val clock: Clock
) {
    /** Ejecuta el comando y devuelve el mensaje de resultado para el usuario. */
    suspend fun execute(command: AssistantCommand): String = when (command) {
        is AssistantCommand.AddNote -> {
            addNote(command.date, command.text)
            "Nota guardada en tu diario."
        }

        is AssistantCommand.CreateGoal -> {
            val goal = Goal(
                title = command.title,
                startDate = LocalDate.now(clock),
                time = command.time,
                frequency = command.frequency,
                daysOfWeek = command.days,
                dayOfMonth = command.dayOfMonth,
                interval = command.interval
            )
            val id = saveGoal(goal)
            scheduler.schedule(goal.copy(id = id))
            "Meta creada: ${command.title}, ${command.frequencyText()} a las ${command.time.hhmm()}."
        }

        is AssistantCommand.CompleteGoal -> {
            completeGoal(command.goalId, command.date)
            notifier.cancel(command.goalId)
            "Marcada como hecha: ${command.title}."
        }

        is AssistantCommand.Unknown -> command.reason
        is AssistantCommand.NeedMore -> command.question
    }
}

// ---------------------------------------------------------------- textos

private val ES = Locale.forLanguageTag("es")
private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

private fun java.time.LocalTime.hhmm(): String = format(HHMM)

private fun dayName(d: DayOfWeek): String = d.getDisplayName(TextStyle.FULL, ES)

private fun pluralDay(d: DayOfWeek): String = dayName(d).let { if (it.endsWith("s")) it else it + "s" }

private fun joinNatural(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items[0]
    else -> items.dropLast(1).joinToString(", ") + " y " + items.last()
}

fun AssistantCommand.CreateGoal.frequencyText(): String = when (frequency) {
    FrequencyType.DAILY -> "todos los días"
    FrequencyType.WEEKDAYS ->
        if (days == setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        ) "de lunes a viernes"
        else "los " + joinNatural(days.sorted().map(::pluralDay))
    FrequencyType.WEEKLY -> "cada " + dayName(days.first())
    FrequencyType.MONTHLY -> "el día ${dayOfMonth ?: 1} de cada mes"
    FrequencyType.EVERY_N_DAYS -> "cada $interval días"
    FrequencyType.YEARLY -> "cada año"
}

/** Texto de la tarjeta de confirmación. */
fun AssistantCommand.confirmationText(): String = when (this) {
    is AssistantCommand.AddNote -> "¿Guardar esta nota en tu diario de hoy?\n\n“$text”"
    is AssistantCommand.CreateGoal ->
        "¿Crear la meta “$title” ${frequencyText()} a las ${time.hhmm()}?"
    is AssistantCommand.CompleteGoal -> "¿Marcar “$title” como hecha hoy?"
    is AssistantCommand.Unknown -> reason
    is AssistantCommand.NeedMore -> question
}