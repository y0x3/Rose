package com.tiempo.app.data.mapper

import com.tiempo.app.data.local.entity.JournalImageEntity
import com.tiempo.app.data.local.entity.JournalNoteEntity
import com.tiempo.app.domain.model.JournalImage
import com.tiempo.app.domain.model.JournalNote
import java.time.Instant
import java.time.LocalDate

fun JournalImageEntity.toDomain() = JournalImage(
    id = id, noteId = noteId, filePath = filePath, position = position
)

fun JournalNoteEntity.toDomain(images: List<JournalImage> = emptyList()) = JournalNote(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    text = text,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    images = images
)