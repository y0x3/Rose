package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import javax.inject.Inject

class AddJournalImagesUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(noteId: Long, sources: List<String>) =
        repository.addImages(noteId, sources)
}