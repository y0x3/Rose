package com.tiempo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tiempo.app.data.local.entity.JournalImageEntity
import com.tiempo.app.data.local.entity.JournalNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_notes ORDER BY dateEpochDay DESC, createdAtMillis ASC")
    fun observeAllNotes(): Flow<List<JournalNoteEntity>>

    @Query("SELECT * FROM journal_notes WHERE dateEpochDay = :day ORDER BY createdAtMillis ASC")
    fun observeNotesForDay(day: Long): Flow<List<JournalNoteEntity>>

    @Query("SELECT * FROM journal_images ORDER BY noteId, position")
    fun observeAllImages(): Flow<List<JournalImageEntity>>

    @Query(
        "SELECT * FROM journal_images WHERE noteId IN " +
                "(SELECT id FROM journal_notes WHERE dateEpochDay = :day) ORDER BY noteId, position"
    )
    fun observeImagesForDay(day: Long): Flow<List<JournalImageEntity>>

    @Query("SELECT * FROM journal_notes WHERE id = :id")
    suspend fun getNote(id: Long): JournalNoteEntity?

    @Insert
    suspend fun insertNote(note: JournalNoteEntity): Long

    @Update
    suspend fun updateNote(note: JournalNoteEntity)

    @Query("DELETE FROM journal_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Insert
    suspend fun insertImage(image: JournalImageEntity): Long

    @Query("SELECT filePath FROM journal_images WHERE noteId = :noteId")
    suspend fun getImagePaths(noteId: Long): List<String>

    @Query("SELECT filePath FROM journal_images WHERE id = :id")
    suspend fun getImagePath(id: Long): String?

    @Query("SELECT COALESCE(MAX(position), -1) FROM journal_images WHERE noteId = :noteId")
    suspend fun getMaxImagePosition(noteId: Long): Int

    @Query("DELETE FROM journal_images WHERE id = :id")
    suspend fun deleteImage(id: Long)

    @Query("SELECT filePath FROM journal_images")
    suspend fun getAllImagePaths(): List<String>

    @Query("DELETE FROM journal_notes")
    suspend fun deleteAllNotes()
}