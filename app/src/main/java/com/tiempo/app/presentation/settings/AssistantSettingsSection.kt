package com.tiempo.app.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiempo.app.service.assistant.AssistantMode
import com.tiempo.app.service.assistant.DeviceCapabilities
import com.tiempo.app.service.assistant.LocalTier

private data class ModeOption(
    val mode: AssistantMode,
    val title: String,
    val description: String,
    val available: Boolean
)

private val modeOptions = listOf(
    ModeOption(
        AssistantMode.KEYWORDS,
        "Palabras clave",
        "Gratis, sin internet y privado. Entiende frases como “anota que…” o “ya hice…”.",
        true
    ),
    ModeOption(
        AssistantMode.LOCAL,
        "Modelo en el teléfono",
        "Lenguaje libre sin internet. Depende de la memoria del teléfono.",
        false
    ),
    ModeOption(
        AssistantMode.CLOUD,
        "Nube con tu propia clave",
        "Lenguaje libre usando tu cuenta de un proveedor de IA.",
        false
    )
)

@Composable
fun AssistantSettingsSection(
    mode: AssistantMode,
    device: DeviceCapabilities?,
    onModeSelected: (AssistantMode) -> Unit,
    onCheckDevice: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Asistente de voz",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        modeOptions.forEach { option ->
            ModeCard(
                option = option,
                selected = mode == option.mode,
                onClick = { if (option.available) onModeSelected(option.mode) }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tu teléfono", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                if (device == null) {
                    Text(
                        "Mide la memoria disponible para saber qué modelos podrá usar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DeviceRow("RAM total", "${device.totalRamMb} MB")
                    DeviceRow("RAM libre ahora", "${device.availableRamMb} MB")
                    DeviceRow("Espacio libre", "${device.freeStorageMb} MB")
                    DeviceRow("Android", "API ${device.androidSdk}")
                    DeviceRow("Procesador 64 bits", if (device.is64Bit) "Sí" else "No")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (device.tier) {
                            LocalTier.NONE -> "Nivel: ninguno. Se usará el modo de palabras clave."
                            LocalTier.SMALL -> "Nivel: pequeño. Podría correr un modelo muy ligero, lo comprobaremos con una prueba de velocidad."
                            LocalTier.MEDIUM -> "Nivel: mediano. Podría correr un modelo de tamaño medio, lo comprobaremos con una prueba de velocidad."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Es una estimación. Estos umbrales se ajustarán con mediciones reales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCheckDevice) {
                    Text(if (device == null) "Medir mi teléfono" else "Medir de nuevo")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Qué puedes decir", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "📝 “Anota que hoy corrí cinco kilómetros”",
                    "🎯 “Nueva meta leer todos los días a las 9 de la noche”",
                    "🎯 “Meta de gimnasio lunes, miércoles y viernes a las 7 de la mañana”",
                    "🎯 “Crea una meta de pagar la renta el 1 de cada mes a las 10”",
                    "✅ “Ya hice el gimnasio”",
                    "✅ “Marca meditar como hecha”"
                ).forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Text(
                    text = "Si falta un dato, el asistente te lo pregunta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeCard(option: ModeOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = option.available, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null, enabled = option.available)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = if (option.available) option.title else "${option.title} (próximamente)",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (option.available) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}