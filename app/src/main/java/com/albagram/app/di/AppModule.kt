package com.albagram.app.di

import android.content.Context
import androidx.room.Room
import com.albagram.app.data.local.AppDatabase
import com.albagram.app.data.local.dao.CrosswordDao
import com.albagram.app.data.local.dao.CrosswordProgressDao
import com.albagram.app.data.local.dao.EssayDao
import com.albagram.app.data.local.dao.ProgressDao
import com.albagram.app.data.local.dao.QuizDao
import com.albagram.app.data.local.dao.SavedWordDao
import com.albagram.app.data.local.dao.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "albagram.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides fun provideWordDao(db: AppDatabase): WordDao = db.wordDao()
    @Provides fun provideCrosswordDao(db: AppDatabase): CrosswordDao = db.crosswordDao()
    @Provides fun provideCrosswordProgressDao(db: AppDatabase): CrosswordProgressDao = db.crosswordProgressDao()
    @Provides fun provideQuizDao(db: AppDatabase): QuizDao = db.quizDao()
    @Provides fun provideEssayDao(db: AppDatabase): EssayDao = db.essayDao()
    @Provides fun provideSavedWordDao(db: AppDatabase): SavedWordDao = db.savedWordDao()
    @Provides fun provideProgressDao(db: AppDatabase): ProgressDao = db.progressDao()
}
