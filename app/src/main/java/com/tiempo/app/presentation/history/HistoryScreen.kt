package com.tiempo.app.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.domain.model.DayHistory
import com.tiempo.app.domain.model.HistoryActivity
import com.tiempo.app.domain.model.HistoryStatus
import com.tiempo.app.domain.model.JournalNote
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape

private val ES = Locale.forLanguageTag("es")
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d", ES)
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ES)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

internal fun Instant.toHour(): String =
    atZone(ZoneId.systemDefault()).toLocalTime().format(hhmm)

@Composable
fun HistoryContent(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Historial",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            if (!state.isLoading && state.days.isEmpty()) {
                Text(
                    text = "Todavía no hay nada que mostrar.\nCrea metas en Objetivos o escribe tu primera nota.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                val grouped = state.days.groupBy { YearMonth.from(it.date) }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (month, days) ->
                        item(key = "month-$month") {
                            Text(
                                text = month.format(monthFormatter).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(days, key = { it.date.toEpochDay() }) { day ->
                            DayCard(day = day, onClick = { viewModel.openDay(day.date) })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = viewModel::openToday,
            icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
            text = { Text("Escribir hoy") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    selectedDate?.let { date ->
        // Si el día no tiene metas ni notas todavía, se muestra vacío
        val day = state.days.firstOrNull { it.date == date }
            ?: DayHistory(date = date, activities = emptyList(), notes = emptyList())

        DayDetailDialog(
            day = day,
            onDismiss = viewModel::closeDay,
            onAdd = { text, photos -> viewModel.addNote(date, text, photos) },
            onUpdate = viewModel::updateNote,
            onDelete = viewModel::deleteNote,
            onAddPhotos = viewModel::addPhotos,
            onDeleteImage = viewModel::deleteImage
        )
    }
}

@Composable
private fun DayCard(day: DayHistory, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = day.date.format(dayFormatter).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (day.totalCount > 0) {
                    val complete = day.doneCount == day.totalCount
                    Text(
                        text = "${day.doneCount}/${day.totalCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (complete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (day.activities.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                day.activities.forEach { ActivityRow(it) }
            }

            if (day.notes.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                day.notes.forEach { NoteRow(it) }
            }
        }
    }
}

@Composable
internal fun ActivityRow(activity: HistoryActivity) {
    val (icon, subtitle) = when (activity.status) {
        HistoryStatus.DONE -> "✅" to activity.completedAt?.toHour()
        HistoryStatus.SKIPPED -> "⏭" to "Omitida"
        HistoryStatus.MISSED -> "❌" to "No cumplida"
        HistoryStatus.PENDING -> "⏳" to "Pendiente"
    }
    val dim = activity.status == HistoryStatus.MISSED || activity.status == HistoryStatus.SKIPPED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorDot(
            color = if (activity.color != 0) Color(activity.color)
            else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = activity.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(icon)
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun NoteRow(note: JournalNote) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "📝 ${note.createdAt.toHour()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (note.text.isNotBlank()) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (note.images.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                note.images.take(4).forEach { image ->
                    LocalImage(
                        source = image.filePath,
                        targetPx = 200,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                if (note.images.size > 4) {
                    Text(
                        text = "+${note.images.size - 4}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}