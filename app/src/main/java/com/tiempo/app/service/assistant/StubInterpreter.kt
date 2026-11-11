package com.tiempo.app.service.assistant

import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.domain.assistant.CommandInterpreter
import com.tiempo.app.domain.assistant.InterpreterContext
import java.text.Normalizer
import java.time.LocalTime
import javax.inject.Inject

class StubInterpreter @Inject constructor() : CommandInterpreter {

    override suspend fun interpret(text: String, context: InterpreterContext): AssistantCommand {
        val clean = text.trim()
        val lower = clean.lowercase()

        return when {
            lower.startsWith("nota ") -> AssistantCommand.AddNote(
                text = clean.substring(5).trim(),
                date = context.today
            )

            lower.startsWith("meta ") -> AssistantCommand.CreateGoal(
                title = clean.substring(5).trim().replaceFirstChar { it.uppercase() },
                time = LocalTime.of(8, 0)
            )

            lower.startsWith("hecho ") -> {
                val query = normalize(clean.substring(6))
                val match = context.goals.firstOrNull { normalize(it.title).contains(query) }
                if (match != null) {
                    AssistantCommand.CompleteGoal(match.id, match.title, context.today)
                } else {
                    AssistantCommand.Unknown("No encontré ninguna meta parecida a \"${clean.substring(6).trim()}\".")
                }
            }

            else -> AssistantCommand.Unknown(
                "Por ahora solo entiendo frases que empiecen con \"nota\", \"meta\" o \"hecho\"."
            )
        }
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()
}