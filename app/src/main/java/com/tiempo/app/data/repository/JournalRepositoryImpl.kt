package com.tiempo.app.data.repository

import android.net.Uri
import com.tiempo.app.data.local.JournalImageStorage
import com.tiempo.app.data.local.dao.JournalDao
import com.tiempo.app.data.local.entity.JournalImageEntity
import com.tiempo.app.data.local.entity.JournalNoteEntity
import com.tiempo.app.data.mapper.toDomain
import com.tiempo.app.domain.model.JournalNote
import com.tiempo.app.domain.repository.JournalRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalDao,
    private val storage: JournalImageStorage,
    private val clock: Clock
) : JournalRepository {

    override fun observeAllNotes(): Flow<List<JournalNote>> =
        combine(dao.observeAllNotes(), dao.observeAllImages()) { notes, images ->
            assemble(notes, images)
        }

    override fun observeNotesForDay(date: LocalDate): Flow<List<JournalNote>> {
        val day = date.toEpochDay()
        return combine(dao.observeNotesForDay(day), dao.observeImagesForDay(day)) { notes, images ->
            assemble(notes, images)
        }
    }

    override suspend fun addNote(date: LocalDate, text: String): Long {
        val now = clock.millis()
        return dao.insertNote(
            JournalNoteEntity(
                dateEpochDay = date.toEpochDay(),
                text = text,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    override suspend fun updateNote(id: Long, text: String) {
        val note = dao.getNote(id) ?: return
        dao.updateNote(note.copy(text = text, updatedAtMillis = clock.millis()))
    }

    override suspend fun deleteNote(id: Long) {
        val paths = dao.getImagePaths(id)
        dao.deleteNote(id)               // las filas de fotos se borran en cascada
        paths.forEach { storage.delete(it) }
    }

    override suspend fun addImages(noteId: Long, sources: List<String>) {
        var position = dao.getMaxImagePosition(noteId) + 1
        sources.forEach { source ->
            val path = storage.import(Uri.parse(source)) ?: return@forEach
            dao.insertImage(
                JournalImageEntity(
                    noteId = noteId,
                    filePath = path,
                    position = position++,
                    createdAtMillis = clock.millis()
                )
            )
        }
    }

    override suspend fun deleteImage(imageId: Long) {
        val path = dao.getImagePath(imageId)
        dao.deleteImage(imageId)
        path?.let { storage.delete(it) }
    }

    override suspend fun deleteAll() {
        val paths = dao.getAllImagePaths()
        dao.deleteAllNotes()             // las filas de fotos se borran en cascada
        paths.forEach { storage.delete(it) }
    }
    private fun assemble(
        notes: List<JournalNoteEntity>,
        images: List<JournalImageEntity>
    ): List<JournalNote> {
        val byNote = images.groupBy { it.noteId }
        return notes.map { note ->
            note.toDomain(byNote[note.id].orEmpty().map { it.toDomain() })
        }
    }
}