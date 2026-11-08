package com.tiempo.app.di

import android.content.Context
import androidx.room.Room
import com.tiempo.app.data.local.TiempoDatabase
import com.tiempo.app.data.local.dao.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TiempoDatabase =
        Room.databaseBuilder(context, TiempoDatabase::class.java, TiempoDatabase.NAME).build()

    @Provides
    fun provideUserProfileDao(database: TiempoDatabase): UserProfileDao = database.userProfileDao()
}
