package com.tiempo.app.presentation.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalTime
import kotlin.random.Random
import kotlinx.coroutines.delay

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
    val greeting = remember { greetingFor(LocalTime.now().hour) }
    var promptIndex by remember { mutableIntStateOf(Random.nextInt(prompts.size)) }

    // Cambia el mensaje cada 8 s mientras no se esté escuchando
    LaunchedEffect(state.listening) {
        while (!state.listening) {
            delay(8_000)
            promptIndex = (promptIndex + 1) % prompts.size
        }
    }

    val message = if (state.listening) "Te escucho…" else prompts[promptIndex]

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
            AssistantButton(listening = state.listening, onClick = viewModel::onMicTap)
        }

        Text(
            text = if (state.listening) "Toca para detener" else "Toca para hablar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
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
        modifier = Modifier.fillMaxSize().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // El círculo ocupa el lado menor del espacio disponible
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