package com.tiempo.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class JournalImage(
    val id: Long = 0,
    val noteId: Long,
    val filePath: String,
    val position: Int
)

data class JournalNote(
    val id: Long = 0,
    val date: LocalDate,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val images: List<JournalImage> = emptyList()
)