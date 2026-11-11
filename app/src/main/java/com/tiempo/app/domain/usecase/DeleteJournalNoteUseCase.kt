package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import javax.inject.Inject

class DeleteJournalNoteUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteNote(id)
}