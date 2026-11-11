package com.tiempo.app.service.assistant

import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.domain.assistant.CommandInterpreter
import com.tiempo.app.domain.assistant.InterpreterContext
import javax.inject.Inject

class RoutingInterpreter @Inject constructor(
    private val settings: AssistantSettings,
    private val keywords: KeywordInterpreter
) : CommandInterpreter {

    override suspend fun interpret(text: String, context: InterpreterContext): AssistantCommand {
        // Pasos 5 y 6: aquí se probará primero el intérprete del modo elegido (local o nube)
        // y, si falla o no está disponible, se caerá en palabras clave.
        return when (settings.mode.value) {
            AssistantMode.KEYWORDS,
            AssistantMode.LOCAL,
            AssistantMode.CLOUD -> keywords.interpret(text, context)
        }
    }
}