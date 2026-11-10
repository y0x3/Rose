package com.tiempo.app.presentation.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempo.app.domain.model.Goal
import com.tiempo.app.domain.model.GoalOccurrence
import com.tiempo.app.domain.model.OccurrenceStatus
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    NotificationPermissionEffect()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                editorOpen = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva meta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DayHeader(
                text = state.date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                onPrevious = viewModel::previousDay,
                onNext = viewModel::nextDay,
                onToday = viewModel::today
            )

            if (!state.isLoading && state.occurrences.isEmpty()) {
                Text(
                    text = "No hay metas para este día.\nToca + para crear una.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.occurrences, key = { it.goal.id }) { occurrence ->
                        GoalCard(
                            occurrence = occurrence,
                            onToggle = { viewModel.toggle(occurrence) },
                            onClick = {
                                editing = occurrence.goal
                                editorOpen = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (editorOpen) {
        val current = editing
        GoalEditorSheet(
            goal = current,
            defaultDate = state.date,
            onDismiss = { editorOpen = false },
            onSave = { goal ->
                viewModel.save(goal)
                editorOpen = false
            },
            onDelete = if (current != null) {
                { _: Goal ->
                    viewModel.delete(current.id)
                    editorOpen = false
                }
            } else null
        )
    }
}

@Composable
private fun DayHeader(
    text: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Día anterior")
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToday)
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Día siguiente")
        }
    }
}

@Composable
private fun GoalCard(
    occurrence: GoalOccurrence,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val done = occurrence.status == OccurrenceStatus.DONE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (done) 0.6f else 1f)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                Text(
                    text = "${occurrence.goal.time.format(timeFormatter)} · ${frequencyLabel(occurrence.goal)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (done) Icons.Filled.CheckCircle
                    else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (done) "Desmarcar" else "Marcar como hecha",
                    tint = if (done) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}