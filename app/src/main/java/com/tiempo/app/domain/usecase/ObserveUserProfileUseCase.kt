package com.tiempo.app.domain.usecase

import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.repository.UserProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}
