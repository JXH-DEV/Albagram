package com.albagram.app.domain.daily

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object DailyChallengeKeys {
    const val WORD = "word"
    const val CROSSWORD = "crossword"
    const val QUIZ = "quiz"
    const val ESE = "ese"
    val all = listOf(WORD, CROSSWORD, QUIZ, ESE)
}

data class DailyChallengeSet(
    val date: LocalDate,
    val wordId: String?,
    val puzzleId: String?,
    val quizQuestionIds: List<String>,
    val essayPromptId: String?,
    val weeklyWordIds: List<String>
)

object DailyChallengeEngine {
    private val locale = Locale("sq", "AL")

    fun pickIndex(date: LocalDate, salt: String, size: Int): Int {
        if (size <= 0) return 0
        val hash = (date.toString() + salt).hashCode().toLong() and 0xFFFFFFFFL
        return (hash % size).toInt()
    }

    fun dailyWordId(wordIds: List<String>, date: LocalDate): String? {
        if (wordIds.isEmpty()) return null
        return wordIds[pickIndex(date, "word", wordIds.size)]
    }

    fun dailyPuzzleId(puzzleIds: List<String>, date: LocalDate): String? {
        if (puzzleIds.isEmpty()) return null
        return puzzleIds[pickIndex(date, "crossword", puzzleIds.size)]
    }

    fun dailyQuizIds(questionIds: List<String>, date: LocalDate, count: Int = 5): List<String> {
        if (questionIds.isEmpty()) return emptyList()
        val start = pickIndex(date, "quiz", questionIds.size)
        return (0 until count.coerceAtMost(questionIds.size)).map { i ->
            questionIds[(start + i) % questionIds.size]
        }
    }

    fun dailyEssayId(promptIds: List<String>, date: LocalDate): String? {
        if (promptIds.isEmpty()) return null
        return promptIds[pickIndex(date, "ese", promptIds.size)]
    }

    fun weeklyWordIds(wordIds: List<String>, date: LocalDate, count: Int = 5): List<String> {
        if (wordIds.isEmpty()) return emptyList()
        val week = date.get(WeekFields.of(locale).weekOfWeekBasedYear())
        val year = date.get(WeekFields.of(locale).weekBasedYear())
        val start = pickIndex(LocalDate.of(year, 1, 1), "week-$year-$week", wordIds.size)
        return (0 until count.coerceAtMost(wordIds.size)).map { i ->
            wordIds[(start + i) % wordIds.size]
        }.distinct()
    }

    fun build(
        date: LocalDate,
        wordIds: List<String>,
        puzzleIds: List<String>,
        quizIds: List<String>,
        essayIds: List<String>
    ): DailyChallengeSet = DailyChallengeSet(
        date = date,
        wordId = dailyWordId(wordIds, date),
        puzzleId = dailyPuzzleId(puzzleIds, date),
        quizQuestionIds = dailyQuizIds(quizIds, date),
        essayPromptId = dailyEssayId(essayIds, date),
        weeklyWordIds = weeklyWordIds(wordIds, date)
    )
}
