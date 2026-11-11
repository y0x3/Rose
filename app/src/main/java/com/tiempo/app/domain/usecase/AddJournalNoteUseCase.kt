package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import java.time.LocalDate
import javax.inject.Inject

class AddJournalNoteUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    /** El texto puede quedar vacío: una nota puede ser solo una foto. */
    suspend operator fun invoke(date: LocalDate, text: String): Long =
        repository.addNote(date, text.trim())
}