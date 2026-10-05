package com.albagram.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.albagram.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words WHERE wordId = :id")
    suspend fun getById(id: String): WordEntity?

    @Query("SELECT * FROM words WHERE wordId = :id")
    fun observeById(id: String): Flow<WordEntity?>

    @Query("SELECT * FROM words WHERE inGlossary = 1 ORDER BY term COLLATE NOCASE ASC")
    fun observeGlossary(): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM words
        WHERE inGlossary = 1 AND letterOfAlphabet = :letter
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun observeByLetter(letter: String): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM words
        WHERE term LIKE '%' || :query || '%'
           OR definition LIKE '%' || :query || '%'
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM words
        WHERE rhymeKey = :rhymeKey AND LOWER(term) != LOWER(:excludeTerm)
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    suspend fun findByRhymeKey(rhymeKey: String, excludeTerm: String): List<WordEntity>

    @Query(
        """
        SELECT * FROM words
        WHERE term LIKE '%' || :suffix AND LOWER(term) != LOWER(:excludeTerm)
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    suspend fun findBySuffix(suffix: String, excludeTerm: String): List<WordEntity>

    @Query("SELECT COUNT(*) FROM words")
    suspend fun count(): Int

    @Query("SELECT * FROM words WHERE wordId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<WordEntity>

    @Query("SELECT wordId FROM words WHERE inGlossary = 1 ORDER BY wordId ASC")
    suspend fun getGlossaryWordIds(): List<String>

    @Query("SELECT * FROM words WHERE inGlossary = 1")
    suspend fun getAllGlossary(): List<WordEntity>

    @Query("DELETE FROM words")
    suspend fun clear()
}
