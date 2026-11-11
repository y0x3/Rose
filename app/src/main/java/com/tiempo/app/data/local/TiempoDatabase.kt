package com.tiempo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tiempo.app.data.local.dao.GoalDao
import com.tiempo.app.data.local.dao.JournalDao
import com.tiempo.app.data.local.dao.UserProfileDao
import com.tiempo.app.data.local.entity.GoalCompletionEntity
import com.tiempo.app.data.local.entity.GoalEntity
import com.tiempo.app.data.local.entity.JournalImageEntity
import com.tiempo.app.data.local.entity.JournalNoteEntity
import com.tiempo.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        GoalEntity::class,
        GoalCompletionEntity::class,
        JournalNoteEntity::class,
        JournalImageEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class TiempoDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao
    abstract fun journalDao(): JournalDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) Copia del título y color en cada cumplimiento
                db.execSQL(
                    "ALTER TABLE `goal_completions` ADD COLUMN `goalTitle` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE `goal_completions` ADD COLUMN `goalColor` INTEGER NOT NULL DEFAULT 0"
                )
                // Rellena los cumplimientos que ya existen con los datos actuales de su meta
                db.execSQL(
                    """
                    UPDATE `goal_completions` SET
                        `goalTitle` = COALESCE(
                            (SELECT `title` FROM `goals` WHERE `goals`.`id` = `goal_completions`.`goalId`), ''),
                        `goalColor` = COALESCE(
                            (SELECT `color` FROM `goals` WHERE `goals`.`id` = `goal_completions`.`goalId`), 0)
                    """.trimIndent()
                )

                // 2) Notas del diario
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_notes_dateEpochDay` " +
                            "ON `journal_notes` (`dateEpochDay`)"
                )

                // 3) Imágenes de cada nota
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `journal_images` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `journal_notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_journal_images_noteId` " +
                            "ON `journal_images` (`noteId`)"
                )
            }
        }
    }
}