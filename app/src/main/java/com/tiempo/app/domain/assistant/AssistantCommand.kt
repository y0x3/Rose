package com.tiempo.app.domain.assistant

import com.tiempo.app.domain.model.FrequencyType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

sealed interface AssistantCommand {

    data class AddNote(val text: String, val date: LocalDate) : AssistantCommand

    data class CreateGoal(
        val title: String,
        val time: LocalTime,
        val frequency: FrequencyType = FrequencyType.DAILY,
        val days: Set<DayOfWeek> = emptySet(),
        val dayOfMonth: Int? = null,
        val interval: Int = 1
    ) : AssistantCommand

    data class CompleteGoal(val goalId: Long, val title: String, val date: LocalDate) : AssistantCommand

    /** El intérprete no entendió; [reason] es el mensaje para el usuario. */
    data class Unknown(val reason: String) : AssistantCommand

    /**
     * Falta un dato: se muestra [question], se escucha la respuesta y [resume]
     * la convierte en otro comando (que puede volver a preguntar).
     */
    class NeedMore(
        val question: String,
        val resume: (String) -> AssistantCommand
    ) : AssistantCommand
}

/** Metas activas que el intérprete puede mencionar (solo id y título, nada más). */
data class GoalRef(val id: Long, val title: String)

/** Contexto que recibe cualquier intérprete. Los LLM futuros recibirán exactamente esto. */
data class InterpreterContext(
    val today: LocalDate,
    val goals: List<GoalRef>
)

interface CommandInterpreter {
    suspend fun interpret(text: String, context: InterpreterContext): AssistantCommand
}