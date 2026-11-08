package com.tiempo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tiempo.app.data.local.dao.UserProfileDao
import com.tiempo.app.data.local.entity.UserProfileEntity

@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TiempoDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

    companion object {
        const val NAME = "tiempo.db"
    }
}
