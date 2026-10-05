package com.albagram.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val wordId: String,
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val exampleSentence: String? = null,
    val letterOfAlphabet: String,
    val rhymeKey: String,
    val inGlossary: Boolean = true,
    val synonymsJson: String = "[]",
    val antonymsJson: String = "[]",
    val usageNote: String? = null
)

@Entity(tableName = "crossword_progress")
data class CrosswordProgressEntity(
    @PrimaryKey val puzzleId: String,
    val entriesJson: String = "{}",
    val solvedClueIdsJson: String = "[]",
    val hintedClueIdsJson: String = "[]",
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
