package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import javax.inject.Inject

class UpdateJournalNoteUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long, text: String) =
        repository.updateNote(id, text.trim())
}