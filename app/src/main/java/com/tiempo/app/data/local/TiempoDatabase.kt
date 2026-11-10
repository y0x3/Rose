package com.tiempo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tiempo.app.data.local.dao.GoalDao
import com.tiempo.app.data.local.dao.UserProfileDao
import com.tiempo.app.data.local.entity.GoalCompletionEntity
import com.tiempo.app.data.local.entity.GoalEntity
import com.tiempo.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        GoalEntity::class,
        GoalCompletionEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class TiempoDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val NAME = "tiempo.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT,
                        `color` INTEGER NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `daysOfWeekMask` INTEGER NOT NULL,
                        `dayOfMonth` INTEGER,
                        `interval` INTEGER NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `endEpochDay` INTEGER,
                        `timeMinutes` INTEGER NOT NULL,
                        `alertType` TEXT NOT NULL,
                        `soundUri` TEXT,
                        `archived` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `completedAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_goal_completions_goalId_dateEpochDay` " +
                            "ON `goal_completions` (`goalId`, `dateEpochDay`)"
                )
            }
        }
    }
}