package com.tiempo.app.presentation.lifepanel

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.R
import com.tiempo.app.domain.lifepanel.CellState
import com.tiempo.app.domain.lifepanel.ScaleSnapshot
import com.tiempo.app.domain.lifepanel.TimeScale
import kotlin.math.ceil
import kotlin.math.min

// Degradado del tiempo transcurrido: azul -> violeta -> dorado (el acento).
private val LifeGradient = listOf(
    Color(0xFF5B8DB8),
    Color(0xFF8E7CC3),
    Color(0xFFE6C58D)
)

private fun gradientAt(fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f) * (LifeGradient.size - 1)
    val i = f.toInt().coerceAtMost(LifeGradient.size - 2)
    return lerp(LifeGradient[i], LifeGradient[i + 1], f - i)
}

@Composable
fun LifePanelScreen(
    modifier: Modifier = Modifier,
    viewModel: LifePanelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScaleSelector(selected = state.scale, onSelected = viewModel::onScaleSelected)

        val snapshot = state.snapshot
        when {
            state.isLoading -> Unit
            snapshot == null -> Text(
                stringResource(R.string.life_panel_no_profile),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                Text(
                    stringResource(R.string.life_panel_percent, (snapshot.percentElapsed * 100).toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (snapshot.beyondReference) {
                    Text(
                        stringResource(R.string.life_panel_beyond),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // weight(1f): la cuadrícula ocupa exactamente el espacio que sobra.
                LifeGrid(snapshot, Modifier.weight(1f).fillMaxWidth())
            }
        }

        Text(
            stringResource(R.string.life_panel_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScaleSelector(selected: TimeScale, onSelected: (TimeScale) -> Unit) {
    val labels = mapOf(
        TimeScale.YEARS to R.string.scale_years,
        TimeScale.MONTHS to R.string.scale_months,
        TimeScale.WEEKS to R.string.scale_weeks,
        TimeScale.DAYS to R.string.scale_days,
        TimeScale.HOURS to R.string.scale_hours
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeScale.entries.forEach { scale ->
            FilterChip(
                selected = scale == selected,
                onClick = { onSelected(scale) },
                label = { Text(stringResource(labels.getValue(scale)), maxLines = 1) }
            )
        }
    }
}

@Composable
private fun LifeGrid(snapshot: ScaleSnapshot, modifier: Modifier = Modifier) {
    val currentColor = MaterialTheme.colorScheme.primary
    val futureColor = MaterialTheme.colorScheme.outline

    // Colores de los cuadros transcurridos: se calculan una sola vez por cambio.
    val elapsedColors = remember(snapshot.total, snapshot.elapsed) {
        List(snapshot.elapsed) { i -> gradientAt(i / (snapshot.total - 1).coerceAtLeast(1).toFloat()) }
    }

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Canvas(modifier) {
        val rows = ceil(snapshot.total / snapshot.columns.toFloat()).toInt()
        // El menor entre lo que permite el ancho y lo que permite el alto.
        val cell = min(
            min(size.width / snapshot.columns, size.height / rows),
            44.dp.toPx()
        )
        val originX = (size.width - cell * snapshot.columns) / 2f // centrada
        val gap = cell * 0.14f
        val side = cell - gap
        val radius = CornerRadius(side * 0.2f)
        val stroke = Stroke(width = (cell * 0.08f).coerceAtLeast(1f))

        for (i in 0 until snapshot.total) {
            val topLeft = Offset(
                originX + (i % snapshot.columns) * cell + gap / 2,
                (i / snapshot.columns) * cell + gap / 2
            )
            val cellSize = Size(side, side)
            when (snapshot.stateAt(i)) {
                CellState.ELAPSED -> drawRoundRect(elapsedColors[i], topLeft, cellSize, radius)
                CellState.CURRENT -> drawRoundRect(currentColor, topLeft, cellSize, radius, alpha = pulse)
                CellState.FUTURE -> drawRoundRect(futureColor, topLeft, cellSize, radius, style = stroke, alpha = 0.55f)
            }
        }
    }
}