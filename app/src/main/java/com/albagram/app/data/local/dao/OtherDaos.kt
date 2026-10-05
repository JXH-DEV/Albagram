package com.albagram.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.albagram.app.data.local.entity.CrosswordProgressEntity
import com.albagram.app.data.local.entity.EssayDraftEntity
import com.albagram.app.data.local.entity.EssayPromptEntity
import com.albagram.app.data.local.entity.QuizQuestionEntity
import com.albagram.app.data.local.entity.SavedWordEntity
import com.albagram.app.data.local.entity.SavedWordListItem
import com.albagram.app.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuizQuestionEntity>)

    @Query("SELECT * FROM quiz_questions")
    suspend fun getAll(): List<QuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM quiz_questions")
    suspend fun count(): Int

    @Query("SELECT questionId FROM quiz_questions ORDER BY questionId ASC")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM quiz_questions WHERE questionId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<QuizQuestionEntity>

    @Query("DELETE FROM quiz_questions")
    suspend fun clear()
}

@Dao
interface EssayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompts(prompts: List<EssayPromptEntity>)

    @Query("SELECT * FROM essay_prompts ORDER BY title ASC")
    fun observePrompts(): Flow<List<EssayPromptEntity>>

    @Query("SELECT * FROM essay_prompts WHERE promptId = :id")
    suspend fun getPrompt(id: String): EssayPromptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: EssayDraftEntity)

    @Query("SELECT * FROM essay_drafts WHERE promptId = :promptId")
    fun observeDraft(promptId: String): Flow<EssayDraftEntity?>

    @Query("SELECT * FROM essay_drafts WHERE promptId = :promptId")
    suspend fun getDraft(promptId: String): EssayDraftEntity?

    @Query("SELECT * FROM essay_drafts WHERE completedAt IS NOT NULL")
    suspend fun getCompletedDrafts(): List<EssayDraftEntity>

    @Query("SELECT COUNT(*) FROM essay_drafts WHERE completedAt IS NOT NULL")
    suspend fun countCompleted(): Int

    @Query("SELECT * FROM essay_drafts WHERE completedAt IS NULL AND length(body) > 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestInProgressDraft(): EssayDraftEntity?

    @Query("SELECT promptId FROM essay_prompts ORDER BY promptId ASC")
    suspend fun getAllPromptIds(): List<String>

    @Query("SELECT * FROM essay_drafts")
    fun observeAllDrafts(): Flow<List<EssayDraftEntity>>

    @Query("SELECT COUNT(*) FROM essay_prompts")
    suspend fun count(): Int

    @Query("DELETE FROM essay_prompts")
    suspend fun clearPrompts()
}

@Dao
interface SavedWordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(saved: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE wordId = :wordId")
    suspend fun delete(wordId: String)

    @Query("SELECT * FROM saved_words WHERE wordId = :wordId")
    suspend fun get(wordId: String): SavedWordEntity?

    @Query(
        """
        SELECT w.*, s.timesReviewed AS timesReviewed FROM words w
        INNER JOIN saved_words s ON w.wordId = s.wordId
        WHERE (:query = '' OR w.term LIKE '%' || :query || '%' OR w.definition LIKE '%' || :query || '%')
        ORDER BY s.dateSaved DESC
        """
    )
    fun observeSavedWords(query: String): Flow<List<SavedWordListItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE wordId = :wordId)")
    fun observeIsSaved(wordId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM saved_words")
    fun observeCount(): Flow<Int>

    @Query("UPDATE saved_words SET timesReviewed = timesReviewed + 1 WHERE wordId = :wordId")
    suspend fun incrementReviewed(wordId: String)
}

@Dao
interface ProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgressEntity)

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun observe(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun get(): UserProgressEntity?

    @Query("UPDATE user_progress SET score = score + :points WHERE id = 1")
    suspend fun addScore(points: Int): Int

    @Query("UPDATE user_progress SET streak = streak + 1 WHERE id = 1")
    suspend fun bumpStreak(): Int

    @Query("UPDATE user_progress SET streak = 0 WHERE id = 1")
    suspend fun resetStreak(): Int

    @Query("UPDATE user_progress SET badgesJson = :badgesJson WHERE id = 1")
    suspend fun updateBadges(badgesJson: String): Int

    @Query("UPDATE user_progress SET lastVisitDate = :date, dailyVisitStreak = :streak WHERE id = 1")
    suspend fun updateDailyVisit(date: String, streak: Int): Int

    @Query("UPDATE user_progress SET dailyCompletedJson = :json WHERE id = 1")
    suspend fun updateDailyCompleted(json: String): Int
}

@Dao
interface CrosswordProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: CrosswordProgressEntity)

    @Query("SELECT * FROM crossword_progress WHERE puzzleId = :puzzleId")
    suspend fun get(puzzleId: String): CrosswordProgressEntity?

    @Query("SELECT * FROM crossword_progress WHERE completed = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestInProgress(): CrosswordProgressEntity?

    @Query("SELECT COUNT(*) FROM crossword_progress WHERE completed = 1")
    suspend fun countCompleted(): Int

    @Query("SELECT * FROM crossword_progress")
    fun observeAll(): Flow<List<CrosswordProgressEntity>>
}
