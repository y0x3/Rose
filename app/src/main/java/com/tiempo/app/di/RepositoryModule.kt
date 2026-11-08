package com.tiempo.app.di

import com.tiempo.app.data.repository.UserProfileRepositoryImpl
import com.tiempo.app.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        implementation: UserProfileRepositoryImpl
    ): UserProfileRepository
}
