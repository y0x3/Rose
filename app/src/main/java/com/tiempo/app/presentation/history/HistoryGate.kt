package com.tiempo.app.presentation.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

private enum class SecurityStep { NONE, MENU, SET, VERIFY_CHANGE, VERIFY_REMOVE, SET_NEW }

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    lockViewModel: HistoryLockViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val state by lockViewModel.state.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(SecurityStep.NONE) }
    var recoveryCode by remember { mutableStateOf<String?>(null) }
    var showForgot by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Se bloquea al salir de la pestaña y tras 60 s con la app en segundo plano
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> lockViewModel.onStopped()
                Lifecycle.Event.ON_START -> lockViewModel.onStarted()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            lockViewModel.lock()
        }
    }

    LaunchedEffect(state.locked) {
        if (state.locked) historyViewModel.closeDay()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.locked) {
            PinLockScreen(
                state = state,
                onPin = lockViewModel::tryUnlock,
                onForgot = { showForgot = true }
            )
        } else {
            HistoryContent(viewModel = historyViewModel, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { step = SecurityStep.MENU },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (state.hasPin) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Seguridad del historial"
                )
            }
        }
    }

    when (step) {
        SecurityStep.NONE -> Unit

        SecurityStep.MENU -> AlertDialog(
            onDismissRequest = { step = SecurityStep.NONE },
            title = { Text("Seguridad del historial") },
            text = {
                Text(
                    if (state.hasPin) "El historial está protegido con un PIN de 4 dígitos."
                    else "Puedes proteger el historial con un PIN de 4 dígitos. Es opcional."
                )
            },
            confirmButton = {
                if (state.hasPin) {
                    TextButton(onClick = { step = SecurityStep.VERIFY_CHANGE }) { Text("Cambiar PIN") }
                } else {
                    TextButton(onClick = { step = SecurityStep.SET }) { Text("Activar PIN") }
                }
            },
            dismissButton = {
                Column {
                    if (state.hasPin) {
                        TextButton(onClick = { step = SecurityStep.VERIFY_REMOVE }) {
                            Text("Quitar PIN")
                        }
                    }
                    TextButton(onClick = { step = SecurityStep.NONE }) { Text("Cerrar") }
                }
            }
        )

        SecurityStep.SET, SecurityStep.SET_NEW -> SetPinDialog(
            onDismiss = { step = SecurityStep.NONE },
            onConfirmed = { pin ->
                recoveryCode = lockViewModel.setPin(pin)
                step = SecurityStep.NONE
            }
        )

        SecurityStep.VERIFY_CHANGE -> VerifyPinDialog(
            title = "Escribe tu PIN actual",
            verify = lockViewModel::verify,
            onDismiss = { step = SecurityStep.NONE },
            onVerified = { step = SecurityStep.SET_NEW }
        )

        SecurityStep.VERIFY_REMOVE -> VerifyPinDialog(
            title = "Escribe tu PIN para quitarlo",
            verify = lockViewModel::verify,
            onDismiss = { step = SecurityStep.NONE },
            onVerified = {
                lockViewModel.removePin()
                step = SecurityStep.NONE
            }
        )
    }

    recoveryCode?.let { code ->
        RecoveryCodeDialog(code = code, onDone = { recoveryCode = null })
    }

    if (showForgot) {
        ForgotPinDialog(
            onDismiss = { showForgot = false },
            onSubmit = { input ->
                val ok = lockViewModel.recoverWithCode(input)
                if (ok) showForgot = false
                ok
            },
            onNoCode = {
                showForgot = false
                showReset = true
            }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("¿Borrar todo el historial?") },
            text = {
                Text(
                    "Se borrarán todas tus notas y fotos y se quitará el PIN. " +
                            "Tus metas y su registro de cumplimiento no se tocan. " +
                            "No se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    lockViewModel.resetEverything()
                }) { Text("Borrar todo", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PinLockScreen(
    state: HistoryLockState,
    onPin: (String) -> Unit,
    onForgot: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.blockedUntil) {
        while (System.currentTimeMillis() < state.blockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    val secondsLeft = ((state.blockedUntil - now + 999) / 1000).toInt()
    val blocked = secondsLeft > 0

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PinPad(
                    title = "Historial protegido",
                    message = when {
                        blocked -> "Demasiados intentos. Espera $secondsLeft s"
                        state.wrong -> "PIN incorrecto"
                        else -> "Escribe tu PIN"
                    },
                    isError = blocked || state.wrong,
                    enabled = !blocked,
                    resetKey = state.attempt,
                    onComplete = onPin
                )
                TextButton(onClick = onForgot) { Text("¿Olvidaste tu PIN?") }
            }
        }
    }
}

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onConfirmed: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var mismatches by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                PinPad(
                    title = if (first == null) "Elige un PIN" else "Repítelo",
                    message = when {
                        mismatches > 0 && first == null -> "No coincidían. Intenta de nuevo"
                        first == null -> "4 dígitos. Después te daremos un código de recuperación"
                        else -> "Confirma tu PIN"
                    },
                    isError = mismatches > 0 && first == null,
                    enabled = true,
                    resetKey = Pair(first, mismatches),
                    onComplete = { pin ->
                        val saved = first
                        when {
                            saved == null -> first = pin
                            saved == pin -> onConfirmed(pin)
                            else -> { first = null; mismatches++ }
                        }
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun VerifyPinDialog(
    title: String,
    verify: (String) -> Boolean,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var wrong by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                PinPad(
                    title = title,
                    message = if (wrong > 0) "PIN incorrecto" else null,
                    isError = wrong > 0,
                    enabled = true,
                    resetKey = wrong,
                    onComplete = { pin -> if (verify(pin)) onVerified() else wrong++ }
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Se muestra una sola vez y no se puede cerrar hasta confirmar que se anotó. */
@Composable
private fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    var saved by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Guarda tu código de recuperación") },
        text = {
            Column {
                Text(
                    "Si olvidas el PIN, este código es la única forma de volver a entrar " +
                            "sin perder tu diario. Solo se muestra ahora. Anótalo en papel " +
                            "y guárdalo en un lugar seguro."
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saved, onCheckedChange = { saved = it })
                    Text("Ya lo anoté")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone, enabled = saved) { Text("Continuar") }
        }
    )
}

@Composable
private fun ForgotPinDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Boolean,
    onNoCode: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recuperar acceso") },
        text = {
            Column {
                Text("Escribe el código de recuperación que se te mostró al crear el PIN.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; wrong = false },
                    label = { Text("Código") },
                    singleLine = true,
                    isError = wrong,
                    supportingText = if (wrong) {
                        { Text("Código incorrecto") }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onNoCode) { Text("No tengo el código") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!onSubmit(text)) wrong = true },
                enabled = text.isNotBlank()
            ) { Text("Recuperar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}