package com.albagram.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "crossword_puzzles")
data class CrosswordPuzzleEntity(
    @PrimaryKey val puzzleId: String,
    val title: String,
    val width: Int,
    val height: Int,
    val gridJson: String
)

@Entity(
    tableName = "crossword_clues",
    foreignKeys = [
        ForeignKey(
            entity = CrosswordPuzzleEntity::class,
            parentColumns = ["puzzleId"],
            childColumns = ["puzzleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["wordId"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("puzzleId"), Index("wordId")]
)
data class CrosswordClueEntity(
    @PrimaryKey val clueId: String,
    val puzzleId: String,
    val wordId: String,
    val direction: String,
    val number: Int,
    val row: Int,
    val col: Int,
    val length: Int,
    val clueText: String,
    val answer: String
)
