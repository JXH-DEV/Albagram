package com.albagram.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey val questionId: String,
    val prompt: String,
    val optionsJson: String,
    val correctIndex: Int,
    val timeLimitSeconds: Int,
    val relatedWordId: String? = null
)

@Entity(tableName = "essay_prompts")
data class EssayPromptEntity(
    @PrimaryKey val promptId: String,
    val title: String,
    val promptText: String,
    val suggestedWordIdsJson: String
)

@Entity(tableName = "essay_drafts")
data class EssayDraftEntity(
    @PrimaryKey val promptId: String,
    val body: String,
    val updatedAt: Long,
    val completedAt: Long? = null
)

@Entity(tableName = "saved_words")
data class SavedWordEntity(
    @PrimaryKey val wordId: String,
    val dateSaved: Long,
    val timesReviewed: Int = 0,
    val sourceModule: String? = null
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val score: Int = 0,
    val streak: Int = 0,
    val badgesJson: String = "[]",
    val lastVisitDate: String = "",
    val dailyVisitStreak: Int = 0,
    val dailyCompletedJson: String = "{}"
)

data class SavedWordListItem(
    @androidx.room.Embedded val word: WordEntity,
    val timesReviewed: Int
)
