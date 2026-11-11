package com.tiempo.app.presentation.history

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tiempo.app.domain.model.DayHistory
import com.tiempo.app.domain.model.JournalImage
import com.tiempo.app.domain.model.JournalNote
import java.time.format.DateTimeFormatter
import java.util.Locale

private val titleFormatter =
    DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailDialog(
    day: DayHistory,
    onDismiss: () -> Unit,
    onAdd: (String, List<String>) -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onAddPhotos: (Long, List<String>) -> Unit,
    onDeleteImage: (Long) -> Unit
) {
    var draft by remember(day.date) { mutableStateOf("") }
    var staged by remember(day.date) { mutableStateOf(emptyList<String>()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<JournalNote?>(null) }
    var viewing by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> staged = staged + uris.map { it.toString() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                day.date.format(titleFormatter)
                                    .replaceFirstChar { it.uppercase() }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (staged.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                staged.forEach { source ->
                                    PhotoThumb(
                                        source = source,
                                        size = 64.dp,
                                        onRemove = { staged = staged - source }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                picker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Añadir foto")
                            }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                placeholder = { Text("Escribe una nota…") },
                                modifier = Modifier.weight(1f),
                                maxLines = 4
                            )
                            IconButton(
                                onClick = {
                                    onAdd(draft, staged)
                                    draft = ""
                                    staged = emptyList()
                                },
                                enabled = draft.isNotBlank() || staged.isNotEmpty()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Guardar nota")
                            }
                        }
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        if (day.totalCount > 0) {
                            Text(
                                text = "${day.doneCount} de ${day.totalCount} metas cumplidas",
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                text = "Sin metas este día",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (day.activities.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    day.activities.forEach { ActivityRow(it) }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Notas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (day.notes.isEmpty()) {
                        item {
                            Text(
                                text = "Aún no escribiste nada este día.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(day.notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { editingId = note.id },
                                onImageClick = { viewing = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Se busca la nota en cada recomposición, así las fotos nuevas aparecen al instante
    val editingNote = day.notes.firstOrNull { it.id == editingId }
    if (editingNote != null) {
        EditNoteDialog(
            note = editingNote,
            onDismiss = { editingId = null },
            onSave = { text ->
                onUpdate(editingNote.id, text)
                editingId = null
            },
            onDeleteRequest = {
                editingId = null
                deleting = editingNote
            },
            onAddPhotos = { onAddPhotos(editingNote.id, it) },
            onRemoveImage = onDeleteImage,
            onOpenImage = { viewing = it }
        )
    }

    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar nota?") },
            text = { Text("Se borrarán también sus fotos. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(note.id)
                    deleting = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancelar") }
            }
        )
    }

    viewing?.let { path ->
        Dialog(
            onDismissRequest = { viewing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewing = null }
            ) {
                LocalImage(
                    source = path,
                    targetPx = 2000,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    contentDescription = "Foto"
                )
            }
        }
    }
}

@Composable
private fun PhotoThumb(
    source: String,
    size: Dp,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Box(modifier = Modifier.size(size)) {
        LocalImage(
            source = source,
            targetPx = 300,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA000000))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Quitar foto",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: JournalNote,
    onClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 ${note.createdAt.toHour()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (note.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = note.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (note.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    note.images.forEach { image ->
                        PhotoThumb(
                            source = image.filePath,
                            size = 96.dp,
                            onClick = { onImageClick(image.filePath) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditNoteDialog(
    note: JournalNote,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDeleteRequest: () -> Unit,
    onAddPhotos: (List<String>) -> Unit,
    onRemoveImage: (Long) -> Unit,
    onOpenImage: (String) -> Unit
) {
    var text by remember(note.id) { mutableStateOf(note.text) }
    var removing by remember { mutableStateOf<JournalImage?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> onAddPhotos(uris.map { it.toString() }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar nota") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(12.dp))
                if (note.images.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        note.images.forEach { image ->
                            PhotoThumb(
                                source = image.filePath,
                                size = 80.dp,
                                onClick = { onOpenImage(image.filePath) },
                                onRemove = { removing = image }
                            )
                        }
                    }
                }
                TextButton(onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir fotos")
                }
                TextButton(onClick = onDeleteRequest) {
                    Text("Eliminar nota", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank() || note.images.isNotEmpty()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    removing?.let { image ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("¿Quitar foto?") },
            text = { Text("Se borrará de esta nota. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveImage(image.id)
                    removing = null
                }) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancelar") }
            }
        )
    }
}