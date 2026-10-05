package com.albagram.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.albagram.app.data.local.dao.CrosswordDao
import com.albagram.app.data.local.dao.CrosswordProgressDao
import com.albagram.app.data.local.dao.EssayDao
import com.albagram.app.data.local.dao.ProgressDao
import com.albagram.app.data.local.dao.QuizDao
import com.albagram.app.data.local.dao.SavedWordDao
import com.albagram.app.data.local.dao.WordDao
import com.albagram.app.data.local.entity.CrosswordClueEntity
import com.albagram.app.data.local.entity.CrosswordProgressEntity
import com.albagram.app.data.local.entity.CrosswordPuzzleEntity
import com.albagram.app.data.local.entity.EssayDraftEntity
import com.albagram.app.data.local.entity.EssayPromptEntity
import com.albagram.app.data.local.entity.QuizQuestionEntity
import com.albagram.app.data.local.entity.SavedWordEntity
import com.albagram.app.data.local.entity.UserProgressEntity
import com.albagram.app.data.local.entity.WordEntity

@Database(
    entities = [
        WordEntity::class,
        CrosswordPuzzleEntity::class,
        CrosswordClueEntity::class,
        CrosswordProgressEntity::class,
        QuizQuestionEntity::class,
        EssayPromptEntity::class,
        EssayDraftEntity::class,
        SavedWordEntity::class,
        UserProgressEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun crosswordDao(): CrosswordDao
    abstract fun crosswordProgressDao(): CrosswordProgressDao
    abstract fun quizDao(): QuizDao
    abstract fun essayDao(): EssayDao
    abstract fun savedWordDao(): SavedWordDao
    abstract fun progressDao(): ProgressDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN synonymsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE words ADD COLUMN antonymsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE words ADD COLUMN usageNote TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS crossword_progress (
                        puzzleId TEXT NOT NULL PRIMARY KEY,
                        entriesJson TEXT NOT NULL DEFAULT '{}',
                        solvedClueIdsJson TEXT NOT NULL DEFAULT '[]',
                        hintedClueIdsJson TEXT NOT NULL DEFAULT '[]',
                        completed INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_progress ADD COLUMN lastVisitDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_progress ADD COLUMN dailyVisitStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_progress ADD COLUMN dailyCompletedJson TEXT NOT NULL DEFAULT '{}'")
                db.execSQL("ALTER TABLE essay_drafts ADD COLUMN completedAt INTEGER")
            }
        }
    }
}
