package com.tiempo.app.presentation.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.service.assistant.SpeechEngine
import java.time.LocalTime
import kotlin.random.Random
import kotlinx.coroutines.delay
import androidx.compose.material3.AlertDialog
import com.tiempo.app.domain.assistant.AssistantCommand
import com.tiempo.app.service.assistant.confirmationText

private val prompts = listOf(
    "¿Qué puedo hacer por ti?",
    "¿Cómo va el día?",
    "¿Quieres agregar una nueva meta?",
    "¿Anoto algo en tu diario?"
)

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val greeting = remember { greetingFor(LocalTime.now().hour) }
    var promptIndex by remember { mutableIntStateOf(Random.nextInt(prompts.size)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening() else viewModel.onPermissionDenied()
    }

    // Si el usuario sale de la pestaña, se corta el micrófono
    DisposableEffect(Unit) {
        onDispose { viewModel.cancel() }
    }

    // El mensaje rota cada 8 s mientras no se esté escuchando
    LaunchedEffect(state.phase) {
        while (state.phase == VoicePhase.IDLE) {
            delay(8_000)
            promptIndex = (promptIndex + 1) % prompts.size
        }
    }

    val message = when (state.phase) {
        VoicePhase.LISTENING -> state.question ?: "Te escucho…"
        VoicePhase.PROCESSING -> "Procesando…"
        VoicePhase.IDLE -> prompts[promptIndex]
    }

    val onMicTap = {
        when (state.phase) {
            VoicePhase.LISTENING -> viewModel.stopListening()
            VoicePhase.PROCESSING -> viewModel.cancel()
            VoicePhase.IDLE -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.startListening()
                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Crossfade(targetState = message, label = "assistant-message") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AssistantButton(
                listening = state.phase == VoicePhase.LISTENING,
                onClick = onMicTap
            )
        }

        // Zona de texto reconocido y errores
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.heardText.isNotBlank()) {
                Text(
                    text = "“${state.heardText}”",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (state.isFinal) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                state.engine?.let { engine ->
                    Text(
                        text = when (engine) {
                            SpeechEngine.ON_DEVICE -> "Reconocimiento en el teléfono"
                            SpeechEngine.SERVICE -> "Reconocimiento con el servicio de Google"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            state.result?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            if (state.permissionDenied) {
                Text(
                    text = "Necesito el micrófono para escucharte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }) { Text("Abrir ajustes") }
            }
        }

        Text(
            text = when (state.phase) {
                VoicePhase.LISTENING -> "Toca para terminar"
                VoicePhase.PROCESSING -> "Toca para cancelar"
                VoicePhase.IDLE -> "Toca para hablar"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
    }
    state.pending?.let { command ->
        if (command is AssistantCommand.Unknown) {
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text("No te entendí") },
                text = { Text(command.reason) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismiss) { Text("Entendido") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text("¿Es correcto?") },
                text = { Text(command.confirmationText()) },
                confirmButton = {
                    TextButton(onClick = viewModel::confirm, enabled = !state.executing) {
                        Text("Sí")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismiss) { Text("No") }
                }
            )
        }
    }
}

@Composable
private fun AssistantButton(listening: Boolean, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse-scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val diameter = if (maxWidth < maxHeight) maxWidth else maxHeight

        Box(
            modifier = Modifier
                .size(diameter)
                .scale(if (listening) pulse else 1f)
                .clip(CircleShape)
                .background(
                    if (listening) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Hablar con el asistente",
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(diameter / 3)
            )
        }
    }
}