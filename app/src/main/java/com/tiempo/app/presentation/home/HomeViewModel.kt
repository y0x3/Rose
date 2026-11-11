package com.tiempo.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.domain.assistant.CommandInterpreter
import com.tiempo.app.domain.assistant.GoalRef
import com.tiempo.app.domain.assistant.InterpreterContext
import com.tiempo.app.domain.model.occursOn
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.service.assistant.AssistantExecutor
import com.tiempo.app.service.assistant.SpeechCallbacks
import com.tiempo.app.service.assistant.SpeechController
import com.tiempo.app.service.assistant.SpeechEngine
import com.tiempo.app.service.assistant.SpeechError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoicePhase { IDLE, LISTENING, PROCESSING }

data class HomeUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val heardText: String = "",
    val isFinal: Boolean = false,
    val error: String? = null,
    val engine: SpeechEngine? = null,
    val permissionDenied: Boolean = false,
    /** Comando esperando el Sí/No del usuario. */
    val pending: AssistantCommand? = null,
    /** Mensaje de resultado tras ejecutar. */
    val result: String? = null,
    val executing: Boolean = false,
    /** Pregunta de seguimiento que el asistente le hace al usuario. */
    val question: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val interpreter: CommandInterpreter,
    private val executor: AssistantExecutor,
    private val goalRepository: GoalRepository,
    private val clock: Clock
) : ViewModel() {

    private val speech = SpeechController(context)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Pregunta pendiente: la siguiente frase reconocida es la respuesta. */
    private var followUp: AssistantCommand.NeedMore? = null

    private val callbacks = object : SpeechCallbacks {
        override fun onListening(engine: SpeechEngine) {
            _uiState.update { it.copy(phase = VoicePhase.LISTENING, engine = engine) }
        }

        override fun onPartial(text: String) {
            _uiState.update { it.copy(heardText = text, isFinal = false) }
        }

        override fun onEndOfSpeech() {
            _uiState.update { it.copy(phase = VoicePhase.PROCESSING) }
        }

        override fun onFinal(text: String) {
            _uiState.update {
                it.copy(phase = VoicePhase.PROCESSING, heardText = text, isFinal = true)
            }
            handleText(text)
        }

        override fun onError(error: SpeechError) {
            followUp = null
            _uiState.update {
                it.copy(phase = VoicePhase.IDLE, error = error.toMessage(), question = null)
            }
        }
    }

    private fun handleText(text: String) {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val waiting = followUp
            followUp = null

            val command = runCatching {
                if (waiting != null) {
                    waiting.resume(text)
                } else {
                    // Solo metas que tocan hoy: es lo que se puede marcar como hecha
                    val goals = goalRepository.observeGoals().first()
                        .filter { it.occursOn(today) }
                        .map { GoalRef(it.id, it.title) }
                    interpreter.interpret(text, InterpreterContext(today, goals))
                }
            }.getOrElse { AssistantCommand.Unknown("No pude interpretar eso. Intenta de nuevo.") }

            if (command is AssistantCommand.NeedMore) {
                followUp = command
                _uiState.update {
                    it.copy(
                        phase = VoicePhase.LISTENING,
                        question = command.question,
                        heardText = "",
                        isFinal = false
                    )
                }
                delay(500)                       // deja que el reconocedor anterior se libere
                if (followUp === command) speech.start(callbacks)
            } else {
                _uiState.update { it.copy(phase = VoicePhase.IDLE, pending = command, question = null) }
            }
        }
    }

    fun confirm() {
        val command = _uiState.value.pending ?: return
        if (_uiState.value.executing) return
        _uiState.update { it.copy(executing = true) }
        viewModelScope.launch {
            val message = runCatching { executor.execute(command) }
                .getOrElse { "No se pudo completar la acción." }
            _uiState.update {
                it.copy(pending = null, executing = false, result = message, heardText = "")
            }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(pending = null, heardText = "", result = null) }
    }

    fun startListening() {
        followUp = null
        _uiState.update {
            it.copy(
                phase = VoicePhase.LISTENING,
                heardText = "",
                isFinal = false,
                error = null,
                result = null,
                pending = null,
                question = null,
                permissionDenied = false
            )
        }
        speech.start(callbacks)
    }

    fun stopListening() {
        speech.stop()
    }

    fun cancel() {
        followUp = null
        speech.cancel()
        _uiState.update { it.copy(phase = VoicePhase.IDLE, question = null) }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionDenied = true) }
    }

    override fun onCleared() {
        speech.cancel()
        super.onCleared()
    }

    private fun SpeechError.toMessage(): String = when (this) {
        SpeechError.NOT_AVAILABLE ->
            "Este teléfono no tiene un servicio de reconocimiento de voz. " +
                    "Instala \"Servicios de voz de Google\" desde Play Store."
        SpeechError.NO_SPEECH -> "No escuché nada. Intenta de nuevo."
        SpeechError.NO_MATCH -> "No te entendí. Intenta de nuevo."
        SpeechError.NETWORK -> "Sin conexión. El reconocimiento de voz necesita internet en este teléfono."
        SpeechError.PERMISSION -> "Falta el permiso del micrófono."
        SpeechError.BUSY -> "El reconocedor está ocupado. Espera un momento."
        SpeechError.LANGUAGE -> "El español no está disponible para reconocimiento de voz."
        SpeechError.OTHER -> "No se pudo escuchar. Intenta de nuevo."
    }
}