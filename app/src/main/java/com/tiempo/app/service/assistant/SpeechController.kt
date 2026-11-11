package com.tiempo.app.service.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

enum class SpeechEngine { ON_DEVICE, SERVICE }

enum class SpeechError {
    NOT_AVAILABLE, NO_SPEECH, NO_MATCH, NETWORK, PERMISSION, BUSY, LANGUAGE, OTHER
}

interface SpeechCallbacks {
    fun onListening(engine: SpeechEngine)
    fun onPartial(text: String)
    fun onEndOfSpeech()
    fun onFinal(text: String)
    fun onError(error: SpeechError)
}

/** Debe usarse siempre desde el hilo principal (SpeechRecognizer lo exige). */
class SpeechController(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var session = 0

    fun start(callbacks: SpeechCallbacks) {
        cancel()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callbacks.onError(SpeechError.NOT_AVAILABLE)
            return
        }
        val onDevice = Build.VERSION.SDK_INT >= 33 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        begin(callbacks, if (onDevice) SpeechEngine.ON_DEVICE else SpeechEngine.SERVICE)
    }

    /** Termina de escuchar y entrega lo que alcanzó a entender. */
    fun stop() {
        runCatching { recognizer?.stopListening() }
    }

    /** Descarta todo sin entregar resultados. */
    fun cancel() {
        session++
        runCatching {
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
    }

    private fun begin(callbacks: SpeechCallbacks, engine: SpeechEngine) {
        val id = ++session
        val r = if (engine == SpeechEngine.ON_DEVICE && Build.VERSION.SDK_INT >= 33) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = r

        r.setRecognitionListener(object : RecognitionListener {
            private fun alive() = id == session

            override fun onReadyForSpeech(params: Bundle?) {
                if (alive()) callbacks.onListening(engine)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!alive()) return
                partialResults.firstText()?.let { callbacks.onPartial(it) }
            }

            override fun onEndOfSpeech() {
                if (alive()) callbacks.onEndOfSpeech()
            }

            override fun onResults(results: Bundle?) {
                if (!alive()) return
                val text = results.firstText()
                if (text.isNullOrBlank()) callbacks.onError(SpeechError.NO_MATCH)
                else callbacks.onFinal(text)
            }

            override fun onError(error: Int) {
                if (!alive()) return
                // Sin idioma descargado en el reconocedor local: probamos el servicio normal
                val languageProblem = error == 12 || error == 13
                if (engine == SpeechEngine.ON_DEVICE && languageProblem) {
                    runCatching { r.cancel(); r.destroy() }
                    begin(callbacks, SpeechEngine.SERVICE)
                    return
                }
                callbacks.onError(error.toSpeechError())
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        r.startListening(intent)
    }

    private fun Bundle?.firstText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun Int.toSpeechError(): SpeechError = when (this) {
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechError.NO_SPEECH
        SpeechRecognizer.ERROR_NO_MATCH -> SpeechError.NO_MATCH
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER -> SpeechError.NETWORK
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError.PERMISSION
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SpeechError.BUSY
        12, 13 -> SpeechError.LANGUAGE
        else -> SpeechError.OTHER
    }
}