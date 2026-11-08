package com.tiempo.app.data.repository

import com.tiempo.app.data.local.dao.UserProfileDao
import com.tiempo.app.data.mapper.toDomain
import com.tiempo.app.data.mapper.toEntity
import com.tiempo.app.domain.model.UserProfile
import com.tiempo.app.domain.repository.UserProfileRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao,
    private val clock: Clock
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        dao.observeProfile().map { entity -> entity?.toDomain() }

    override suspend fun getProfile(): UserProfile? = dao.getProfile()?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) {
        dao.upsert(profile.toEntity(updatedAtEpochMillis = clock.millis()))
    }
}
