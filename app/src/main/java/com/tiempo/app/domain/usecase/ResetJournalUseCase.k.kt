package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.repository.JournalRepository
import javax.inject.Inject

class ResetJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke() = repository.deleteAll()
}