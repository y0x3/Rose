package com.tiempo.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiempo.app.data.local.HistoryPinStore
import com.tiempo.app.domain.usecase.ResetJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryLockState(
    val hasPin: Boolean,
    val unlocked: Boolean = false,
    val wrong: Boolean = false,
    val attempt: Int = 0,
    val blockedUntil: Long = 0L
) {
    val locked: Boolean get() = hasPin && !unlocked
}

@HiltViewModel
class HistoryLockViewModel @Inject constructor(
    private val store: HistoryPinStore,
    private val resetJournal: ResetJournalUseCase,
    private val clock: Clock
) : ViewModel() {

    private companion object {
        const val GRACE_MS = 60_000L
        const val BLOCK_MS = 30_000L
        const val MAX_FAILS = 5
    }

    private val _state = MutableStateFlow(HistoryLockState(hasPin = store.hasPin()))
    val state: StateFlow<HistoryLockState> = _state.asStateFlow()

    private var failed = 0
    private var stoppedAt = 0L

    fun tryUnlock(pin: String) {
        val now = clock.millis()
        if (now < _state.value.blockedUntil) {
            _state.update { it.copy(attempt = it.attempt + 1) }
            return
        }
        if (store.check(pin)) {
            failed = 0
            _state.update { it.copy(unlocked = true, wrong = false) }
        } else {
            failed++
            val blocked = if (failed % MAX_FAILS == 0) now + BLOCK_MS else 0L
            _state.update {
                it.copy(wrong = true, attempt = it.attempt + 1, blockedUntil = blocked)
            }
        }
    }

    /** Comprueba el PIN actual (para cambiarlo o quitarlo). */
    fun verify(pin: String): Boolean = store.check(pin)

    /** Guarda el PIN y devuelve el código de recuperación nuevo. */
    fun setPin(pin: String): String {
        val code = store.setPin(pin)
        failed = 0
        _state.update { it.copy(hasPin = true, unlocked = true, wrong = false) }
        return code
    }

    fun removePin() {
        store.clear()
        _state.update { it.copy(hasPin = false, unlocked = false, wrong = false) }
    }

    /** Con el código correcto se quita el PIN. Devuelve si el código era válido. */
    fun recoverWithCode(code: String): Boolean {
        if (!store.checkRecoveryCode(code)) return false
        removePin()
        return true
    }

    /** Último recurso: borra notas y fotos, y quita el PIN. */
    fun resetEverything() {
        viewModelScope.launch {
            resetJournal()
            removePin()
        }
    }

    fun lock() {
        _state.update { it.copy(unlocked = false, wrong = false) }
    }

    fun onStopped() { stoppedAt = clock.millis() }

    fun onStarted() {
        if (stoppedAt > 0 && clock.millis() - stoppedAt > GRACE_MS) lock()
        stoppedAt = 0L
    }
}