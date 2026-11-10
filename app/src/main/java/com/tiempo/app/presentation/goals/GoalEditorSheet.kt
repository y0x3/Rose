package com.tiempo.app.presentation.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tiempo.app.domain.model.AlertType
import com.tiempo.app.domain.model.FrequencyType
import com.tiempo.app.domain.model.Goal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es")
private val longDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES)
private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

private val frequencyOptions = listOf(
    FrequencyType.DAILY to "Todos los días",
    FrequencyType.WEEKDAYS to "Días de la semana",
    FrequencyType.WEEKLY to "Cada semana",
    FrequencyType.MONTHLY to "Cada mes",
    FrequencyType.YEARLY to "Cada año",
    FrequencyType.EVERY_N_DAYS to "Cada N días"
)

private val dayChips = listOf(
    DayOfWeek.MONDAY to "L", DayOfWeek.TUESDAY to "M", DayOfWeek.WEDNESDAY to "X",
    DayOfWeek.THURSDAY to "J", DayOfWeek.FRIDAY to "V", DayOfWeek.SATURDAY to "S",
    DayOfWeek.SUNDAY to "D"
)

private data class AlertOption(
    val type: AlertType,
    val emoji: String,
    val title: String,
    val description: String
)

private val alertOptions = listOf(
    AlertOption(AlertType.SIMPLE, "🔔", "Alerta simple", "Solo un sonido, sin mensaje en pantalla."),
    AlertOption(AlertType.MESSAGE, "💬", "Sonido con mensaje", "Suena y muestra el texto de la meta."),
    AlertOption(AlertType.ALARM, "⏰", "Alarma", "Suena sin parar hasta que la apagues.")
)

/** Texto corto que describe cuándo se repite una meta (se usa también en las tarjetas). */
fun frequencyLabel(goal: Goal): String = when (goal.frequency) {
    FrequencyType.DAILY -> "Todos los días"
    FrequencyType.WEEKDAYS, FrequencyType.WEEKLY ->
        goal.daysOfWeek.sorted().joinToString(", ") {
            it.getDisplayName(TextStyle.SHORT, ES).replace(".", "")
        }.ifEmpty { "Sin días" }
    FrequencyType.MONTHLY -> "Día ${goal.dayOfMonth ?: goal.startDate.dayOfMonth} de cada mes"
    FrequencyType.YEARLY -> "Cada año el ${goal.startDate.dayOfMonth} de " +
            goal.startDate.month.getDisplayName(TextStyle.FULL, ES)
    FrequencyType.EVERY_N_DAYS -> "Cada ${goal.interval} días"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalEditorSheet(
    goal: Goal?,                       // null = meta nueva
    defaultDate: LocalDate,            // fecha de inicio por defecto
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit,
    onDelete: ((Goal) -> Unit)?        // null cuando es una meta nueva
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(goal?.title ?: "") }
    var note by remember { mutableStateOf(goal?.note ?: "") }
    var frequency by remember { mutableStateOf(goal?.frequency ?: FrequencyType.DAILY) }
    var days by remember { mutableStateOf(goal?.daysOfWeek ?: emptySet()) }
    var dayOfMonth by remember { mutableStateOf(goal?.dayOfMonth ?: defaultDate.dayOfMonth) }
    var interval by remember { mutableStateOf(goal?.interval?.coerceAtLeast(2) ?: 2) }
    var startDate by remember { mutableStateOf(goal?.startDate ?: defaultDate) }
    var time by remember { mutableStateOf(goal?.time ?: LocalTime.of(8, 0)) }
    var alertType by remember { mutableStateOf(goal?.alertType ?: AlertType.MESSAGE) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val needsDays = frequency == FrequencyType.WEEKDAYS || frequency == FrequencyType.WEEKLY
    val isValid = title.isNotBlank() && (!needsDays || days.isNotEmpty())

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (goal == null) "Nueva meta" else "Editar meta",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- Frecuencia ----
            SectionTitle("Se repite")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                frequencyOptions.forEach { (type, label) ->
                    FilterChip(
                        selected = frequency == type,
                        onClick = {
                            frequency = type
                            // "Cada semana" admite un solo día
                            if (type == FrequencyType.WEEKLY && days.size > 1) {
                                days = setOf(days.minOf { it })
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (needsDays) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayChips.forEach { (day, letter) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = day in days,
                            onClick = {
                                days = when {
                                    frequency == FrequencyType.WEEKLY -> setOf(day)
                                    day in days -> days - day
                                    else -> days + day
                                }
                            },
                            label = {
                                Text(
                                    text = letter,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }
            }

            if (frequency == FrequencyType.MONTHLY) {
                Stepper(
                    label = "Día del mes",
                    value = dayOfMonth,
                    onChange = { dayOfMonth = it.coerceIn(1, 31) }
                )
                if (dayOfMonth > 28) {
                    Text(
                        "En los meses más cortos se usará el último día del mes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (frequency == FrequencyType.EVERY_N_DAYS) {
                Stepper(
                    label = "Cada cuántos días",
                    value = interval,
                    onChange = { interval = it.coerceIn(2, 365) }
                )
            }

            // ---- Hora y fecha de inicio ----
            SectionTitle("Cuándo")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) { Text("Hora: ${time.format(hhmm)}") }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(2f)
                ) { Text("Desde: ${startDate.format(longDate)}") }
            }

            // ---- Tipo de aviso ----
            SectionTitle("Aviso")
            alertOptions.forEach { option ->
                AlertCard(
                    option = option,
                    selected = alertType == option.type,
                    onClick = { alertType = option.type }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Nota (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val base = goal ?: Goal(title = "", startDate = startDate, time = time)
                    onSave(
                        base.copy(
                            title = title.trim(),
                            note = note.trim().ifEmpty { null },
                            frequency = frequency,
                            daysOfWeek = if (needsDays) days else emptySet(),
                            dayOfMonth = if (frequency == FrequencyType.MONTHLY) dayOfMonth else null,
                            interval = if (frequency == FrequencyType.EVERY_N_DAYS) interval else 1,
                            startDate = startDate,
                            time = time,
                            alertType = alertType
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (goal == null) "Crear meta" else "Guardar cambios") }

            if (goal != null && onDelete != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Eliminar meta", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(time.hour, time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            }
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis =
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = state) }
    }

    if (showDeleteConfirm && goal != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar meta?") },
            text = { Text("Se borrarán también todos sus cumplimientos. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete(goal) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange(value - 1) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Menos")
        }
        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onChange(value + 1) }) {
            Icon(Icons.Filled.Add, contentDescription = "Más")
        }
    }
}

@Composable
private fun AlertCard(option: AlertOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(option.emoji, style = MaterialTheme.typography.headlineSmall)
            Column {
                Text(option.title, style = MaterialTheme.typography.titleMedium)
                Text(option.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}