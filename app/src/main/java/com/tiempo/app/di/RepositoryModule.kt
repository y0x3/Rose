package com.tiempo.app.di

import com.tiempo.app.data.repository.GoalRepositoryImpl
import com.tiempo.app.data.repository.JournalRepositoryImpl
import com.tiempo.app.data.repository.UserProfileRepositoryImpl
import com.tiempo.app.domain.repository.GoalRepository
import com.tiempo.app.domain.repository.JournalRepository
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

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        implementation: GoalRepositoryImpl
    ): GoalRepository

    @Binds
    @Singleton
    abstract fun bindJournalRepository(
        implementation: JournalRepositoryImpl
    ): JournalRepository
}