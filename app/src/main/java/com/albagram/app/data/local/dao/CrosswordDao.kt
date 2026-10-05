package com.albagram.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.albagram.app.data.local.entity.CrosswordClueEntity
import com.albagram.app.data.local.entity.CrosswordPuzzleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CrosswordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzles(puzzles: List<CrosswordPuzzleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClues(clues: List<CrosswordClueEntity>)

    @Query("SELECT * FROM crossword_puzzles ORDER BY title ASC")
    fun observePuzzles(): Flow<List<CrosswordPuzzleEntity>>

    @Query("SELECT * FROM crossword_puzzles WHERE puzzleId = :id")
    suspend fun getPuzzle(id: String): CrosswordPuzzleEntity?

    @Query("SELECT * FROM crossword_clues WHERE puzzleId = :puzzleId ORDER BY number ASC, direction ASC")
    suspend fun getClues(puzzleId: String): List<CrosswordClueEntity>

    @Query("SELECT puzzleId FROM crossword_puzzles ORDER BY puzzleId ASC")
    suspend fun getAllPuzzleIds(): List<String>

    @Query("SELECT COUNT(*) FROM crossword_puzzles")
    suspend fun count(): Int

    @Query("DELETE FROM crossword_clues")
    suspend fun clearClues()

    @Query("DELETE FROM crossword_puzzles")
    suspend fun clearPuzzles()
}
