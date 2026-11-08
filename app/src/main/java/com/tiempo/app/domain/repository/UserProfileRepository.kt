package com.tiempo.app.domain.repository

import com.tiempo.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    suspend fun getProfile(): UserProfile?

    suspend fun saveProfile(profile: UserProfile)
}
