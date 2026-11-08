package com.leissa.slimtrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Food::class,
        DiaryEntry::class,
        Goals::class,
        Profile::class,
        WeightEntry::class,
        ExerciseEntry::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SlimTrackDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun diaryDao(): DiaryDao
    abstract fun goalsDao(): GoalsDao
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER NOT NULL, " +
                        "`unitSystem` TEXT NOT NULL, `sex` TEXT NOT NULL, `ageYears` INTEGER NOT NULL, " +
                        "`heightCm` REAL NOT NULL, `activityLevel` TEXT NOT NULL, `dietTier` TEXT NOT NULL, " +
                        "`targetWeightKg` REAL NOT NULL, `isConfigured` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `weightKg` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_weight_entries_epochDay` " +
                        "ON `weight_entries` (`epochDay`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `exercise_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`epochDay` INTEGER NOT NULL, `activity` TEXT NOT NULL, `minutes` INTEGER NOT NULL, " +
                        "`caloriesBurned` REAL NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_entries_epochDay` " +
                        "ON `exercise_entries` (`epochDay`)"
                )
            }
        }

        /**
         * Drops the unused `isConfigured` column. SQLite cannot drop a column before
         * API 34, so the table is rebuilt and existing profile data is copied over.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profile_new` (`id` INTEGER NOT NULL, " +
                        "`unitSystem` TEXT NOT NULL, `sex` TEXT NOT NULL, `ageYears` INTEGER NOT NULL, " +
                        "`heightCm` REAL NOT NULL, `activityLevel` TEXT NOT NULL, `dietTier` TEXT NOT NULL, " +
                        "`targetWeightKg` REAL NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT INTO `profile_new` (`id`, `unitSystem`, `sex`, `ageYears`, `heightCm`, " +
                        "`activityLevel`, `dietTier`, `targetWeightKg`) " +
                        "SELECT `id`, `unitSystem`, `sex`, `ageYears`, `heightCm`, " +
                        "`activityLevel`, `dietTier`, `targetWeightKg` FROM `profile`"
                )
                db.execSQL("DROP TABLE `profile`")
                db.execSQL("ALTER TABLE `profile_new` RENAME TO `profile`")
            }
        }

        fun build(context: Context): SlimTrackDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SlimTrackDatabase::class.java,
                "slimtrack.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
