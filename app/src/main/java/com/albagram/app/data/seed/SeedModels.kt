package com.albagram.app.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedWord(
    val wordId: String,
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val exampleSentence: String? = null,
    val letterOfAlphabet: String,
    val rhymeKey: String,
    val inGlossary: Boolean = true,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val usageNote: String? = null
)

@Serializable
data class SeedCrosswordFile(
    val puzzles: List<SeedPuzzle>
)

@Serializable
data class SeedPuzzle(
    val puzzleId: String,
    val title: String,
    val width: Int,
    val height: Int,
    val clues: List<SeedClue>
)

@Serializable
data class SeedClue(
    val clueId: String,
    val wordId: String,
    val direction: String,
    val number: Int,
    val row: Int,
    val col: Int,
    val length: Int,
    val clueText: String,
    val answer: String
)

@Serializable
data class SeedQuizQuestion(
    val questionId: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val timeLimitSeconds: Int,
    val relatedWordId: String? = null
)

@Serializable
data class SeedEssayPrompt(
    val promptId: String,
    val title: String,
    val promptText: String,
    val suggestedWordIds: List<String>
)

@Serializable
data class GridCellSeed(
    val row: Int,
    val col: Int,
    val block: Boolean,
    val number: Int? = null,
    val letter: String? = null
)
