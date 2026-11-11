package com.tiempo.app.domain.repository

import com.tiempo.app.domain.model.JournalNote
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    /** Todas las notas, de la más reciente a la más antigua (dentro de un día, en orden de escritura). */
    fun observeAllNotes(): Flow<List<JournalNote>>

    fun observeNotesForDay(date: LocalDate): Flow<List<JournalNote>>

    /** Devuelve el id de la nota creada. */
    suspend fun addNote(date: LocalDate, text: String): Long

    suspend fun updateNote(id: Long, text: String)

    /** Borra la nota y los archivos de sus fotos. */
    suspend fun deleteNote(id: Long)

    /** [sources] son los enlaces que entrega el selector de fotos. Las que fallen se omiten. */
    suspend fun addImages(noteId: Long, sources: List<String>)

    /** Borra la foto y su archivo. */
    suspend fun deleteImage(imageId: Long)

    /** Borra todas las notas y los archivos de sus fotos. */
    suspend fun deleteAll()
}