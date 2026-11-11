package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import javax.inject.Inject

class DeleteJournalImageUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(imageId: Long) = repository.deleteImage(imageId)
}